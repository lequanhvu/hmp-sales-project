package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.product.dto.ProductDetailResponse;
import com.hmp.sales.feature.product.dto.ProductFilterParams;
import com.hmp.sales.feature.product.dto.ProductSummaryResponse;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller công khai phục vụ tra cứu sản phẩm tôn sóng và phụ kiện.
 * <p>
 * Hỗ trợ tìm kiếm, phân trang và xem chi tiết sản phẩm.
 * Hiển thị giá sản phẩm được điều khiển động qua quyền {@code PRODUCT_PRICE_VIEW}:
 * chỉ khi người dùng được cấp quyền xem giá thì trường giá mới có dữ liệu.
 */
@RestController
@RequestMapping("/api/v1/products")
public class PublicProductController {

    private final ProductService productService;

    /**
     * Khởi tạo PublicProductController với ProductService.
     *
     * @param productService service nghiệp vụ quản lý sản phẩm
     */
    public PublicProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Tìm kiếm và lọc danh sách sản phẩm với phân trang.
     * <p>
     * Nếu người gọi không có quyền {@code PRODUCT_PRICE_VIEW}, cờ {@code priceVisible}
     * trong response sẽ là {@code false} và chặn sắp xếp theo giá ({@code sortBy=price}).
     *
     * @param productName   từ khóa tìm theo tên sản phẩm (tùy chọn)
     * @param categoryName  từ khóa tìm theo tên danh mục (tùy chọn)
     * @param status        lọc theo trạng thái (tùy chọn, mặc định ACTIVE cho khách vãng lai)
     * @param sortBy        trường sắp xếp (mặc định createdAt)
     * @param sortDirection hướng sắp xếp (asc hoặc desc, mặc định asc)
     * @param page          chỉ số trang (mặc định 0)
     * @param size          kích thước trang (mặc định 20)
     * @return danh sách sản phẩm tóm tắt có phân trang
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<ProductSummaryResponse>>> searchProducts(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProductFilterParams params = new ProductFilterParams(
                productName, categoryName, status, sortBy, sortDirection, page, size);
        Page<ProductSummaryResponse> result = productService.searchProducts(params);
        return ResponseEntity.ok(ApiSuccessResponse.success(PageResponse.from(result)));
    }

    /**
     * Lấy thông tin chi tiết của một sản phẩm theo ID (kèm danh sách ảnh, biến thể và bảng giá).
     *
     * @param productId ID sản phẩm cần tra cứu
     * @return chi tiết sản phẩm
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> getProductDetail(
            @PathVariable Long productId) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }
}
