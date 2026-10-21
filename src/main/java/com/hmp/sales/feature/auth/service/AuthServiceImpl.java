package com.hmp.sales.feature.auth.service;

import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.exception.UnauthorizedOperationException;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.auth.dto.AuthResponse;
import com.hmp.sales.feature.auth.service.GoogleAuthService.GoogleUserPayload;
import com.hmp.sales.feature.auth.service.RefreshTokenService.RefreshTokenPair;
import com.hmp.sales.feature.auth.util.CookieUtils;
import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.repository.RoleRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Triển khai dịch vụ xác thực người dùng.
 * <p>
 * Điều phối toàn bộ luồng đăng nhập Google, làm mới token và đăng xuất.
 * Tài khoản CUSTOMER được tạo tự động nếu chưa tồn tại (idempotent).
 * MANAGER và ADMIN phải được Admin provision trước — không tự đăng ký.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final GoogleAuthService googleAuthService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AppProperties appProperties;

    /**
     * Khởi tạo service với các phụ thuộc cần thiết.
     *
     * @param googleAuthService   dịch vụ xác minh Google ID Token
     * @param jwtTokenService     dịch vụ cấp phát JWT Access Token
     * @param refreshTokenService dịch vụ quản lý Refresh Token
     * @param userRepository      repository người dùng
     * @param roleRepository      repository vai trò (để lấy Role CUSTOMER)
     * @param appProperties       cấu hình ứng dụng (refreshExpirationDays)
     */
    public AuthServiceImpl(
            GoogleAuthService googleAuthService,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            AppProperties appProperties) {
        this.googleAuthService = googleAuthService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.appProperties = appProperties;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng:
     * <ol>
     *   <li>Xác minh Google ID Token.</li>
     *   <li>Tìm User theo (GOOGLE, sub) → tạo mới nếu chưa tồn tại (CUSTOMER).</li>
     *   <li>Kiểm tra tài khoản không bị khóa.</li>
     *   <li>Cấp Access Token + Refresh Token.</li>
     *   <li>Gắn Refresh Token vào cookie HttpOnly.</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String credential, HttpServletResponse httpResponse) {
        GoogleUserPayload payload = googleAuthService.verifyGoogleToken(credential);

        User user = userRepository
                .findByAuthProviderAndProviderSubjectAndIsDeletedFalse(AuthProvider.GOOGLE, payload.sub())
                .orElseGet(() -> createNewCustomer(payload));

        if (UserStatus.LOCKED.equals(user.getStatus())) {
            throw new UnauthorizedOperationException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        return issueTokensAndSetCookie(user, httpResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AuthResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String rawRefreshToken = CookieUtils.getRefreshTokenFromCookie(httpRequest)
                .orElseThrow(() -> new UnauthorizedOperationException("Refresh token không tìm thấy trong cookie"));

        UUID newAccessJti = jwtTokenService.generateJti();

        // Trong cùng transaction: revoke token cũ, tạo token mới
        RefreshTokenPair pair = refreshTokenService.rotateRefreshToken(rawRefreshToken, newAccessJti);

        // Lấy user từ entity đã lưu
        User user = pair.savedToken().getUser();

        // Cấp Access Token mới (sẽ tạo jti riêng bên trong generateAccessToken)
        String accessToken = jwtTokenService.generateAccessToken(user);

        int maxAge = (int) (appProperties.jwt().refreshExpirationDays() * 86400L);
        CookieUtils.setRefreshTokenCookie(httpResponse, pair.rawToken(), maxAge);

        return AuthResponse.bearer(accessToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void logout(String authorizationHeader, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // Thu hồi Access Token trên Redis
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String rawJwt = authorizationHeader.substring(7);
            try {
                UUID jti = jwtTokenService.extractJti(rawJwt);
                jwtTokenService.revokeAccessToken(jti);
            } catch (Exception e) {
                log.warn("Không thể thu hồi access token khi logout: {}", e.getMessage());
            }
        }

        // Thu hồi Refresh Token trong PostgreSQL
        CookieUtils.getRefreshTokenFromCookie(httpRequest).ifPresent(rawToken -> {
            try {
                refreshTokenService.revokeRefreshToken(rawToken);
            } catch (Exception e) {
                log.warn("Không thể thu hồi refresh token khi logout: {}", e.getMessage());
            }
        });

        // Xóa cookie
        CookieUtils.clearRefreshTokenCookie(httpResponse);
    }

    /**
     * Tạo tài khoản CUSTOMER mới từ thông tin Google OAuth payload.
     * Chỉ được gọi khi không tìm thấy tài khoản tồn tại với cùng (GOOGLE, sub).
     *
     * @param payload thông tin người dùng từ Google
     * @return {@link User} mới đã được lưu vào PostgreSQL
     */
    private User createNewCustomer(GoogleUserPayload payload) {
        Role customerRole = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò CUSTOMER trong hệ thống"));

        User newUser = User.builder()
                .fullName(payload.name())
                .email(payload.email())
                .authProvider(AuthProvider.GOOGLE)
                .providerSubject(payload.sub())
                .role(customerRole)
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .build();

        User saved = userRepository.save(newUser);
        log.info("Tạo tài khoản CUSTOMER mới cho email={}", payload.email());
        return saved;
    }

    /**
     * Cấp phát Access Token + Refresh Token và gắn cookie cho người dùng đã xác thực.
     *
     * @param user         người dùng cần cấp token
     * @param httpResponse servlet response để gắn cookie
     * @return {@link AuthResponse} chứa Access Token
     */
    private AuthResponse issueTokensAndSetCookie(User user, HttpServletResponse httpResponse) {
        String accessToken = jwtTokenService.generateAccessToken(user);
        UUID accessJti = jwtTokenService.extractJti(accessToken);

        RefreshTokenPair pair = refreshTokenService.createRefreshToken(user, accessJti);

        int maxAge = (int) (appProperties.jwt().refreshExpirationDays() * 86400L);
        CookieUtils.setRefreshTokenCookie(httpResponse, pair.rawToken(), maxAge);

        return AuthResponse.bearer(accessToken);
    }
}
