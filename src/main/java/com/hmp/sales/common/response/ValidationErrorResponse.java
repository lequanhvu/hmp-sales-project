package com.hmp.sales.common.response;

/**
 * Record biểu diễn chi tiết lỗi validation cho từng trường dữ liệu cụ thể.
 *
 * @param field   Tên trường dữ liệu gặp lỗi vi phạm (ví dụ: "phone", "quantity").
 * @param message Thông điệp mô tả chi tiết lỗi vi phạm.
 */
public record ValidationErrorResponse(
    String field,
    String message
) {}
