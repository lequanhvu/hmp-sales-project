package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.feature.product.dto.*;
import com.hmp.sales.feature.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller dành cho Quản lý (Manager) điều hành sản phẩm tôn sóng.
 * <p>
 * Cung cấp các thao tác:
 * <ul>
 *   <li>Tạo sản phẩm trực tiếp dưới một danh mục theo mô hình file-system.</li>
 *   <li>Cập nhật thông tin cơ bản sản phẩm, khóa/mở khóa và xóa mềm.</li>
 *   <li>Quản lý danh sách hình ảnh (tối đa 1 ảnh chính và 10 ảnh phụ).</li>
 *   <li>Quản lý danh sách biến thể sản phẩm, màu sắc và bảng giá theo đơn vị tính.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/manager")
public class ManagerProductController {

    private final ProductService productService;

    /**
     * Khởi tạo ManagerProductController với ProductService.
     *
     * @param productService service quản lý sản phẩm
     */
    public ManagerProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Tạo mới sản phẩm trong một thư mục/danh mục cụ thể.
     *
     * @param categoryId ID danh mục chứa sản phẩm
     * @param request    payload thông tin sản phẩm và các biến thể ban đầu
     * @param jwt        JWT xác thực của Manager
     * @return chi tiết sản phẩm vừa tạo
     */
    @PostMapping("/categories/{categoryId}/products")
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> createProduct(
            @PathVariable Long categoryId,
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long managerId = SecurityUtils.extractUserId(jwt);
        ProductDetailResponse response = productService.createProduct(categoryId, request, managerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo sản phẩm thành công.", response));
    }

    /**
     * Cập nhật thông tin cơ bản của sản phẩm.
     *
     * @param productId ID sản phẩm cần cập nhật
     * @param request   payload cập nhật
     * @return chi tiết sản phẩm sau khi cập nhật
     */
    @PatchMapping("/products/{productId}")
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductDetailResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật sản phẩm thành công.", response));
    }

    /**
     * Khóa sản phẩm (chuyển sang trạng thái INACTIVE, không cho đặt hàng mới).
     *
     * @param productId ID sản phẩm cần khóa
     * @return chi tiết sản phẩm sau khi khóa
     */
    @PostMapping("/products/{productId}/lock")
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> lockProduct(
            @PathVariable Long productId) {
        ProductDetailResponse response = productService.lockProduct(productId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa sản phẩm thành công.", response));
    }

    /**
     * Mở khóa sản phẩm (chuyển về trạng thái ACTIVE).
     *
     * @param productId ID sản phẩm cần mở khóa
     * @return chi tiết sản phẩm sau khi mở khóa
     */
    @PostMapping("/products/{productId}/unlock")
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> unlockProduct(
            @PathVariable Long productId) {
        ProductDetailResponse response = productService.unlockProduct(productId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa sản phẩm thành công.", response));
    }

    /**
     * Xóa mềm sản phẩm ({@code isDeleted = true}).
     * <p>
     * Cho phép xóa kể cả khi sản phẩm đã có trong các đơn hàng lịch sử (COMPLETED, CANCELLED).
     * Chặn xóa (409 Conflict) khi sản phẩm đang nằm trong đơn hàng chưa hoàn thành (PENDING, CONFIRMED, PROCESSING, SHIPPING).
     *
     * @param productId ID sản phẩm cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa sản phẩm thành công.", null));
    }

    /**
     * Thêm ảnh mới cho sản phẩm.
     * <p>
     * Ràng buộc: Mỗi sản phẩm chỉ có tối đa 1 ảnh chính và 10 ảnh phụ (tối đa 11 ảnh).
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param request   payload ảnh
     * @return thông tin ảnh vừa tạo
     */
    @PostMapping("/products/{productId}/images")
    public ResponseEntity<ApiSuccessResponse<ProductImageResponse>> uploadProductImage(
            @PathVariable Long productId,
            @Valid @RequestBody ProductImageUploadRequest request) {
        ProductImageResponse response = productService.uploadProductImage(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Thêm ảnh sản phẩm thành công.", response));
    }

    /**
     * Xóa mềm một ảnh của sản phẩm.
     *
     * @param productId ID sản phẩm
     * @param imageId   ID ảnh cần xóa
     * @return thông báo xóa ảnh thành công
     */
    @DeleteMapping("/products/{productId}/images/{imageId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa ảnh sản phẩm thành công.", null));
    }

    /**
     * Thêm biến thể mới cho sản phẩm (kèm danh sách màu và bảng giá theo đơn vị tính).
     *
     * @param productId ID sản phẩm
     * @param request   payload thông tin biến thể
     * @return chi tiết biến thể vừa tạo
     */
    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ApiSuccessResponse<ProductVariantResponse>> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = productService.addVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Thêm biến thể thành công.", response));
    }

    /**
     * Cập nhật thông tin biến thể sản phẩm.
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể cần cập nhật
     * @param request   payload cập nhật
     * @return chi tiết biến thể sau khi cập nhật
     */
    @PatchMapping("/products/{productId}/variants/{variantId}")
    public ResponseEntity<ApiSuccessResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = productService.updateVariant(productId, variantId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật biến thể thành công.", response));
    }

    /**
     * Khóa biến thể sản phẩm (chuyển sang LOCKED).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể cần khóa
     * @return chi tiết biến thể sau khi khóa
     */
    @PostMapping("/products/{productId}/variants/{variantId}/lock")
    public ResponseEntity<ApiSuccessResponse<ProductVariantResponse>> lockVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        ProductVariantResponse response = productService.lockVariant(productId, variantId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa biến thể thành công.", response));
    }

    /**
     * Mở khóa biến thể sản phẩm (chuyển về ACTIVE).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể cần mở khóa
     * @return chi tiết biến thể sau khi mở khóa
     */
    @PostMapping("/products/{productId}/variants/{variantId}/unlock")
    public ResponseEntity<ApiSuccessResponse<ProductVariantResponse>> unlockVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        ProductVariantResponse response = productService.unlockVariant(productId, variantId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa biến thể thành công.", response));
    }

    /**
     * Xóa mềm biến thể sản phẩm (cascade xóa màu sắc và bảng giá liên kết).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/products/{productId}/variants/{variantId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        productService.deleteVariant(productId, variantId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa biến thể thành công.", null));
    }
}
