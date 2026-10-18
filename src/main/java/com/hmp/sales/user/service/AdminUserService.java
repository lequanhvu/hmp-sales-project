package com.hmp.sales.user.service;

import com.hmp.sales.user.dto.CreateManagerRequest;
import com.hmp.sales.user.dto.UserSearchRequest;
import com.hmp.sales.user.dto.UserStatusResponse;
import com.hmp.sales.user.dto.UserSummaryResponse;
import org.springframework.data.domain.Page;

/**
 * Service quản lý tài khoản người dùng dành cho Admin.
 * <p>
 * Cung cấp các thao tác: tìm kiếm người dùng, khóa/mở khóa tài khoản,
 * xóa mềm tài khoản và tạo tài khoản Manager mới.
 * Mọi thao tác nhạy cảm đều được ghi vào {@code SystemLog}.
 */
public interface AdminUserService {

    /**
     * Tìm kiếm và lọc danh sách người dùng có phân trang.
     *
     * @param request tiêu chí tìm kiếm (name, phone, email, status)
     * @return trang kết quả chứa danh sách {@link UserSummaryResponse}
     */
    Page<UserSummaryResponse> searchUsers(UserSearchRequest request);

    /**
     * Khóa tài khoản người dùng (chuyển sang trạng thái {@code LOCKED}).
     * <p>
     * Điều kiện: tài khoản phải đang ở trạng thái {@code ACTIVE}.
     * Admin không thể tự khóa chính mình.
     *
     * @param userId       ID người dùng cần khóa
     * @param actorId      ID Admin đang thực hiện thao tác
     * @return trạng thái mới của tài khoản sau khi khóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy user
     * @throws com.hmp.sales.common.exception.BusinessRuleException     nếu vi phạm nghiệp vụ
     */
    UserStatusResponse lockUser(Long userId, Long actorId);

    /**
     * Mở khóa tài khoản người dùng (chuyển sang trạng thái {@code ACTIVE}).
     * <p>
     * Điều kiện: tài khoản phải đang ở trạng thái {@code LOCKED}.
     *
     * @param userId  ID người dùng cần mở khóa
     * @param actorId ID Admin đang thực hiện thao tác
     * @return trạng thái mới của tài khoản sau khi mở khóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy user
     * @throws com.hmp.sales.common.exception.BusinessRuleException     nếu tài khoản không bị khóa
     */
    UserStatusResponse unlockUser(Long userId, Long actorId);

    /**
     * Xóa mềm tài khoản người dùng (đặt cờ {@code isDeleted = true}).
     * <p>
     * Dữ liệu lịch sử (đơn hàng, audit) vẫn được giữ nguyên.
     * Access token (Redis) và refresh token (PostgreSQL) của user sẽ bị revoke.
     *
     * @param userId  ID người dùng cần xóa mềm
     * @param actorId ID Admin đang thực hiện thao tác
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy user
     * @throws com.hmp.sales.common.exception.BusinessRuleException     nếu Admin tự xóa chính mình
     */
    void softDeleteUser(Long userId, Long actorId);

    /**
     * Tạo tài khoản Manager mới được provisioned bởi Admin.
     * <p>
     * Manager sẽ đăng nhập qua Google bằng email đã đăng ký.
     * Không có mật khẩu được tạo.
     *
     * @param request thông tin Manager mới (email, fullName)
     * @param actorId ID Admin đang thực hiện thao tác
     * @return thông tin tóm tắt tài khoản Manager vừa được tạo
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu email đã tồn tại
     */
    UserSummaryResponse createManager(CreateManagerRequest request, Long actorId);
}
