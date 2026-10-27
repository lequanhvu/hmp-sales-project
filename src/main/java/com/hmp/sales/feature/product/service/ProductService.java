package com.hmp.sales.feature.product.service;

import com.hmp.sales.feature.product.dto.*;
import org.springframework.data.domain.Page;

/**
 * Giao diện nghiệp vụ quản lý sản phẩm tôn sóng và biến thể sản phẩm (Product).
 * <p>
 * Bao gồm:
 * <ul>
 *   <li>Tìm kiếm/lọc sản phẩm công khai với kiểm soát hiển thị giá ({@code PRODUCT_PRICE_VIEW}).</li>
 *   <li>CRUD sản phẩm, quản lý ảnh và biến thể bởi Manager.</li>
 *   <li>Khóa/mở khóa sản phẩm.</li>
 *   <li>Xóa mềm với kiểm tra đơn hàng dở dang.</li>
 * </ul>
 */
public interface ProductService {

    /**
     * Tìm kiếm và lọc sản phẩm với phân trang. Giá sản phẩm chỉ hiển thị khi caller có quyền
     * {@code PRODUCT_PRICE_VIEW}. Không cho phép {@code sortBy=price} khi thiếu quyền.
     *
     * @param params tham số lọc, sắp xếp và phân trang
     * @return trang sản phẩm tóm tắt
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu cố sắp xếp theo giá mà thiếu quyền
     */
    Page<ProductSummaryResponse> searchProducts(ProductFilterParams params);

    /**
     * Lấy thông tin chi tiết sản phẩm theo ID, bao gồm ảnh và biến thể.
     * Giá được che giấu nếu caller thiếu quyền {@code PRODUCT_PRICE_VIEW}.
     *
     * @param productId ID sản phẩm cần lấy
     * @return DTO chi tiết sản phẩm
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    ProductDetailResponse getProductDetail(Long productId);

    /**
     * Tạo mới sản phẩm trong một danh mục, bao gồm khởi tạo biến thể ban đầu.
     * Thực hiện trong một transaction atomic.
     *
     * @param categoryId ID danh mục chứa sản phẩm
     * @param request    payload tạo sản phẩm và các biến thể
     * @param managerId  ID Manager tạo sản phẩm
     * @return DTO chi tiết sản phẩm vừa tạo
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu danh mục hoặc thương hiệu không tồn tại
     */
    ProductDetailResponse createProduct(Long categoryId, ProductCreateRequest request, Long managerId);

    /**
     * Cập nhật thông tin cơ bản của sản phẩm (tên, mô tả, thương hiệu, danh mục).
     *
     * @param productId ID sản phẩm cần cập nhật
     * @param request   payload chứa thông tin cập nhật
     * @return DTO chi tiết sản phẩm sau khi cập nhật
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request);

    /**
     * Khóa sản phẩm (chuyển sang trạng thái INACTIVE, không cho đặt hàng mới).
     *
     * @param productId ID sản phẩm cần khóa
     * @return DTO chi tiết sản phẩm sau khi khóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    ProductDetailResponse lockProduct(Long productId);

    /**
     * Mở khóa sản phẩm (chuyển về trạng thái ACTIVE).
     *
     * @param productId ID sản phẩm cần mở khóa
     * @return DTO chi tiết sản phẩm sau khi mở khóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    ProductDetailResponse unlockProduct(Long productId);

    /**
     * Xóa mềm sản phẩm ({@code isDeleted = true}).
     * Cho phép xóa kể cả khi có đơn hàng lịch sử đã hoàn thành/hủy.
     * Chỉ chặn xóa (409 Conflict) khi sản phẩm đang trong đơn hàng chưa hoàn thành.
     *
     * @param productId ID sản phẩm cần xóa mềm
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu có đơn hàng chưa hoàn thành
     */
    void deleteProduct(Long productId);

    /**
     * Thêm ảnh cho sản phẩm. Kiểm tra ràng buộc tối đa 1 ảnh chính và 10 ảnh phụ.
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param request   payload URL ảnh và loại ảnh
     * @return DTO chi tiết ảnh vừa thêm
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy sản phẩm
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu vượt quá giới hạn ảnh
     */
    ProductImageResponse uploadProductImage(Long productId, ProductImageUploadRequest request);

    /**
     * Xóa mềm ảnh sản phẩm theo ID.
     *
     * @param productId ID sản phẩm chứa ảnh
     * @param imageId   ID ảnh cần xóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy sản phẩm hoặc ảnh
     */
    void deleteProductImage(Long productId, Long imageId);

    /**
     * Thêm biến thể mới cho sản phẩm, bao gồm màu sắc và bảng giá theo đơn vị tính.
     *
     * @param productId ID sản phẩm cần thêm biến thể
     * @param request   payload thông tin biến thể
     * @return DTO chi tiết biến thể vừa tạo
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy sản phẩm
     */
    ProductVariantResponse addVariant(Long productId, ProductVariantRequest request);

    /**
     * Cập nhật thông tin biến thể sản phẩm (tên, loại sóng, độ dày, màu, giá).
     *
     * @param productId ID sản phẩm chứa biến thể
     * @param variantId ID biến thể cần cập nhật
     * @param request   payload thông tin cập nhật
     * @return DTO chi tiết biến thể sau khi cập nhật
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request);

    /**
     * Khóa biến thể sản phẩm (chuyển sang LOCKED).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể
     * @return DTO biến thể sau khi khóa
     */
    ProductVariantResponse lockVariant(Long productId, Long variantId);

    /**
     * Mở khóa biến thể sản phẩm (chuyển sang ACTIVE).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể
     * @return DTO biến thể sau khi mở khóa
     */
    ProductVariantResponse unlockVariant(Long productId, Long variantId);

    /**
     * Xóa mềm biến thể sản phẩm kèm màu sắc và bảng giá liên quan.
     *
     * @param productId ID sản phẩm chứa biến thể
     * @param variantId ID biến thể cần xóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    void deleteVariant(Long productId, Long variantId);
}
