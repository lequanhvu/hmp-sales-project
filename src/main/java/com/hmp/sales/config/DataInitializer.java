package com.hmp.sales.config;

import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.Permission;
import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.RolePermission;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.repository.PermissionRepository;
import com.hmp.sales.feature.user.repository.RolePermissionRepository;
import com.hmp.sales.feature.user.repository.RoleRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Component khởi tạo dữ liệu mặc định cho hệ thống khi ứng dụng khởi động.
 * <p>
 * Thực hiện seed dữ liệu cho bảng {@code roles}, {@code permissions}, và
 * {@code role_permissions} theo nguyên tắc <em>idempotent</em>: chỉ insert
 * bản ghi chưa tồn tại, không ghi đè dữ liệu hiện có.
 * <p>
 * Thứ tự khởi tạo:
 * <ol>
 *     <li>Tạo 3 vai trò: ADMIN, MANAGER, CUSTOMER</li>
 *     <li>Tạo toàn bộ quyền hạn theo định nghĩa trong {@code database.md}</li>
 *     <li>Gán quyền mặc định cho từng vai trò</li>
 * </ol>
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final com.hmp.sales.security.PermissionAuthorizationManager permissionAuthorizationManager;

    /**
     * Khởi tạo DataInitializer với các repository và manager cần thiết để seed dữ liệu.
     *
     * @param roleRepository                 repository quản lý vai trò
     * @param permissionRepository           repository quản lý quyền hạn
     * @param rolePermissionRepository       repository liên kết quyền và vai trò
     * @param userRepository                 repository quản lý người dùng
     * @param permissionAuthorizationManager bộ quản lý phân quyền động trên RAM
     */
    public DataInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository,
            com.hmp.sales.security.PermissionAuthorizationManager permissionAuthorizationManager) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.permissionAuthorizationManager = permissionAuthorizationManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[DataInitializer] Bắt đầu khởi tạo dữ liệu mặc định...");
        seedRoles();
        seedPermissions();
        seedRolePermissions();
        seedUsers();

        // Nạp dữ liệu vừa seed vào bộ nhớ đệm RAM của PermissionAuthorizationManager
        if (permissionAuthorizationManager != null) {
            permissionAuthorizationManager.loadCache();
        }

        log.info("[DataInitializer] Khởi tạo dữ liệu hoàn tất.");
    }

    // -------------------------------------------------------------------------
    // 1. Seed Roles
    // -------------------------------------------------------------------------

    /**
     * Khởi tạo 3 vai trò mặc định: ADMIN, MANAGER, CUSTOMER.
     * Bỏ qua nếu vai trò đã tồn tại trong DB.
     */
    private void seedRoles() {
        seedRoleIfAbsent(RoleCode.ADMIN, "Quản trị viên", "Vai trò có toàn quyền quản trị hệ thống");
        seedRoleIfAbsent(RoleCode.MANAGER, "Quản lý", "Vai trò quản lý sản phẩm, danh mục và đơn hàng");
        seedRoleIfAbsent(RoleCode.CUSTOMER, "Khách hàng", "Vai trò dành cho khách hàng đặt hàng");
    }

    private void seedRoleIfAbsent(RoleCode code, String name, String description) {
        if (!roleRepository.existsByRoleCodeAndIsDeletedFalse(code)) {
            Role role = Role.builder()
                    .roleCode(code)
                    .roleName(name)
                    .description(description)
                    .isDeleted(false)
                    .build();
            roleRepository.save(role);
            log.info("[DataInitializer] Đã tạo vai trò: {}", code);
        }
    }

    // -------------------------------------------------------------------------
    // 2. Seed Permissions
    // -------------------------------------------------------------------------

    /**
     * Khai báo toàn bộ quyền hạn hệ thống theo {@code database.md}.
     * Mỗi mục là: permissionName, module, apiPath, method, description.
     */
    private static final List<Object[]> PERMISSIONS = List.of(
            // --- PRODUCT module ---
            new Object[]{"PRODUCT_VIEW",             "PRODUCT",  "/api/v1/products",                              "GET",    "Xem danh sách và chi tiết sản phẩm"},
            new Object[]{"PRODUCT_PRICE_VIEW",        "PRODUCT",  "/api/v1/products/price",                        "GET",    "Xem giá sản phẩm (quyền cấu hình được bởi Admin)"},
            new Object[]{"PRODUCT_CREATE",            "PRODUCT",  "/api/v1/manager/categories/{id}/products",      "POST",   "Tạo sản phẩm mới trong danh mục"},
            new Object[]{"PRODUCT_UPDATE",            "PRODUCT",  "/api/v1/manager/products/{id}",                 "PATCH",  "Cập nhật thông tin sản phẩm"},
            new Object[]{"PRODUCT_LOCK",              "PRODUCT",  "/api/v1/manager/products/{id}/lock",            "POST",   "Khóa/mở khóa sản phẩm"},
            // --- CATEGORY module ---
            new Object[]{"PRODUCT_CATEGORY_CREATE",   "CATEGORY", "/api/v1/manager/categories",                   "POST",   "Tạo danh mục sản phẩm"},
            new Object[]{"PRODUCT_CATEGORY_UPDATE",   "CATEGORY", "/api/v1/manager/categories/{id}",              "PATCH",  "Cập nhật danh mục sản phẩm"},
            new Object[]{"PRODUCT_CATEGORY_DELETE",   "CATEGORY", "/api/v1/manager/categories/{id}",              "DELETE", "Xóa danh mục sản phẩm"},
            // --- ORDER module ---
            new Object[]{"ORDER_CREATE",              "ORDER",    "/api/v1/orders",                               "POST",   "Tạo đơn hàng mới"},
            new Object[]{"ORDER_VIEW_OWN",            "ORDER",    "/api/v1/orders",                               "GET",    "Xem danh sách đơn hàng của chính mình"},
            new Object[]{"ORDER_CANCEL_OWN",          "ORDER",    "/api/v1/orders/{id}/cancel",                   "POST",   "Hủy đơn hàng của chính mình (trong cửa sổ cho phép)"},
            new Object[]{"ORDER_CONFIRM_OWN",         "ORDER",    "/api/v1/orders/{id}/confirm",                  "POST",   "Xác nhận đơn hàng đã được tính giá"},
            new Object[]{"ORDER_OTP_REQUEST",         "ORDER",    "/api/v1/orders/otp/request",                   "POST",   "Gửi mã OTP xác thực số điện thoại"},
            new Object[]{"ORDER_OTP_VERIFY",          "ORDER",    "/api/v1/orders/otp/verify",                    "POST",   "Xác minh mã OTP số điện thoại"},
            new Object[]{"ME_OTP_REQUEST",            "ORDER",    "/api/v1/me/otp/request",                       "POST",   "Gửi mã OTP xác minh số điện thoại cá nhân"},
            new Object[]{"ME_OTP_VERIFY",             "ORDER",    "/api/v1/me/otp/verify",                        "POST",   "Xác minh mã OTP số điện thoại cá nhân"},
            new Object[]{"ORDER_VIEW_ALL",            "ORDER",    "/api/v1/manager/orders",                       "GET",    "Xem toàn bộ đơn hàng trong hệ thống"},
            new Object[]{"ORDER_CALCULATE",           "ORDER",    "/api/v1/manager/orders/{id}/calculate",        "POST",   "Tính giá đơn hàng sản xuất"},
            new Object[]{"ORDER_CANCEL_ANY",          "ORDER",    "/api/v1/manager/orders/{id}/cancel",           "POST",   "Hủy bất kỳ đơn hàng nào"},
            new Object[]{"ORDER_STATUS_UPDATE",       "ORDER",    "/api/v1/manager/orders/{id}/status",           "PATCH",  "Cập nhật trạng thái đơn hàng"},
            // --- USER module ---
            new Object[]{"USER_VIEW_ALL",             "USER",     "/api/v1/admin/users",                          "GET",    "Xem danh sách tài khoản người dùng"},
            new Object[]{"USER_LOCK",                 "USER",     "/api/v1/admin/users/{id}/lock",                "POST",   "Khóa tài khoản người dùng"},
            new Object[]{"USER_UNLOCK",               "USER",     "/api/v1/admin/users/{id}/unlock",              "POST",   "Mở khóa tài khoản người dùng"},
            new Object[]{"USER_DELETE",               "USER",     "/api/v1/admin/users/{id}",                     "DELETE", "Xóa mềm tài khoản người dùng"},
            new Object[]{"MANAGER_CREATE",            "USER",     "/api/v1/admin/managers",                       "POST",   "Tạo tài khoản quản lý mới"},
            // --- SYSTEM module ---
            new Object[]{"SYSTEM_LOG_VIEW",           "SYSTEM",   "/api/v1/admin/logs",                           "GET",    "Xem nhật ký hệ thống"},
            new Object[]{"SYSTEM_SETTING_UPDATE",     "SYSTEM",   "/api/v1/admin/settings/{key}",                 "PUT",    "Cập nhật cấu hình hệ thống"},
            // --- ROLE module ---
            new Object[]{"ROLE_PERMISSION_VIEW",      "ROLE",     "/api/v1/admin/roles/{id}/permissions",         "GET",    "Xem danh sách quyền hạn của vai trò"},
            new Object[]{"ROLE_PERMISSION_UPDATE",    "ROLE",     "/api/v1/admin/roles/{id}/permissions",         "PUT",    "Thay đổi danh sách quyền hạn của vai trò"}
    );

    /**
     * Khởi tạo toàn bộ quyền hạn hệ thống.
     * Bỏ qua quyền đã tồn tại theo {@code permissionName}.
     */
    private void seedPermissions() {
        for (Object[] p : PERMISSIONS) {
            String permName = (String) p[0];
            if (permissionRepository.findByPermissionNameAndIsDeletedFalse(permName).isEmpty()) {
                Permission perm = Permission.builder()
                        .permissionName(permName)
                        .module((String) p[1])
                        .apiPath((String) p[2])
                        .method((String) p[3])
                        .description((String) p[4])
                        .isDeleted(false)
                        .build();
                permissionRepository.save(perm);
                log.info("[DataInitializer] Đã tạo quyền: {}", permName);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. Seed RolePermissions
    // -------------------------------------------------------------------------

    /** Danh sách quyền mặc định của ADMIN. */
    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            "PRODUCT_VIEW", "PRODUCT_PRICE_VIEW",
            "USER_VIEW_ALL", "USER_LOCK", "USER_UNLOCK", "USER_DELETE", "MANAGER_CREATE",
            "SYSTEM_LOG_VIEW", "SYSTEM_SETTING_UPDATE",
            "ROLE_PERMISSION_VIEW", "ROLE_PERMISSION_UPDATE"
    );

    /** Danh sách quyền mặc định của MANAGER. */
    private static final Set<String> MANAGER_PERMISSIONS = Set.of(
            "PRODUCT_VIEW", "PRODUCT_PRICE_VIEW",
            "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_LOCK",
            "PRODUCT_CATEGORY_CREATE", "PRODUCT_CATEGORY_UPDATE", "PRODUCT_CATEGORY_DELETE",
            "ORDER_VIEW_ALL", "ORDER_CALCULATE", "ORDER_CANCEL_ANY", "ORDER_STATUS_UPDATE"
    );

    /**
     * Danh sách quyền mặc định của CUSTOMER.
     * Lưu ý: PRODUCT_PRICE_VIEW không có trong danh sách này ban đầu.
     * Admin có thể bổ sung sau thông qua API quản lý phân quyền.
     */
    private static final Set<String> CUSTOMER_PERMISSIONS = Set.of(
            "PRODUCT_VIEW",
            "ORDER_CREATE", "ORDER_VIEW_OWN", "ORDER_CANCEL_OWN", "ORDER_CONFIRM_OWN",
            "ORDER_OTP_REQUEST", "ORDER_OTP_VERIFY",
            "ME_OTP_REQUEST", "ME_OTP_VERIFY"
    );

    /**
     * Gán quyền hạn mặc định cho 3 vai trò hệ thống.
     * Chỉ tạo liên kết chưa tồn tại; không xóa hoặc ghi đè liên kết hiện có.
     */
    private void seedRolePermissions() {
        Map<String, Permission> permMap = permissionRepository.findAllByIsDeletedFalse()
                .stream()
                .collect(Collectors.toMap(Permission::getPermissionName, Function.identity()));

        assignPermissionsToRole(RoleCode.ADMIN, ADMIN_PERMISSIONS, permMap);
        assignPermissionsToRole(RoleCode.MANAGER, MANAGER_PERMISSIONS, permMap);
        assignPermissionsToRole(RoleCode.CUSTOMER, CUSTOMER_PERMISSIONS, permMap);
    }

    /**
     * Gán các quyền vào một vai trò cụ thể (bỏ qua nếu đã được gán).
     *
     * @param roleCode mã vai trò
     * @param permissionNames tập hợp tên quyền cần gán
     * @param permMap bản đồ permissionName -> Permission entity
     */
    private void assignPermissionsToRole(RoleCode roleCode,
                                         Set<String> permissionNames,
                                         Map<String, Permission> permMap) {
        Role role = roleRepository.findByRoleCodeAndIsDeletedFalse(roleCode).orElse(null);
        if (role == null) {
            log.warn("[DataInitializer] Không tìm thấy vai trò {} — bỏ qua gán quyền.", roleCode);
            return;
        }

        for (String permName : permissionNames) {
            Permission perm = permMap.get(permName);
            if (perm == null) {
                log.warn("[DataInitializer] Không tìm thấy quyền {} — bỏ qua.", permName);
                continue;
            }
            boolean exists = rolePermissionRepository
                    .findByIdRoleIdAndIdPermissionId(role.getId(), perm.getId())
                    .isPresent();
            if (!exists) {
                RolePermission rp = new RolePermission(role, perm);
                rolePermissionRepository.save(rp);
                log.info("[DataInitializer] Đã gán quyền {} cho vai trò {}.", permName, roleCode);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4. Seed Users (10 người dùng mẫu phục vụ kiểm thử)
    // -------------------------------------------------------------------------

    /**
     * Khởi tạo 10 tài khoản người dùng mẫu phục vụ môi trường kiểm thử (idempotent).
     * <p>
     * Danh sách bao gồm các vai trò và trạng thái khác nhau:
     * <ul>
     *   <li>1 Admin (Active)</li>
     *   <li>2 Manager (Active)</li>
     *   <li>5 Customer (Active)</li>
     *   <li>2 Customer (Locked - dùng kiểm thử tính năng lọc và mở khóa)</li>
     * </ul>
     * </p>
     */
    private void seedUsers() {
        Role adminRole = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.ADMIN).orElse(null);
        Role managerRole = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.MANAGER).orElse(null);
        Role customerRole = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.CUSTOMER).orElse(null);

        if (adminRole == null || managerRole == null || customerRole == null) {
            log.warn("[DataInitializer] Thiếu vai trò hệ thống — không thể seed người dùng.");
            return;
        }

        // Mật khẩu mẫu băm an toàn ($2a$10$...) cho tài khoản LOCAL: "Password@123"
        String defaultPasswordHash = "$2a$10$wE44W7nL7jIuR26LdM2lYuvbZkH5Zl5J2P1L6c3kK9eB7uQ8aB6re";

        // 1. Admin
        seedUserIfAbsent("Nguyễn Văn Admin", "admin@hmp.vn", "0901234561",
                defaultPasswordHash, AuthProvider.LOCAL, null, UserStatus.ACTIVE, adminRole);

        // 2-3. Manager
        seedUserIfAbsent("Trần Thị Quản Lý", "manager1@hmp.vn", "0901234562",
                defaultPasswordHash, AuthProvider.LOCAL, null, UserStatus.ACTIVE, managerRole);
        seedUserIfAbsent("Lê Hoàng Quản Lý", "manager2@hmp.vn", "0901234563",
                defaultPasswordHash, AuthProvider.LOCAL, null, UserStatus.ACTIVE, managerRole);

        // 4-8. Customer Active (Google OAuth)
        seedUserIfAbsent("Phạm Minh Khách", "customer1@gmail.com", "0901234564",
                null, AuthProvider.GOOGLE, "google_sub_001", UserStatus.ACTIVE, customerRole);
        seedUserIfAbsent("Hoàng Đức Anh", "customer2@gmail.com", "0901234565",
                null, AuthProvider.GOOGLE, "google_sub_002", UserStatus.ACTIVE, customerRole);
        seedUserIfAbsent("Vũ Thu Trang", "customer3@gmail.com", "0901234566",
                null, AuthProvider.GOOGLE, "google_sub_003", UserStatus.ACTIVE, customerRole);
        seedUserIfAbsent("Đặng Tuấn Kiệt", "customer4@gmail.com", "0901234567",
                null, AuthProvider.GOOGLE, "google_sub_004", UserStatus.ACTIVE, customerRole);
        seedUserIfAbsent("Bùi Ngọc Hân", "customer5@gmail.com", "0901234568",
                null, AuthProvider.GOOGLE, "google_sub_005", UserStatus.ACTIVE, customerRole);

        // 9-10. Customer Locked (Dùng test chức năng khóa / mở khóa / lọc status)
        seedUserIfAbsent("Ngô Gia Bảo", "customer6@gmail.com", "0901234569",
                null, AuthProvider.GOOGLE, "google_sub_006", UserStatus.LOCKED, customerRole);
        seedUserIfAbsent("Đỗ Thùy Dung", "customer7@gmail.com", "0901234570",
                null, AuthProvider.GOOGLE, "google_sub_007", UserStatus.LOCKED, customerRole);
    }

    /**
     * Lưu người dùng mới nếu email chưa tồn tại trong hệ thống.
     *
     * @param fullName        họ và tên
     * @param email           địa chỉ email duy nhất
     * @param phone           số điện thoại
     * @param passwordHash    mật khẩu đã băm
     * @param authProvider    nhà cung cấp xác thực (LOCAL/GOOGLE)
     * @param providerSubject mã subject Google
     * @param status          trạng thái tài khoản (ACTIVE/LOCKED)
     * @param role            vai trò được gán
     */
    private void seedUserIfAbsent(String fullName, String email, String phone,
                                  String passwordHash, AuthProvider authProvider,
                                  String providerSubject, UserStatus status, Role role) {
        if (!userRepository.existsByEmailAndIsDeletedFalse(email)) {
            User user = User.builder()
                    .fullName(fullName)
                    .email(email)
                    .phone(phone)
                    .passwordHash(passwordHash)
                    .authProvider(authProvider)
                    .providerSubject(providerSubject)
                    .status(status)
                    .role(role)
                    .isDeleted(false)
                    .build();
            userRepository.save(user);
            log.info("[DataInitializer] Đã khởi tạo người dùng mẫu: {} ({}) - {}", fullName, email, role.getRoleCode());
        }
    }
}
