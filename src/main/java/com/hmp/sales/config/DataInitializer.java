package com.hmp.sales.config;

import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.entity.CategoryStatus;
import com.hmp.sales.feature.category.repository.CategoryRepository;
import com.hmp.sales.feature.product.entity.*;
import com.hmp.sales.feature.product.repository.*;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Component khởi tạo dữ liệu mặc định cho hệ thống khi ứng dụng khởi động.
 * <p>
 * Thực hiện seed dữ liệu cho bảng {@code roles}, {@code permissions}, {@code role_permissions},
 * người dùng mẫu, danh mục master data (Brand, Unit, Color), cây danh mục phân cấp
 * và sản phẩm mẫu theo nguyên tắc <em>idempotent</em>: chỉ insert bản ghi chưa tồn tại.
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final UnitRepository unitRepository;
    private final ColorRepository colorRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductColorRepository productColorRepository;
    private final ProductPriceRepository productPriceRepository;
    private final com.hmp.sales.security.PermissionAuthorizationManager permissionAuthorizationManager;

    /**
     * Khởi tạo DataInitializer với các repository và manager cần thiết để seed dữ liệu.
     *
     * @param roleRepository                 repository quản lý vai trò
     * @param permissionRepository           repository quản lý quyền hạn
     * @param rolePermissionRepository       repository liên kết quyền và vai trò
     * @param userRepository                 repository quản lý người dùng
     * @param brandRepository                repository quản lý thương hiệu
     * @param unitRepository                 repository quản lý đơn vị tính
     * @param colorRepository                repository quản lý màu sắc
     * @param categoryRepository             repository quản lý danh mục
     * @param productRepository              repository quản lý sản phẩm
     * @param productVariantRepository       repository quản lý biến thể
     * @param productImageRepository         repository quản lý ảnh sản phẩm
     * @param productColorRepository         repository quản lý màu biến thể
     * @param productPriceRepository         repository quản lý bảng giá
     * @param permissionAuthorizationManager bộ quản lý phân quyền động trên RAM
     */
    public DataInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository,
            BrandRepository brandRepository,
            UnitRepository unitRepository,
            ColorRepository colorRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            ProductColorRepository productColorRepository,
            ProductPriceRepository productPriceRepository,
            com.hmp.sales.security.PermissionAuthorizationManager permissionAuthorizationManager) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.unitRepository = unitRepository;
        this.colorRepository = colorRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.productColorRepository = productColorRepository;
        this.productPriceRepository = productPriceRepository;
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
        seedMasterData();
        seedCategories();
        seedProducts();

        // Nạp dữ liệu vừa seed vào bộ nhớ đệm RAM của PermissionAuthorizationManager
        if (permissionAuthorizationManager != null) {
            permissionAuthorizationManager.loadCache();
        }

        log.info("[DataInitializer] Khởi tạo dữ liệu hoàn tất.");
    }

    // -------------------------------------------------------------------------
    // 1. Seed Roles
    // -------------------------------------------------------------------------

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

    private static final List<Object[]> PERMISSIONS = List.of(
            // --- PRODUCT module ---
            new Object[]{"PRODUCT_VIEW",             "PRODUCT",  "/api/v1/products/**",                           "GET",    "Xem danh sách và chi tiết sản phẩm"},
            new Object[]{"PRODUCT_PRICE_VIEW",        "PRODUCT",  "/api/v1/products/price",                        "GET",    "Xem giá sản phẩm (quyền cấu hình được bởi Admin)"},
            new Object[]{"PRODUCT_CREATE",            "PRODUCT",  "/api/v1/manager/categories/**/products",       "POST",   "Tạo sản phẩm mới trong danh mục"},
            new Object[]{"PRODUCT_UPDATE",            "PRODUCT",  "/api/v1/manager/products/**",                  "PATCH",  "Cập nhật thông tin sản phẩm"},
            new Object[]{"PRODUCT_LOCK",              "PRODUCT",  "/api/v1/manager/products/**/lock",             "POST",   "Khóa sản phẩm"},
            new Object[]{"PRODUCT_UNLOCK",            "PRODUCT",  "/api/v1/manager/products/**/unlock",           "POST",   "Mở khóa sản phẩm"},
            new Object[]{"PRODUCT_DELETE",            "PRODUCT",  "/api/v1/manager/products/**",                  "DELETE", "Xóa mềm sản phẩm"},
            new Object[]{"PRODUCT_IMAGE_MANAGE",      "PRODUCT",  "/api/v1/manager/products/**/images",           "POST",   "Thêm ảnh sản phẩm"},
            new Object[]{"PRODUCT_IMAGE_DELETE",      "PRODUCT",  "/api/v1/manager/products/**/images/**",        "DELETE", "Xóa ảnh sản phẩm"},
            new Object[]{"PRODUCT_VARIANT_CREATE",    "PRODUCT",  "/api/v1/manager/products/**/variants",         "POST",   "Thêm biến thể sản phẩm"},
            new Object[]{"PRODUCT_VARIANT_UPDATE",    "PRODUCT",  "/api/v1/manager/products/**/variants/**",      "PATCH",  "Cập nhật biến thể sản phẩm"},
            new Object[]{"PRODUCT_VARIANT_LOCK",      "PRODUCT",  "/api/v1/manager/products/**/variants/**/lock", "POST",   "Khóa biến thể sản phẩm"},
            new Object[]{"PRODUCT_VARIANT_UNLOCK",    "PRODUCT",  "/api/v1/manager/products/**/variants/**/unlock", "POST", "Mở khóa biến thể sản phẩm"},
            new Object[]{"PRODUCT_VARIANT_DELETE",    "PRODUCT",  "/api/v1/manager/products/**/variants/**",      "DELETE", "Xóa biến thể sản phẩm"},
            // --- CATEGORY module ---
            new Object[]{"PRODUCT_CATEGORY_VIEW",     "CATEGORY", "/api/v1/manager/categories/**",                "GET",    "Xem danh mục và sản phẩm quản lý"},
            new Object[]{"PRODUCT_CATEGORY_CREATE",   "CATEGORY", "/api/v1/manager/categories/**",                "POST",   "Tạo danh mục sản phẩm"},
            new Object[]{"PRODUCT_CATEGORY_UPDATE",   "CATEGORY", "/api/v1/manager/categories/**",                "PATCH",  "Cập nhật danh mục sản phẩm"},
            new Object[]{"PRODUCT_CATEGORY_LOCK",     "CATEGORY", "/api/v1/manager/categories/**/lock",          "POST",   "Khóa danh mục sản phẩm"},
            new Object[]{"PRODUCT_CATEGORY_UNLOCK",   "CATEGORY", "/api/v1/manager/categories/**/unlock",        "POST",   "Mở khóa danh mục sản phẩm"},
            new Object[]{"PRODUCT_CATEGORY_DELETE",   "CATEGORY", "/api/v1/manager/categories/**",                "DELETE", "Xóa danh mục sản phẩm"},
            // --- BRAND module (Master Data) ---
            new Object[]{"BRAND_VIEW_ALL",            "BRAND",    "/api/v1/admin/brands/**",                      "GET",    "Xem danh sách thương hiệu"},
            new Object[]{"BRAND_CREATE",              "BRAND",    "/api/v1/admin/brands",                         "POST",   "Tạo thương hiệu mới"},
            new Object[]{"BRAND_UPDATE",              "BRAND",    "/api/v1/admin/brands/**",                      "PATCH",  "Cập nhật thương hiệu"},
            new Object[]{"BRAND_LOCK",                "BRAND",    "/api/v1/admin/brands/**/lock",                 "POST",   "Khóa thương hiệu"},
            new Object[]{"BRAND_UNLOCK",              "BRAND",    "/api/v1/admin/brands/**/unlock",               "POST",   "Mở khóa thương hiệu"},
            new Object[]{"BRAND_DELETE",              "BRAND",    "/api/v1/admin/brands/**",                      "DELETE", "Xóa thương hiệu"},
            // --- UNIT module (Master Data) ---
            new Object[]{"UNIT_VIEW_ALL",             "UNIT",     "/api/v1/admin/units/**",                       "GET",    "Xem danh sách đơn vị tính"},
            new Object[]{"UNIT_CREATE",               "UNIT",     "/api/v1/admin/units",                          "POST",   "Tạo đơn vị tính mới"},
            new Object[]{"UNIT_UPDATE",               "UNIT",     "/api/v1/admin/units/**",                       "PATCH",  "Cập nhật đơn vị tính"},
            new Object[]{"UNIT_LOCK",                 "UNIT",     "/api/v1/admin/units/**/lock",                  "POST",   "Khóa đơn vị tính"},
            new Object[]{"UNIT_UNLOCK",               "UNIT",     "/api/v1/admin/units/**/unlock",                "POST",   "Mở khóa đơn vị tính"},
            new Object[]{"UNIT_DELETE",               "UNIT",     "/api/v1/admin/units/**",                       "DELETE", "Xóa đơn vị tính"},
            // --- COLOR module (Master Data) ---
            new Object[]{"COLOR_VIEW_ALL",            "COLOR",    "/api/v1/admin/colors/**",                      "GET",    "Xem danh sách màu sắc"},
            new Object[]{"COLOR_CREATE",              "COLOR",    "/api/v1/admin/colors",                         "POST",   "Tạo màu sắc mới"},
            new Object[]{"COLOR_UPDATE",              "COLOR",    "/api/v1/admin/colors/**",                      "PATCH",  "Cập nhật màu sắc"},
            new Object[]{"COLOR_LOCK",                "COLOR",    "/api/v1/admin/colors/**/lock",                 "POST",   "Khóa màu sắc"},
            new Object[]{"COLOR_UNLOCK",              "COLOR",    "/api/v1/admin/colors/**/unlock",               "POST",   "Mở khóa màu sắc"},
            new Object[]{"COLOR_DELETE",              "COLOR",    "/api/v1/admin/colors/**",                      "DELETE", "Xóa màu sắc"},
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

    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            "PRODUCT_VIEW", "PRODUCT_PRICE_VIEW",
            "PRODUCT_CATEGORY_VIEW", "PRODUCT_CATEGORY_CREATE", "PRODUCT_CATEGORY_UPDATE", "PRODUCT_CATEGORY_LOCK", "PRODUCT_CATEGORY_UNLOCK", "PRODUCT_CATEGORY_DELETE",
            "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_LOCK", "PRODUCT_UNLOCK", "PRODUCT_DELETE",
            "PRODUCT_IMAGE_MANAGE", "PRODUCT_IMAGE_DELETE",
            "PRODUCT_VARIANT_CREATE", "PRODUCT_VARIANT_UPDATE", "PRODUCT_VARIANT_LOCK", "PRODUCT_VARIANT_UNLOCK", "PRODUCT_VARIANT_DELETE",
            "BRAND_VIEW_ALL", "BRAND_CREATE", "BRAND_UPDATE", "BRAND_LOCK", "BRAND_UNLOCK", "BRAND_DELETE",
            "UNIT_VIEW_ALL", "UNIT_CREATE", "UNIT_UPDATE", "UNIT_LOCK", "UNIT_UNLOCK", "UNIT_DELETE",
            "COLOR_VIEW_ALL", "COLOR_CREATE", "COLOR_UPDATE", "COLOR_LOCK", "COLOR_UNLOCK", "COLOR_DELETE",
            "USER_VIEW_ALL", "USER_LOCK", "USER_UNLOCK", "USER_DELETE", "MANAGER_CREATE",
            "SYSTEM_LOG_VIEW", "SYSTEM_SETTING_UPDATE",
            "ROLE_PERMISSION_VIEW", "ROLE_PERMISSION_UPDATE"
    );

    private static final Set<String> MANAGER_PERMISSIONS = Set.of(
            "PRODUCT_VIEW", "PRODUCT_PRICE_VIEW",
            "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_LOCK", "PRODUCT_UNLOCK", "PRODUCT_DELETE",
            "PRODUCT_IMAGE_MANAGE", "PRODUCT_IMAGE_DELETE",
            "PRODUCT_VARIANT_CREATE", "PRODUCT_VARIANT_UPDATE", "PRODUCT_VARIANT_LOCK", "PRODUCT_VARIANT_UNLOCK", "PRODUCT_VARIANT_DELETE",
            "PRODUCT_CATEGORY_VIEW", "PRODUCT_CATEGORY_CREATE", "PRODUCT_CATEGORY_UPDATE", "PRODUCT_CATEGORY_LOCK", "PRODUCT_CATEGORY_UNLOCK", "PRODUCT_CATEGORY_DELETE",
            "ORDER_VIEW_ALL", "ORDER_CALCULATE", "ORDER_CANCEL_ANY", "ORDER_STATUS_UPDATE"
    );

    private static final Set<String> CUSTOMER_PERMISSIONS = Set.of(
            "PRODUCT_VIEW",
            "ORDER_CREATE", "ORDER_VIEW_OWN", "ORDER_CANCEL_OWN", "ORDER_CONFIRM_OWN",
            "ORDER_OTP_REQUEST", "ORDER_OTP_VERIFY",
            "ME_OTP_REQUEST", "ME_OTP_VERIFY"
    );

    private void seedRolePermissions() {
        Map<String, Permission> permMap = permissionRepository.findAllByIsDeletedFalse()
                .stream()
                .collect(Collectors.toMap(Permission::getPermissionName, Function.identity()));

        assignPermissionsToRole(RoleCode.ADMIN, ADMIN_PERMISSIONS, permMap);
        assignPermissionsToRole(RoleCode.MANAGER, MANAGER_PERMISSIONS, permMap);
        assignPermissionsToRole(RoleCode.CUSTOMER, CUSTOMER_PERMISSIONS, permMap);
    }

    private void assignPermissionsToRole(RoleCode roleCode,
                                         Set<String> permissionNames,
                                         Map<String, Permission> permMap) {
        Role role = roleRepository.findByRoleCodeAndIsDeletedFalse(roleCode).orElse(null);
        if (role == null) return;

        for (String permName : permissionNames) {
            Permission perm = permMap.get(permName);
            if (perm == null) continue;
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
    // 4. Seed Users
    // -------------------------------------------------------------------------

    private void seedUsers() {
        Role adminRole = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.ADMIN).orElse(null);
        Role managerRole = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.MANAGER).orElse(null);
        Role customerRole = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.CUSTOMER).orElse(null);

        if (adminRole == null || managerRole == null || customerRole == null) {
            log.warn("[DataInitializer] Thiếu vai trò hệ thống — không thể seed người dùng.");
            return;
        }

        String defaultPasswordHash = "$2a$10$wE44W7nL7jIuR26LdM2lYuvbZkH5Zl5J2P1L6c3kK9eB7uQ8aB6re";

        // Admin
        seedUserIfAbsent("Nguyễn Văn Admin", "admin@hmp.vn", "0901234561",
                defaultPasswordHash, AuthProvider.LOCAL, null, UserStatus.ACTIVE, adminRole);

        // Manager
        seedUserIfAbsent("Trần Thị Quản Lý", "manager1@hmp.vn", "0901234562",
                defaultPasswordHash, AuthProvider.LOCAL, null, UserStatus.ACTIVE, managerRole);
        seedUserIfAbsent("Lê Hoàng Quản Lý", "manager2@hmp.vn", "0901234563",
                defaultPasswordHash, AuthProvider.LOCAL, null, UserStatus.ACTIVE, managerRole);

        // Customer
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
        seedUserIfAbsent("Ngô Gia Bảo", "customer6@gmail.com", "0901234569",
                null, AuthProvider.GOOGLE, "google_sub_006", UserStatus.LOCKED, customerRole);
    }

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

    // -------------------------------------------------------------------------
    // 5. Seed Master Data (Brand, Unit, Color)
    // -------------------------------------------------------------------------

    private void seedMasterData() {
        // Brands
        List<String> brands = List.of("Tôn Hoa Sen", "Tôn Đông Á", "Tôn Nam Kim");
        for (String b : brands) {
            if (!brandRepository.existsByBrandNameAndIsDeletedFalse(b)) {
                brandRepository.save(Brand.builder().brandName(b).status(BrandStatus.ACTIVE).isDeleted(false).build());
                log.info("[DataInitializer] Đã tạo thương hiệu: {}", b);
            }
        }

        // Units
        List<String> units = List.of("m", "tấm", "cuộn", "cái");
        for (String u : units) {
            if (!unitRepository.existsByUnitNameAndIsDeletedFalse(u)) {
                unitRepository.save(Unit.builder().unitName(u).status(UnitStatus.ACTIVE).isDeleted(false).build());
                log.info("[DataInitializer] Đã tạo đơn vị tính: {}", u);
            }
        }

        // Colors
        List<String> colors = List.of("Xanh ngọc", "Đỏ đậm", "Xanh rêu", "Xám lông chuột", "Trắng sữa");
        for (String c : colors) {
            if (!colorRepository.existsByColorNameAndIsDeletedFalse(c)) {
                colorRepository.save(Color.builder().colorName(c).status(ColorStatus.ACTIVE).isDeleted(false).build());
                log.info("[DataInitializer] Đã tạo màu sắc: {}", c);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 6. Seed Categories
    // -------------------------------------------------------------------------

    private void seedCategories() {
        User manager = userRepository.findByEmailAndIsDeletedFalse("manager1@hmp.vn").orElse(null);
        if (manager == null) return;

        // 1. Tôn lợp mái
        Category roofing = categoryRepository.findByCategoryNameAndParentCategoryIsNullAndIsDeletedFalse("Tôn lợp mái")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("Tôn lợp mái")
                        .status(CategoryStatus.ACTIVE)
                        .createdBy(manager)
                        .isDeleted(false)
                        .build()));

        // Con của Tôn lợp mái: Tôn sóng vuông, Tôn sóng ngói
        if (!categoryRepository.existsByCategoryNameAndParentCategory_IdAndIsDeletedFalse("Tôn sóng vuông", roofing.getId())) {
            categoryRepository.save(Category.builder()
                    .categoryName("Tôn sóng vuông")
                    .parentCategory(roofing)
                    .status(CategoryStatus.ACTIVE)
                    .createdBy(manager)
                    .isDeleted(false)
                    .build());
        }
        if (!categoryRepository.existsByCategoryNameAndParentCategory_IdAndIsDeletedFalse("Tôn sóng ngói", roofing.getId())) {
            categoryRepository.save(Category.builder()
                    .categoryName("Tôn sóng ngói")
                    .parentCategory(roofing)
                    .status(CategoryStatus.ACTIVE)
                    .createdBy(manager)
                    .isDeleted(false)
                    .build());
        }

        // 2. Phụ kiện tôn
        Category accessories = categoryRepository.findByCategoryNameAndParentCategoryIsNullAndIsDeletedFalse("Phụ kiện tôn")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("Phụ kiện tôn")
                        .status(CategoryStatus.ACTIVE)
                        .createdBy(manager)
                        .isDeleted(false)
                        .build()));

        // Con của Phụ kiện tôn: Máng xối, Diềm nóc, Sườn tôn
        List<String> subAccessories = List.of("Máng xối", "Diềm nóc", "Sườn tôn");
        for (String sub : subAccessories) {
            if (!categoryRepository.existsByCategoryNameAndParentCategory_IdAndIsDeletedFalse(sub, accessories.getId())) {
                categoryRepository.save(Category.builder()
                        .categoryName(sub)
                        .parentCategory(accessories)
                        .status(CategoryStatus.ACTIVE)
                        .createdBy(manager)
                        .isDeleted(false)
                        .build());
            }
        }
    }

    // -------------------------------------------------------------------------
    // 7. Seed Sample Product
    // -------------------------------------------------------------------------

    private void seedProducts() {
        String sampleProductName = "Tôn Hoa Sen 11 sóng vuông";
        List<Product> existing = productRepository.findByCategoryIdAndStatusAndIsDeletedFalse(null, ProductStatus.ACTIVE);
        // Kiểm tra xem đã có sản phẩm tên này chưa
        boolean alreadyExists = productRepository.findAll().stream()
                .anyMatch(p -> !p.getIsDeleted() && sampleProductName.equals(p.getProductName()));
        if (alreadyExists) return;

        User manager = userRepository.findByEmailAndIsDeletedFalse("manager1@hmp.vn").orElse(null);
        Brand brand = brandRepository.findByBrandNameAndIsDeletedFalse("Tôn Hoa Sen").orElse(null);
        Unit unitMeter = unitRepository.findByUnitNameAndIsDeletedFalse("m").orElse(null);
        Unit unitSheet = unitRepository.findByUnitNameAndIsDeletedFalse("tấm").orElse(null);
        Color colorGreen = colorRepository.findByColorNameAndIsDeletedFalse("Xanh ngọc").orElse(null);
        Color colorRed = colorRepository.findByColorNameAndIsDeletedFalse("Đỏ đậm").orElse(null);

        Category subCategory = categoryRepository.findAllByIsDeletedFalse().stream()
                .filter(c -> "Tôn sóng vuông".equals(c.getCategoryName()))
                .findFirst()
                .orElse(null);

        if (manager == null || brand == null || subCategory == null || unitMeter == null || unitSheet == null) {
            return;
        }

        // Tạo Product
        Product product = Product.builder()
                .productName(sampleProductName)
                .description("Tôn lợp mái 11 sóng vuông công nghệ mạ kẽm phủ màu cao cấp Hoa Sen.")
                .status(ProductStatus.ACTIVE)
                .category(subCategory)
                .brand(brand)
                .createdBy(manager)
                .isDeleted(false)
                .build();
        product = productRepository.save(product);

        // Ảnh chính
        ProductImage primaryImage = ProductImage.builder()
                .product(product)
                .imageUrl("https://placehold.co/600x400/png?text=Ton+Hoa+Sen+11+Song")
                .isPrimary(true)
                .displayOrder(0)
                .isDeleted(false)
                .build();
        productImageRepository.save(primaryImage);

        // Biến thể 1: 0.40mm
        ProductVariant variant1 = ProductVariant.builder()
                .product(product)
                .variantCode("VAR-HS11-040")
                .variantName("Tôn Hoa Sen 11 sóng - 0.40mm")
                .typeValue("11 sóng vuông")
                .thickness(new BigDecimal("0.40"))
                .status(VariantStatus.ACTIVE)
                .isDeleted(false)
                .build();
        variant1 = productVariantRepository.save(variant1);

        if (colorGreen != null) {
            productColorRepository.save(ProductColor.builder()
                    .id(new ProductColorId(variant1.getId(), colorGreen.getId()))
                    .variant(variant1).color(colorGreen).isDeleted(false).build());
        }
        if (colorRed != null) {
            productColorRepository.save(ProductColor.builder()
                    .id(new ProductColorId(variant1.getId(), colorRed.getId()))
                    .variant(variant1).color(colorRed).isDeleted(false).build());
        }

        productPriceRepository.save(ProductPrice.builder()
                .id(new ProductPriceId(variant1.getId(), unitMeter.getId()))
                .variant(variant1).unit(unitMeter).price(95000).isDeleted(false).build());
        productPriceRepository.save(ProductPrice.builder()
                .id(new ProductPriceId(variant1.getId(), unitSheet.getId()))
                .variant(variant1).unit(unitSheet).price(145000).isDeleted(false).build());

        // Biến thể 2: 0.45mm
        ProductVariant variant2 = ProductVariant.builder()
                .product(product)
                .variantCode("VAR-HS11-045")
                .variantName("Tôn Hoa Sen 11 sóng - 0.45mm")
                .typeValue("11 sóng vuông")
                .thickness(new BigDecimal("0.45"))
                .status(VariantStatus.ACTIVE)
                .isDeleted(false)
                .build();
        variant2 = productVariantRepository.save(variant2);

        if (colorGreen != null) {
            productColorRepository.save(ProductColor.builder()
                    .id(new ProductColorId(variant2.getId(), colorGreen.getId()))
                    .variant(variant2).color(colorGreen).isDeleted(false).build());
        }
        productPriceRepository.save(ProductPrice.builder()
                .id(new ProductPriceId(variant2.getId(), unitMeter.getId()))
                .variant(variant2).unit(unitMeter).price(110000).isDeleted(false).build());
        productPriceRepository.save(ProductPrice.builder()
                .id(new ProductPriceId(variant2.getId(), unitSheet.getId()))
                .variant(variant2).unit(unitSheet).price(165000).isDeleted(false).build());

        log.info("[DataInitializer] Đã khởi tạo sản phẩm mẫu thành công: {}", sampleProductName);
    }
}
