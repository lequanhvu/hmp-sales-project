package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.product.dto.BrandRequest;
import com.hmp.sales.feature.product.dto.BrandResponse;
import com.hmp.sales.feature.product.service.BrandService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller dành cho Admin quản lý danh mục Thương hiệu (Brand).
 * <p>
 * Cho phép xem danh sách phân trang, tạo mới, chỉnh sửa tên,
 * khóa/mở khóa trạng thái và xóa thương hiệu (có kiểm tra ràng buộc liên kết sản phẩm).
 */
@RestController
@RequestMapping("/api/v1/admin/brands")
public class AdminBrandController {

    private final BrandService brandService;

    /**
     * Khởi tạo AdminBrandController với BrandService.
     *
     * @param brandService service quản lý thương hiệu
     */
    public AdminBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    /**
     * Tìm kiếm và phân trang danh sách thương hiệu.
     *
     * @param keyword từ khóa tìm theo tên (tùy chọn)
     * @param status  lọc theo trạng thái (tùy chọn)
     * @param page    chỉ số trang (mặc định 0)
     * @param size    số lượng bản ghi mỗi trang (mặc định 20)
     * @return danh sách thương hiệu có phân trang
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<BrandResponse>>> listBrands(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BrandResponse> result = brandService.searchBrands(keyword, status, page, size);
        return ResponseEntity.ok(ApiSuccessResponse.success(PageResponse.from(result)));
    }

    /**
     * Lấy thông tin thương hiệu theo ID.
     *
     * @param brandId ID thương hiệu cần lấy
     * @return chi tiết thương hiệu
     */
    @GetMapping("/{brandId}")
    public ResponseEntity<ApiSuccessResponse<BrandResponse>> getBrandById(@PathVariable Long brandId) {
        BrandResponse response = brandService.getBrandById(brandId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Tạo mới thương hiệu sản phẩm.
     *
     * @param request payload chứa tên thương hiệu
     * @return thông tin thương hiệu vừa tạo
     */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<BrandResponse>> createBrand(
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo thương hiệu thành công.", response));
    }

    /**
     * Cập nhật thông tin thương hiệu.
     *
     * @param brandId ID thương hiệu cần cập nhật
     * @param request payload chứa thông tin mới
     * @return thông tin thương hiệu sau cập nhật
     */
    @PatchMapping("/{brandId}")
    public ResponseEntity<ApiSuccessResponse<BrandResponse>> updateBrand(
            @PathVariable Long brandId,
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.updateBrand(brandId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật thương hiệu thành công.", response));
    }

    /**
     * Khóa thương hiệu (chuyển sang LOCKED).
     *
     * @param brandId ID thương hiệu cần khóa
     * @return thông tin thương hiệu sau khi khóa
     */
    @PostMapping("/{brandId}/lock")
    public ResponseEntity<ApiSuccessResponse<BrandResponse>> lockBrand(@PathVariable Long brandId) {
        BrandResponse response = brandService.lockBrand(brandId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa thương hiệu thành công.", response));
    }

    /**
     * Mở khóa thương hiệu (chuyển về ACTIVE).
     *
     * @param brandId ID thương hiệu cần mở khóa
     * @return thông tin thương hiệu sau khi mở khóa
     */
    @PostMapping("/{brandId}/unlock")
    public ResponseEntity<ApiSuccessResponse<BrandResponse>> unlockBrand(@PathVariable Long brandId) {
        BrandResponse response = brandService.unlockBrand(brandId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa thương hiệu thành công.", response));
    }

    /**
     * Xóa mềm thương hiệu (đánh dấu is_deleted = true).
     * <p>
     * Hệ thống chặn xóa nếu thương hiệu đang có sản phẩm liên kết.
     *
     * @param brandId ID thương hiệu cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{brandId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteBrand(@PathVariable Long brandId) {
        brandService.deleteBrand(brandId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa thương hiệu thành công.", null));
    }
}
