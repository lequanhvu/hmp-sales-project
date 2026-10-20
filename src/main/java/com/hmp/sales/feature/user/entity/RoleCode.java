package com.hmp.sales.feature.user.entity;

/**
 * Định danh các vai trò người dùng trong hệ thống bán hàng HMP.
 * <p>
 * Hệ thống hỗ trợ 3 vai trò chính:
 * <ul>
 *     <li>{@link #ADMIN}: Quản trị viên hệ thống có toàn quyền cấu hình, phân quyền, quản lý tài khoản.</li>
 *     <li>{@link #MANAGER}: Quản lý nghiệp vụ quản lý danh mục, sản phẩm, xử lý đơn hàng, tính giá.</li>
 *     <li>{@link #CUSTOMER}: Khách hàng mua sắm, đặt hàng và theo dõi đơn hàng của mình.</li>
 * </ul>
 */
public enum RoleCode {
    /**
     * Quản trị viên hệ thống.
     */
    ADMIN,

    /**
     * Quản lý nghiệp vụ bán hàng và đơn hàng.
     */
    MANAGER,

    /**
     * Khách hàng cá nhân.
     */
    CUSTOMER
}
