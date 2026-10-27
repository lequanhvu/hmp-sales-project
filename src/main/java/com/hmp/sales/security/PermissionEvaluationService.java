package com.hmp.sales.security;

import org.springframework.security.core.Authentication;

/**
 * Service kiểm tra quyền hạn động của người dùng hiện tại phục vụ tầng nghiệp vụ (Service Layer).
 * <p>
 * Do token Access Token chỉ lưu danh sách vai trò (roles) để tránh token bloat,
 * service này tra cứu quyền tương ứng của vai trò từ bộ nhớ đệm RAM (Singleton)
 * thông qua {@link PermissionAuthorizationManager} — không query DB mỗi request.
 * <p>
 * Dùng trong {@code @PreAuthorize} hoặc service layer để kiểm tra quyền nghiệp vụ,
 * đặc biệt là {@code PRODUCT_PRICE_VIEW} phục vụ ẩn/hiện giá sản phẩm.
 */
public interface PermissionEvaluationService {

    /**
     * Kiểm tra xem người dùng hiện tại có quyền hạn được chỉ định hay không.
     * Tra cứu quyền từ danh sách vai trò của người dùng trong cache RAM.
     *
     * @param authentication đối tượng xác thực hiện tại từ Spring Security context
     * @param permissionName tên quyền hạn cần kiểm tra (ví dụ: {@code "PRODUCT_PRICE_VIEW"})
     * @return {@code true} nếu người dùng có quyền, {@code false} nếu không có hoặc chưa xác thực
     */
    boolean hasPermission(Authentication authentication, String permissionName);

    /**
     * Kiểm tra quyền hạn của người dùng đang đăng nhập trong SecurityContextHolder hiện tại.
     *
     * @param permissionName tên quyền hạn cần kiểm tra
     * @return {@code true} nếu người dùng có quyền, {@code false} nếu không có hoặc chưa xác thực
     */
    boolean hasPermission(String permissionName);

    /**
     * Kiểm tra xem người dùng hiện tại có quyền xem giá sản phẩm hay không.
     * <p>
     * Đây là kiểm tra quan trọng nhất theo nghiệp vụ: giá sản phẩm chỉ
     * được trả về khi caller có quyền {@code PRODUCT_PRICE_VIEW}.
     * Không được hard-code theo vai trò — chỉ kiểm tra qua permission.
     *
     * @param authentication đối tượng xác thực hiện tại
     * @return {@code true} nếu có quyền xem giá, {@code false} nếu không
     */
    boolean canViewProductPrice(Authentication authentication);

    /**
     * Kiểm tra xem người dùng trong SecurityContext hiện tại có quyền xem giá sản phẩm hay không.
     *
     * @return {@code true} nếu có quyền xem giá, {@code false} nếu không
     */
    boolean canViewProductPrice();
}
