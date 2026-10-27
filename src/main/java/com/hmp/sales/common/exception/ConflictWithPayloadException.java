package com.hmp.sales.common.exception;

/**
 * Ngoại lệ ném ra khi xóa danh mục còn chứa danh mục con hoặc sản phẩm,
 * cần người dùng xác nhận qua popup cảnh báo trước khi thực hiện xóa cascade.
 * <p>
 * Được xử lý bởi {@code GlobalExceptionHandler} trả về HTTP 409 Conflict kèm
 * payload chi tiết số lượng bị ảnh hưởng để frontend hiển thị thông báo.
 *
 * @param <T> kiểu payload chứa thông tin chi tiết để frontend hiển thị cảnh báo
 */
public class ConflictWithPayloadException extends RuntimeException {

    private final Object payload;

    /**
     * Khởi tạo ngoại lệ xung đột kèm payload chi tiết.
     *
     * @param message thông điệp mô tả xung đột
     * @param payload dữ liệu chi tiết (số lượng danh mục/sản phẩm bị ảnh hưởng) cho frontend
     */
    public ConflictWithPayloadException(String message, Object payload) {
        super(message);
        this.payload = payload;
    }

    /**
     * Lấy payload chi tiết kèm theo ngoại lệ.
     *
     * @return payload chứa thông tin chi tiết để hiển thị popup cảnh báo
     */
    public Object getPayload() {
        return payload;
    }
}
