package com.hmp.sales.common.exception;

/**
 * Ngoại lệ ném ra khi có yêu cầu chuyển đổi trạng thái đơn hàng không hợp lệ theo ma trận trạng thái
 * (ví dụ: chuyển từ PENDING thẳng sang COMPLETED mà chưa qua CONFIRMED/PROCESSING).
 */
public class InvalidOrderStateException extends BusinessRuleException {

    /**
     * Khởi tạo ngoại lệ với thông điệp tùy chỉnh.
     *
     * @param message Thông điệp mô tả chi tiết lỗi.
     */
    public InvalidOrderStateException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với trạng thái hiện tại và trạng thái đích không hợp lệ.
     * Tự động sinh thông điệp bằng tiếng Việt theo chuẩn nghiệp vụ.
     *
     * @param currentState Trạng thái hiện tại của đơn hàng.
     * @param targetState  Trạng thái đích muốn chuyển tới.
     */
    public InvalidOrderStateException(String currentState, String targetState) {
        super(String.format("Không thể chuyển trạng thái đơn hàng từ '%s' sang '%s'.", currentState, targetState));
    }
}
