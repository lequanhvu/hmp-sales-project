package com.hmp.sales.security;

import com.hmp.sales.feature.user.entity.Permission;
import com.hmp.sales.feature.user.entity.RolePermission;
import com.hmp.sales.feature.user.repository.RolePermissionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Bộ quản lý phân quyền động phía máy chủ sử dụng bộ nhớ RAM (Dynamic In-Memory Permission Authorization).
 * <p>
 * Triển khai {@link AuthorizationManager} cho {@link RequestAuthorizationContext}.
 * Khi ứng dụng khởi động, nạp toàn bộ danh sách vai trò (Role) kèm các quyền hạn (Permission)
 * tương ứng vào bộ nhớ đệm {@code rolePermissionsCache} (Singleton).
 * <p>
 * Trên mỗi request được gửi đến:
 * <ol>
 *     <li>Lấy thông tin xác thực {@link Authentication} và danh sách vai trò (roles) của người dùng.</li>
 *     <li>Trích xuất đường dẫn request (URI) và phương thức HTTP (GET, POST, PUT, DELETE, PATCH).</li>
 *     <li>Lấy danh sách Permission tương ứng với các vai trò của người dùng từ cache RAM.</li>
 *     <li>Sử dụng {@link AntPathMatcher} để so sánh khớp giữa request và quy tắc {@code (apiPath, method)}.</li>
 *     <li>Cấp quyền nếu tìm thấy ít nhất một quyền hợp lệ, ngược lại từ chối truy cập (403 Forbidden).</li>
 * </ol>
 */
@Slf4j
@Component
public class PermissionAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final RolePermissionRepository rolePermissionRepository;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * Bộ nhớ đệm lưu trữ danh sách quyền theo mã vai trò.
     * Khóa là tên vai trò (ví dụ: "ADMIN", "ROLE_ADMIN", "CUSTOMER", "ROLE_CUSTOMER").
     */
    private Map<String, List<Permission>> rolePermissionsCache = new ConcurrentHashMap<>();

    /**
     * Khởi tạo PermissionAuthorizationManager với repository truy xuất quyền hạn.
     *
     * @param rolePermissionRepository repository liên kết vai trò và quyền hạn
     */
    public PermissionAuthorizationManager(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Khởi tạo và nạp dữ liệu phân quyền vào bộ nhớ đệm ngay khi bean được tạo.
     */
    @PostConstruct
    public void init() {
        loadCache();
    }

    /**
     * Nạp lại toàn bộ dữ liệu phân quyền từ cơ sở dữ liệu vào bộ nhớ RAM.
     * <p>
     * Phương thức này được đánh dấu {@code synchronized} để đảm bảo tính an toàn đa luồng.
     * Được gọi khi:
     * <ul>
     *     <li>Ứng dụng khởi động (Startup).</li>
     *     <li>DataInitializer hoàn tất việc seed dữ liệu mặc định.</li>
     *     <li>Admin cập nhật danh sách quyền cho vai trò (Cache Invalidation).</li>
     * </ul>
     */
    public synchronized void loadCache() {
        log.info("[PermissionAuthorizationManager] Đang nạp lại cache phân quyền vào RAM...");
        List<RolePermission> activeLinks = rolePermissionRepository.findAllActiveRolePermissions();

        Map<String, List<Permission>> newCache = new ConcurrentHashMap<>();

        for (RolePermission link : activeLinks) {
            if (link.getRole() == null || link.getRole().getRoleCode() == null || link.getPermission() == null) {
                continue;
            }
            String roleCode = link.getRole().getRoleCode().name();
            Permission permission = link.getPermission();

            // Lưu cả 2 dạng: không có tiền tố ("ADMIN") và có tiền tố Spring Security chuẩn ("ROLE_ADMIN")
            newCache.computeIfAbsent(roleCode, k -> new ArrayList<>()).add(permission);
            newCache.computeIfAbsent("ROLE_" + roleCode, k -> new ArrayList<>()).add(permission);
        }

        // Bọc các list bằng unmodifiableList để tránh sửa đổi ngoài ý muốn
        for (Map.Entry<String, List<Permission>> entry : newCache.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }

        this.rolePermissionsCache = newCache;
        log.info("[PermissionAuthorizationManager] Nạp cache hoàn tất. Tổng số vai trò được ánh xạ: {}", newCache.size());
    }

    /**
     * Thực hiện kiểm tra quyền truy cập cho request hiện tại.
     *
     * @param authenticationSupplier đối tượng cung cấp thông tin xác thực của caller
     * @param context                ngữ cảnh yêu cầu HTTP chứa {@link HttpServletRequest}
     * @return {@link AuthorizationDecision} chấp thuận (true) hoặc từ chối (false)
     */
    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();

        // 1. Kiểm tra trạng thái xác thực
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }

        HttpServletRequest request = context.getRequest();
        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            log.warn("[PermissionAuthorizationManager] Người dùng '{}' không có bất kỳ quyền/vai trò nào.", authentication.getName());
            return new AuthorizationDecision(false);
        }

        // 2. Duyệt qua từng vai trò của người dùng để so khớp quyền
        for (GrantedAuthority authority : authorities) {
            String roleName = authority.getAuthority();
            List<Permission> permissions = rolePermissionsCache.get(roleName);

            if (permissions != null && !permissions.isEmpty()) {
                for (Permission permission : permissions) {
                    if (isPermissionMatched(permission, requestPath, httpMethod)) {
                        log.debug("[PermissionAuthorizationManager] Cho phép truy cập: User '{}' với vai trò '{}' khớp quyền '{}' ({}, {})",
                                authentication.getName(), roleName, permission.getPermissionName(), permission.getMethod(), permission.getApiPath());
                        return new AuthorizationDecision(true);
                    }
                }
            }
        }

        log.warn("[PermissionAuthorizationManager] Từ chối truy cập: User '{}' không có quyền truy cập {} {}",
                authentication.getName(), httpMethod, requestPath);
        return new AuthorizationDecision(false);
    }

    /**
     * Kiểm tra xem người dùng hiện tại có sở hữu quyền hạn cụ thể (theo tên quyền) hay không.
     * <p>
     * Dùng cho các tầng dịch vụ (Service Layer) kiểm tra quyền nghiệp vụ như lọc dữ liệu,
     * tiêu biểu là {@code PRODUCT_PRICE_VIEW}.
     *
     * @param authentication đối tượng xác thực của caller
     * @param permissionName tên quyền cần kiểm tra (ví dụ: {@code "PRODUCT_PRICE_VIEW"})
     * @return {@code true} nếu bất kỳ vai trò nào của người dùng có quyền này trong cache, ngược lại {@code false}
     */
    public boolean hasPermission(Authentication authentication, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }

        for (GrantedAuthority authority : authorities) {
            List<Permission> permissions = rolePermissionsCache.get(authority.getAuthority());
            if (permissions != null) {
                for (Permission p : permissions) {
                    if (permissionName.equals(p.getPermissionName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Lấy bản sao đọc của bộ nhớ đệm phân quyền phục vụ mục đích kiểm thử và theo dõi.
     *
     * @return bản đồ vai trò và danh sách quyền hạn
     */
    public Map<String, List<Permission>> getRolePermissionsCache() {
        return Collections.unmodifiableMap(rolePermissionsCache);
    }

    /**
     * So khớp đường dẫn API và phương thức HTTP giữa quyền trong cấu hình và request thực tế.
     *
     * @param permission  quyền hạn cấu hình trong hệ thống
     * @param requestPath đường dẫn API của request thực tế
     * @param httpMethod  phương thức HTTP của request thực tế
     * @return {@code true} nếu khớp cả phương thức và đường dẫn theo Ant pattern
     */
    private boolean isPermissionMatched(Permission permission, String requestPath, String httpMethod) {
        if (permission.getMethod() == null || permission.getApiPath() == null) {
            return false;
        }

        if (!permission.getMethod().equalsIgnoreCase(httpMethod)) {
            return false;
        }

        return antPathMatcher.match(permission.getApiPath(), requestPath);
    }
}
