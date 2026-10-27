package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.product.dto.UnitRequest;
import com.hmp.sales.feature.product.dto.UnitResponse;
import com.hmp.sales.feature.product.service.UnitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller dành cho Admin quản lý danh mục Đơn vị tính (Unit).
 * <p>
 * Cung cấp API tìm kiếm/phân trang, tạo mới, chỉnh sửa tên,
 * khóa/mở khóa trạng thái và xóa đơn vị tính (chặn xóa nếu đã có bảng giá liên kết).
 */
@RestController
@RequestMapping("/api/v1/admin/units")
public class AdminUnitController {

    private final UnitService unitService;

    /**
     * Khởi tạo AdminUnitController với UnitService.
     *
     * @param unitService service quản lý đơn vị tính
     */
    public AdminUnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    /**
     * Tìm kiếm và phân trang danh sách đơn vị tính.
     *
     * @param keyword từ khóa tìm theo tên (tùy chọn)
     * @param status  lọc theo trạng thái (tùy chọn)
     * @param page    chỉ số trang (mặc định 0)
     * @param size    số lượng bản ghi mỗi trang (mặc định 20)
     * @return danh sách đơn vị tính có phân trang
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<UnitResponse>>> listUnits(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UnitResponse> result = unitService.searchUnits(keyword, status, page, size);
        return ResponseEntity.ok(ApiSuccessResponse.success(PageResponse.from(result)));
    }

    /**
     * Lấy thông tin đơn vị tính theo ID.
     *
     * @param unitId ID đơn vị tính cần lấy
     * @return chi tiết đơn vị tính
     */
    @GetMapping("/{unitId}")
    public ResponseEntity<ApiSuccessResponse<UnitResponse>> getUnitById(@PathVariable Long unitId) {
        UnitResponse response = unitService.getUnitById(unitId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Tạo mới đơn vị tính.
     *
     * @param request payload chứa tên đơn vị tính
     * @return thông tin đơn vị tính vừa tạo
     */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<UnitResponse>> createUnit(
            @Valid @RequestBody UnitRequest request) {
        UnitResponse response = unitService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo đơn vị tính thành công.", response));
    }

    /**
     * Cập nhật thông tin đơn vị tính.
     *
     * @param unitId  ID đơn vị tính cần cập nhật
     * @param request payload chứa thông tin mới
     * @return thông tin đơn vị tính sau cập nhật
     */
    @PatchMapping("/{unitId}")
    public ResponseEntity<ApiSuccessResponse<UnitResponse>> updateUnit(
            @PathVariable Long unitId,
            @Valid @RequestBody UnitRequest request) {
        UnitResponse response = unitService.updateUnit(unitId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật đơn vị tính thành công.", response));
    }

    /**
     * Khóa đơn vị tính (chuyển sang LOCKED).
     *
     * @param unitId ID đơn vị tính cần khóa
     * @return thông tin đơn vị tính sau khi khóa
     */
    @PostMapping("/{unitId}/lock")
    public ResponseEntity<ApiSuccessResponse<UnitResponse>> lockUnit(@PathVariable Long unitId) {
        UnitResponse response = unitService.lockUnit(unitId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa đơn vị tính thành công.", response));
    }

    /**
     * Mở khóa đơn vị tính (chuyển về ACTIVE).
     *
     * @param unitId ID đơn vị tính cần mở khóa
     * @return thông tin đơn vị tính sau khi mở khóa
     */
    @PostMapping("/{unitId}/unlock")
    public ResponseEntity<ApiSuccessResponse<UnitResponse>> unlockUnit(@PathVariable Long unitId) {
        UnitResponse response = unitService.unlockUnit(unitId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa đơn vị tính thành công.", response));
    }

    /**
     * Xóa mềm đơn vị tính (đánh dấu is_deleted = true).
     * <p>
     * Chặn xóa nếu đơn vị tính đang được sử dụng trong bất kỳ bảng giá biến thể nào.
     *
     * @param unitId ID đơn vị tính cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{unitId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteUnit(@PathVariable Long unitId) {
        unitService.deleteUnit(unitId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa đơn vị tính thành công.", null));
    }
}
