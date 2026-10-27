package com.hmp.sales.feature.product.entity;

/**
 * Enum trạng thái hoạt động của đơn vị tính bán hàng.
 * <ul>
 *   <li>{@code ACTIVE} — đơn vị tính đang hoạt động, có thể sử dụng cho bảng giá sản phẩm.</li>
 *   <li>{@code LOCKED} — đơn vị tính bị khóa, không dùng cho bảng giá mới.</li>
 * </ul>
 */
public enum UnitStatus {
    ACTIVE,
    LOCKED
}
