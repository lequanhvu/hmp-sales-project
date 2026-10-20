package com.hmp.sales.feature.user.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.user.dto.CreateManagerRequest;
import com.hmp.sales.feature.user.dto.UserSearchRequest;
import com.hmp.sales.feature.user.dto.UserStatusResponse;
import com.hmp.sales.feature.user.dto.UserSummaryResponse;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller quản lý tài khoản người dùng dành cho Admin.
 * <p>
 * Tất cả endpoint yêu cầu xác thực JWT và quyền tương ứng.
 * Controller chỉ xử lý HTTP concerns; business logic nằm trong
 * {@link UserService}.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class UserController {

    private final UserService userService;

    /**
     * Khởi tạo AdminUserController với dependency AdminUserService.
     *
     * @param userService service xử lý nghiệp vụ quản trị người dùng
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Tìm kiếm và lọc danh sách người dùng với phân trang.
     * <p>
     * Yêu cầu quyền {@code USER_LOCK} (quyền xem danh sách user).
     *
     * @param name   lọc theo tên (tùy chọn)
     * @param phone  lọc theo số điện thoại (tùy chọn)
     * @param email  lọc theo email (tùy chọn)
     * @param status lọc theo trạng thái (tùy chọn)
     * @param page   trang hiện tại (mặc định 0)
     * @param size   kích thước trang (mặc định 20)
     * @return danh sách người dùng có phân trang theo cấu trúc chuẩn
     */
    @GetMapping("/users")
    public ResponseEntity<ApiSuccessResponse<PageResponse<UserSummaryResponse>>> listUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        UserSearchRequest request = new UserSearchRequest(name, phone, email, status, page, size);
        Page<UserSummaryResponse> result = userService.searchUsers(request);
        return ResponseEntity.ok(ApiSuccessResponse.success(PageResponse.from(result)));
    }

    /**
     * Khóa tài khoản người dùng (chuyển sang LOCKED).
     * <p>
     * Yêu cầu quyền {@code USER_LOCK}.
     *
     * @param userId ID người dùng cần khóa
     * @param jwt    JWT của Admin đang thực hiện thao tác
     * @return trạng thái tài khoản sau khi khóa
     */
    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<ApiSuccessResponse<UserStatusResponse>> lockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {
        Long actorId = extractActorId(jwt);
        UserStatusResponse response = userService.lockUser(userId, actorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa tài khoản thành công.", response));
    }

    /**
     * Mở khóa tài khoản người dùng (chuyển về ACTIVE).
     * <p>
     * Yêu cầu quyền {@code USER_UNLOCK}.
     *
     * @param userId ID người dùng cần mở khóa
     * @param jwt    JWT của Admin đang thực hiện thao tác
     * @return trạng thái tài khoản sau khi mở khóa
     */
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<ApiSuccessResponse<UserStatusResponse>> unlockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {
        Long actorId = extractActorId(jwt);
        UserStatusResponse response = userService.unlockUser(userId, actorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa tài khoản thành công.", response));
    }

    /**
     * Xóa mềm tài khoản người dùng (đặt is_deleted = true).
     * <p>
     * Dữ liệu lịch sử vẫn được giữ nguyên. Token bị revoke.
     * Yêu cầu quyền {@code MANAGER_CREATE} (Admin-only action).
     *
     * @param userId ID người dùng cần xóa mềm
     * @param jwt    JWT của Admin đang thực hiện thao tác
     * @return xác nhận xóa thành công
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {
        Long actorId = extractActorId(jwt);
        userService.softDeleteUser(userId, actorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa tài khoản thành công.", null));
    }

    /**
     * Tạo tài khoản Manager mới (provisioned bởi Admin).
     * <p>
     * Manager sẽ đăng nhập bằng tài khoản Google đã đăng ký.
     * Yêu cầu quyền {@code MANAGER_CREATE}.
     *
     * @param request thông tin Manager mới (email, fullName)
     * @param jwt     JWT của Admin đang thực hiện thao tác
     * @return thông tin tóm tắt tài khoản Manager vừa tạo
     */
    @PostMapping("/managers")
    public ResponseEntity<ApiSuccessResponse<UserSummaryResponse>> createManager(
            @RequestBody @Valid CreateManagerRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long actorId = extractActorId(jwt);
        UserSummaryResponse response = userService.createManager(request, actorId);
        return ResponseEntity.status(201)
                .body(ApiSuccessResponse.created("Tạo tài khoản Manager thành công.", response));
    }

    /**
     * Trích xuất ID người dùng từ JWT subject claim.
     *
     * @param jwt JWT của người dùng đang xác thực
     * @return ID người dùng
     */
    private Long extractActorId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
