package com.hmp.sales.feature.product.entity;

/**
 * Enum trạng thái hoạt động của sản phẩm.
 * <ul>
 *   <li>{@code ACTIVE} — sản phẩm đang hoạt động, có thể đặt hàng.</li>
 *   <li>{@code INACTIVE} — sản phẩm bị khóa/ngừng kinh doanh, không cho phép đặt hàng mới.</li>
 * </ul>
 */
public enum ProductStatus {
    ACTIVE,
    INACTIVE
}
