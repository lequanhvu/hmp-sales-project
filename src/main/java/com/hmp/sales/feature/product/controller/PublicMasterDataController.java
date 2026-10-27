package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.feature.product.dto.BrandResponse;
import com.hmp.sales.feature.product.dto.ColorResponse;
import com.hmp.sales.feature.product.dto.UnitResponse;
import com.hmp.sales.feature.product.service.BrandService;
import com.hmp.sales.feature.product.service.ColorService;
import com.hmp.sales.feature.product.service.UnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller công khai cung cấp danh mục cấu hình cơ sở (Master Data).
 * <p>
 * Cung cấp các endpoint GET công khai trả về danh sách Thương hiệu (Brand),
 * Đơn vị tính (Unit), và Màu sắc (Color) đang ở trạng thái ACTIVE phục vụ
 * các thành phần dropdown/filter trên giao diện người dùng.
 */
@RestController
@RequestMapping("/api/v1")
public class PublicMasterDataController {

    private final BrandService brandService;
    private final UnitService unitService;
    private final ColorService colorService;

    /**
     * Khởi tạo PublicMasterDataController với các service master data.
     *
     * @param brandService service quản lý thương hiệu
     * @param unitService  service quản lý đơn vị tính
     * @param colorService service quản lý màu sắc
     */
    public PublicMasterDataController(BrandService brandService,
                                      UnitService unitService,
                                      ColorService colorService) {
        this.brandService = brandService;
        this.unitService = unitService;
        this.colorService = colorService;
    }

    /**
     * Lấy danh sách thương hiệu đang hoạt động (ACTIVE).
     *
     * @return danh sách thương hiệu hoạt động
     */
    @GetMapping("/brands")
    public ResponseEntity<ApiSuccessResponse<List<BrandResponse>>> getActiveBrands() {
        List<BrandResponse> brands = brandService.getActiveBrands();
        return ResponseEntity.ok(ApiSuccessResponse.success(brands));
    }

    /**
     * Lấy danh sách đơn vị tính đang hoạt động (ACTIVE).
     *
     * @return danh sách đơn vị tính hoạt động
     */
    @GetMapping("/units")
    public ResponseEntity<ApiSuccessResponse<List<UnitResponse>>> getActiveUnits() {
        List<UnitResponse> units = unitService.getActiveUnits();
        return ResponseEntity.ok(ApiSuccessResponse.success(units));
    }

    /**
     * Lấy danh sách màu sắc đang hoạt động (ACTIVE).
     *
     * @return danh sách màu sắc hoạt động
     */
    @GetMapping("/colors")
    public ResponseEntity<ApiSuccessResponse<List<ColorResponse>>> getActiveColors() {
        List<ColorResponse> colors = colorService.getActiveColors();
        return ResponseEntity.ok(ApiSuccessResponse.success(colors));
    }
}
