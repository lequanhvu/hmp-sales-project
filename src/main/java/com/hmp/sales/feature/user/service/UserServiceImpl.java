package com.hmp.sales.feature.user.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.user.dto.CreateManagerRequest;
import com.hmp.sales.feature.user.dto.UserSearchRequest;
import com.hmp.sales.feature.user.dto.UserStatusResponse;
import com.hmp.sales.feature.user.dto.UserSummaryResponse;
import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.repository.RoleRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.feature.systemlog.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation của {@link UserService}.
 * <p>
 * Xử lý các thao tác quản trị người dùng: tìm kiếm, khóa/mở khóa,
 * xóa mềm và tạo Manager. Mọi thao tác nhạy cảm được ghi audit
 * qua {@link SystemLogService}.
 * <p>
 * Token revocation khi xóa user: access token bị xóa khỏi Redis,
 * refresh token bị mark revoked trong PostgreSQL (Phase 2 sẽ bổ sung
 * RefreshTokenRepository khi entity được tạo).
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final String ENTITY_TYPE_USER = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SystemLogService systemLogService;
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            SystemLogService systemLogService,
            StringRedisTemplate stringRedisTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.systemLogService = systemLogService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // -------------------------------------------------------------------------
    // Tìm kiếm người dùng
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * Dùng JPA Specification để build câu lệnh WHERE động theo các tham số lọc.
     * Sắp xếp theo {@code createdAt} giảm dần mặc định.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> searchUsers(UserSearchRequest request) {
        PageRequest pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = buildUserSearchSpec(request);
        return userRepository.findAll(spec, pageable)
                .map(UserSummaryResponse::from);
    }

    /**
     * Tạo {@link Specification} tìm kiếm người dùng theo tiêu chí lọc.
     *
     * @param request tiêu chí tìm kiếm
     * @return Specification dùng cho JPA query
     */
    private Specification<User> buildUserSearchSpec(UserSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn lọc user chưa bị xóa mềm
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (request.name() != null && !request.name().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("fullName")),
                        "%" + request.name().toLowerCase() + "%"));
            }
            if (request.phone() != null && !request.phone().isBlank()) {
                predicates.add(cb.like(root.get("phone"), "%" + request.phone() + "%"));
            }
            if (request.email() != null && !request.email().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + request.email().toLowerCase() + "%"));
            }
            if (request.status() != null) {
                predicates.add(cb.equal(root.get("status"), request.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // -------------------------------------------------------------------------
    // Khóa tài khoản
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserStatusResponse lockUser(Long userId, Long actorId) {
        User user = findUserOrThrow(userId);

        if (userId.equals(actorId)) {
            throw new BusinessRuleException("Admin không thể tự khóa tài khoản của chính mình.");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessRuleException("Tài khoản đã ở trạng thái khóa.");
        }

        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);

        systemLogService.log(actorId, "USER_LOCKED", ENTITY_TYPE_USER, userId,
                "Admin khóa tài khoản: " + user.getEmail());

        return new UserStatusResponse(userId, UserStatus.LOCKED);
    }

    // -------------------------------------------------------------------------
    // Mở khóa tài khoản
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserStatusResponse unlockUser(Long userId, Long actorId) {
        User user = findUserOrThrow(userId);

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessRuleException("Tài khoản đang ở trạng thái hoạt động, không cần mở khóa.");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        systemLogService.log(actorId, "USER_UNLOCKED", ENTITY_TYPE_USER, userId,
                "Admin mở khóa tài khoản: " + user.getEmail());

        return new UserStatusResponse(userId, UserStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------
    // Xóa mềm tài khoản
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * Sau khi xóa mềm:
     * <ul>
     * <li>Access token (Redis {@code auth:access:{jti}}) bị xóa — Phase 2 sẽ bổ
     * sung
     * khi RefreshToken entity tồn tại để lấy được {@code jti}.</li>
     * <li>Refresh token sẽ bị revoke trong Phase 2 khi
     * {@code RefreshTokenRepository} có sẵn.</li>
     * </ul>
     */
    @Override
    @Transactional
    public void softDeleteUser(Long userId, Long actorId) {
        User user = findUserOrThrow(userId);

        if (userId.equals(actorId)) {
            throw new BusinessRuleException("Admin không thể tự xóa tài khoản của chính mình.");
        }

        user.setIsDeleted(true);
        user.setStatus(UserStatus.LOCKED); // Đảm bảo bị vô hiệu hóa hoàn toàn
        userRepository.save(user);

        // TODO Phase 2: revoke tất cả refresh token của user này
        // (RefreshTokenRepository)
        // TODO Phase 2: lấy access token jti từ refresh token và DEL khỏi Redis
        log.info("[AdminUserService] Đã xóa mềm user id={}. Revoke token sẽ được thực hiện ở Phase 2.", userId);

        systemLogService.log(actorId, "USER_SOFT_DELETED", ENTITY_TYPE_USER, userId,
                "Admin xóa mềm tài khoản: " + user.getEmail());
    }

    // -------------------------------------------------------------------------
    // Tạo tài khoản Manager
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * Manager được provisioned với {@code authProvider = GOOGLE}. Khi Manager
     * đăng nhập Google lần đầu với email này, hệ thống sẽ nhận ra và cấp JWT.
     */
    @Override
    @Transactional
    public UserSummaryResponse createManager(CreateManagerRequest request, Long actorId) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.email())) {
            throw new BusinessRuleException(
                    "Email '" + request.email() + "' đã được sử dụng bởi tài khoản khác trong hệ thống.");
        }

        var managerRole = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.MANAGER)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò MANAGER trong hệ thống."));

        User manager = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .authProvider(AuthProvider.GOOGLE)
                .status(UserStatus.ACTIVE)
                .role(managerRole)
                .isDeleted(false)
                .build();

        User saved = userRepository.save(manager);

        systemLogService.log(actorId, "MANAGER_CREATED", ENTITY_TYPE_USER, saved.getId(),
                "Admin tạo tài khoản Manager: " + request.email());

        return UserSummaryResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Tìm user theo ID (kể cả LOCKED), ném exception nếu không tìm thấy hoặc đã xóa
     * mềm.
     *
     * @param userId ID người dùng
     * @return User entity
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản người dùng với ID: " + userId));
    }
}
