package com.hmp.sales.feature.product.entity;

/**
 * Enum trạng thái hoạt động của thương hiệu sản phẩm.
 * <ul>
 *   <li>{@code ACTIVE} — thương hiệu đang hoạt động, có thể gán cho sản phẩm mới.</li>
 *   <li>{@code LOCKED} — thương hiệu bị khóa, không gán cho sản phẩm mới nhưng vẫn hiển thị trên sản phẩm cũ.</li>
 * </ul>
 */
public enum BrandStatus {
    ACTIVE,
    LOCKED
}
