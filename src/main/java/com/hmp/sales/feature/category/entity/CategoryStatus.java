package com.hmp.sales.feature.category.entity;

/**
 * Enum trạng thái hoạt động của danh mục sản phẩm.
 * <ul>
 *   <li>{@code ACTIVE} — danh mục đang hiển thị và cho phép tạo sản phẩm/danh mục con bên dưới.</li>
 *   <li>{@code LOCKED} — danh mục bị khóa, không nhận thêm sản phẩm/danh mục con nhưng vẫn lưu lịch sử.</li>
 * </ul>
 */
public enum CategoryStatus {
    ACTIVE,
    LOCKED
}
