package com.hmp.sales.feature.product.entity;

/**
 * Enum trạng thái hoạt động của màu sắc tôn sản xuất.
 * <ul>
 *   <li>{@code ACTIVE} — màu sắc đang hoạt động, có thể gán cho biến thể sản phẩm.</li>
 *   <li>{@code LOCKED} — màu sắc bị khóa, không gán cho biến thể sản phẩm mới.</li>
 * </ul>
 */
public enum ColorStatus {
    ACTIVE,
    LOCKED
}
