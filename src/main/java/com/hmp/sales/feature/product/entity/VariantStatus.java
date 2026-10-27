package com.hmp.sales.feature.product.entity;

/**
 * Enum trạng thái hoạt động của biến thể sản phẩm.
 * <ul>
 *   <li>{@code ACTIVE} — biến thể đang hoạt động, có thể đặt hàng.</li>
 *   <li>{@code INACTIVE} — biến thể bị vô hiệu hóa, không nhận đặt hàng mới.</li>
 * </ul>
 */
public enum VariantStatus {
    ACTIVE,
    INACTIVE
}
