package com.hmp.sales.feature.systemlog;

/**
 * Service ghi nhật ký nghiệp vụ quan trọng vào bảng {@code system_logs}.
 * <p>
 * Được sử dụng để audit trail các hành động quản trị nhạy cảm như:
 * thay đổi quyền hạn vai trò, khóa/mở khóa tài khoản, tạo tài khoản Manager,
 * thay đổi cài đặt hệ thống.
 *
 * <p><strong>Lưu ý Phase 5</strong>: Implementation hiện tại là stub rỗng.
 * Entity {@code SystemLog} và persistence sẽ được hoàn thiện ở Phase 5.
 * Mọi service layer có thể gọi các phương thức này ngay bây giờ mà không bị block.
 */
public interface SystemLogService {

    /**
     * Ghi nhật ký một hành động quản trị đơn giản.
     *
     * @param actorId   ID người dùng thực hiện hành động (null nếu là hệ thống)
     * @param action    mã hành động (ví dụ: "USER_LOCKED", "ROLE_PERMISSION_UPDATED")
     * @param entityType loại thực thể bị ảnh hưởng (ví dụ: "USER", "ROLE")
     * @param entityId  ID thực thể bị ảnh hưởng
     * @param description mô tả bằng tiếng Việt về hành động
     */
    void log(Long actorId, String action, String entityType, Long entityId, String description);

    /**
     * Ghi nhật ký hành động có trước/sau state để audit trail đầy đủ.
     *
     * @param actorId   ID người dùng thực hiện hành động
     * @param action    mã hành động
     * @param entityType loại thực thể bị ảnh hưởng
     * @param entityId  ID thực thể bị ảnh hưởng
     * @param description mô tả hành động
     * @param oldValue  giá trị trước khi thay đổi (JSON text hoặc mô tả ngắn)
     * @param newValue  giá trị sau khi thay đổi (JSON text hoặc mô tả ngắn)
     */
    void logWithChange(Long actorId, String action, String entityType, Long entityId,
                       String description, String oldValue, String newValue);
}
