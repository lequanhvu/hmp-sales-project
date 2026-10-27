package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import com.hmp.sales.feature.product.service.ColorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller dành cho Admin quản lý danh mục Màu sắc sản xuất (Color).
 * <p>
 * Cung cấp API tìm kiếm/phân trang, tạo mới, chỉnh sửa tên,
 * khóa/mở khóa trạng thái và xóa màu sắc (chặn xóa nếu đã có biến thể liên kết).
 */
@RestController
@RequestMapping("/api/v1/admin/colors")
public class AdminColorController {

    private final ColorService colorService;

    /**
     * Khởi tạo AdminColorController với ColorService.
     *
     * @param colorService service quản lý màu sắc
     */
    public AdminColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    /**
     * Tìm kiếm và phân trang danh sách màu sắc.
     *
     * @param keyword từ khóa tìm theo tên (tùy chọn)
     * @param status  lọc theo trạng thái (tùy chọn)
     * @param page    chỉ số trang (mặc định 0)
     * @param size    số lượng bản ghi mỗi trang (mặc định 20)
     * @return danh sách màu sắc có phân trang
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<ColorResponse>>> listColors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ColorResponse> result = colorService.searchColors(keyword, status, page, size);
        return ResponseEntity.ok(ApiSuccessResponse.success(PageResponse.from(result)));
    }

    /**
     * Lấy thông tin màu sắc theo ID.
     *
     * @param colorId ID màu sắc cần lấy
     * @return chi tiết màu sắc
     */
    @GetMapping("/{colorId}")
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> getColorById(@PathVariable Long colorId) {
        ColorResponse response = colorService.getColorById(colorId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Tạo mới màu sắc.
     *
     * @param request payload chứa tên màu sắc
     * @return thông tin màu sắc vừa tạo
     */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> createColor(
            @Valid @RequestBody ColorRequest request) {
        ColorResponse response = colorService.createColor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo màu sắc thành công.", response));
    }

    /**
     * Cập nhật thông tin màu sắc.
     *
     * @param colorId ID màu sắc cần cập nhật
     * @param request payload chứa thông tin mới
     * @return thông tin màu sắc sau cập nhật
     */
    @PatchMapping("/{colorId}")
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> updateColor(
            @PathVariable Long colorId,
            @Valid @RequestBody ColorRequest request) {
        ColorResponse response = colorService.updateColor(colorId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật màu sắc thành công.", response));
    }

    /**
     * Khóa màu sắc (chuyển sang LOCKED).
     *
     * @param colorId ID màu sắc cần khóa
     * @return thông tin màu sắc sau khi khóa
     */
    @PostMapping("/{colorId}/lock")
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> lockColor(@PathVariable Long colorId) {
        ColorResponse response = colorService.lockColor(colorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa màu sắc thành công.", response));
    }

    /**
     * Mở khóa màu sắc (chuyển về ACTIVE).
     *
     * @param colorId ID màu sắc cần mở khóa
     * @return thông tin màu sắc sau khi mở khóa
     */
    @PostMapping("/{colorId}/unlock")
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> unlockColor(@PathVariable Long colorId) {
        ColorResponse response = colorService.unlockColor(colorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa màu sắc thành công.", response));
    }

    /**
     * Xóa mềm màu sắc (đánh dấu is_deleted = true).
     * <p>
     * Chặn xóa nếu màu sắc đang được liên kết trong bất kỳ biến thể sản phẩm nào.
     *
     * @param colorId ID màu sắc cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{colorId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteColor(@PathVariable Long colorId) {
        colorService.deleteColor(colorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa màu sắc thành công.", null));
    }
}
