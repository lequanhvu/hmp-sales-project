package com.hmp.sales.feature.product.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.repository.CategoryRepository;
import com.hmp.sales.feature.product.dto.*;
import com.hmp.sales.feature.product.entity.*;
import com.hmp.sales.feature.product.repository.*;
import com.hmp.sales.feature.product.service.impl.ProductServiceImpl;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.security.PermissionEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link ProductServiceImpl}.
 * Kiểm thử quy tắc hiển thị giá bảo vệ qua PRODUCT_PRICE_VIEW,
 * giới hạn 11 ảnh, kiểm tra đơn hàng dở dang khi xóa mềm.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductColorRepository productColorRepository;

    @Mock
    private ProductPriceRepository productPriceRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionEvaluationService permissionEvaluationService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private Category category;
    private Brand brand;
    private User manager;

    @BeforeEach
    void setUp() {
        manager = User.builder().id(1L).fullName("Manager Test").build();

        category = Category.builder()
                .id(10L)
                .categoryName("Tôn sóng vuông")
                .isDeleted(false)
                .build();

        brand = Brand.builder()
                .id(5L)
                .brandName("Tôn Hoa Sen")
                .isDeleted(false)
                .build();

        product = Product.builder()
                .id(100L)
                .productName("Tôn Hoa Sen 11 sóng")
                .status(ProductStatus.ACTIVE)
                .category(category)
                .brand(brand)
                .createdBy(manager)
                .isDeleted(false)
                .images(new ArrayList<>())
                .variants(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("searchProducts - Có quyền PRODUCT_PRICE_VIEW -> priceVisible = true")
    void searchProducts_withPermission_priceVisibleIsTrue() {
        when(permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW")).thenReturn(true);
        when(productRepository.searchProducts(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        ProductFilterParams params = new ProductFilterParams(null, null, null, "createdAt", "asc", 0, 10);
        Page<ProductSummaryResponse> result = productService.searchProducts(params);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).priceVisible()).isTrue();
    }

    @Test
    @DisplayName("searchProducts - Không có quyền PRODUCT_PRICE_VIEW -> priceVisible = false")
    void searchProducts_withoutPermission_priceVisibleIsFalse() {
        when(permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW")).thenReturn(false);
        when(productRepository.searchProducts(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        ProductFilterParams params = new ProductFilterParams(null, null, null, "createdAt", "asc", 0, 10);
        Page<ProductSummaryResponse> result = productService.searchProducts(params);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).priceVisible()).isFalse();
    }

    @Test
    @DisplayName("searchProducts - Thiếu quyền mà sortBy=price -> ném BusinessRuleException (chặn rò rỉ giá)")
    void searchProducts_sortByPrice_withoutPermission_throwsException() {
        when(permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW")).thenReturn(false);

        ProductFilterParams params = new ProductFilterParams(null, null, null, "price", "asc", 0, 10);

        assertThatThrownBy(() -> productService.searchProducts(params))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể sắp xếp theo giá khi chưa được cấp quyền xem giá");
    }

    @Test
    @DisplayName("getProductDetail - Thiếu quyền xem giá thì prices trong variant bị null")
    void getProductDetail_withoutPermission_hidesPrices() {
        ProductVariant variant = ProductVariant.builder()
                .id(200L)
                .product(product)
                .variantCode("VAR-01")
                .variantName("Biến thể 1")
                .status(VariantStatus.ACTIVE)
                .isDeleted(false)
                .colors(new ArrayList<>())
                .prices(new ArrayList<>())
                .build();
        product.getVariants().add(variant);

        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW")).thenReturn(false);

        ProductDetailResponse detail = productService.getProductDetail(100L);

        assertThat(detail.priceVisible()).isFalse();
        assertThat(detail.variants().get(0).prices()).isNull();
    }

    @Test
    @DisplayName("uploadProductImage - Vượt quá 11 ảnh ném BusinessRuleException")
    void uploadProductImage_exceeds11Images_throwsException() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(productImageRepository.countByProductIdAndIsDeletedFalse(100L)).thenReturn(11L);

        ProductImageUploadRequest req = new ProductImageUploadRequest("https://img.com/extra.png", false, 12);

        assertThatThrownBy(() -> productService.uploadProductImage(100L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã đủ tối đa 11 ảnh");
    }

    @Test
    @DisplayName("uploadProductImage - Thêm ảnh chính thứ 2 khi đã có ảnh chính ném BusinessRuleException")
    void uploadProductImage_duplicatePrimary_throwsException() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(productImageRepository.countByProductIdAndIsPrimaryTrueAndIsDeletedFalse(100L)).thenReturn(1L);

        ProductImageUploadRequest req = new ProductImageUploadRequest("https://img.com/primary2.png", true, 0);

        assertThatThrownBy(() -> productService.uploadProductImage(100L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã có ảnh đại diện");
    }

    @Test
    @DisplayName("deleteProduct - Đang có đơn hàng dở dang ném BusinessRuleException (409)")
    void deleteProduct_incompleteOrder_throwsException() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(productRepository.hasIncompleteOrders(100L)).thenReturn(true);

        assertThatThrownBy(() -> productService.deleteProduct(100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đơn hàng chưa hoàn thành");
    }

    @Test
    @DisplayName("deleteProduct - Không có đơn dở dang thực hiện xóa mềm thành công")
    void deleteProduct_noIncompleteOrder_softDeletes() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(productRepository.hasIncompleteOrders(100L)).thenReturn(false);

        productService.deleteProduct(100L);

        assertThat(product.getIsDeleted()).isTrue();
        verify(productRepository).save(product);
        verify(productVariantRepository).softDeleteByProductId(100L);
        verify(productImageRepository).softDeleteByProductId(100L);
    }

    @Test
    @DisplayName("lockProduct và unlockProduct - Cập nhật trạng thái sản phẩm")
    void lockAndUnlockProduct_updatesStatus() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW")).thenReturn(true);

        ProductDetailResponse locked = productService.lockProduct(100L);
        assertThat(locked.status()).isEqualTo(ProductStatus.INACTIVE);

        ProductDetailResponse unlocked = productService.unlockProduct(100L);
        assertThat(unlocked.status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("uploadProductImages - Tải lên danh sách nhiều ảnh phụ thành công")
    void uploadProductImages_success() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(productImageRepository.countByProductIdAndIsPrimaryFalseAndIsDeletedFalse(100L)).thenReturn(2L);
        when(productImageRepository.countByProductIdAndIsDeletedFalse(100L)).thenReturn(3L);

        List<ProductImageUploadRequest> requests = List.of(
                new ProductImageUploadRequest("https://img.com/p1.png", false, 1),
                new ProductImageUploadRequest("https://img.com/p2.png", false, 2)
        );

        when(productImageRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<ProductImage> list = inv.getArgument(0);
            long id = 500L;
            for (ProductImage pi : list) {
                pi.setId(id++);
            }
            return list;
        });

        List<ProductImageResponse> responses = productService.uploadProductImages(100L, requests);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).imageUrl()).isEqualTo("https://img.com/p1.png");
        assertThat(responses.get(1).imageUrl()).isEqualTo("https://img.com/p2.png");
    }

    @Test
    @DisplayName("uploadProductImages - Danh sách vượt quá 10 ảnh ném BusinessRuleException")
    void uploadProductImages_exceeds10ListSize_throwsException() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));

        List<ProductImageUploadRequest> requests = new java.util.ArrayList<>();
        for (int i = 0; i < 11; i++) {
            requests.add(new ProductImageUploadRequest("https://img.com/" + i + ".png", false, i));
        }

        assertThatThrownBy(() -> productService.uploadProductImages(100L, requests))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vượt quá 10 ảnh");
    }

    @Test
    @DisplayName("uploadProductImages - Có nhiều hơn 1 ảnh chính ném BusinessRuleException")
    void uploadProductImages_multiplePrimary_throwsException() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));

        List<ProductImageUploadRequest> requests = List.of(
                new ProductImageUploadRequest("https://img.com/p1.png", true, 1),
                new ProductImageUploadRequest("https://img.com/p2.png", true, 2)
        );

        assertThatThrownBy(() -> productService.uploadProductImages(100L, requests))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nhiều hơn 1 ảnh đại diện");
    }

    @Test
    @DisplayName("deleteProductImages - Xóa danh sách ảnh thành công")
    void deleteProductImages_success() {
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));

        ProductImage img1 = ProductImage.builder().id(501L).product(product).isDeleted(false).build();
        ProductImage img2 = ProductImage.builder().id(502L).product(product).isDeleted(false).build();

        when(productImageRepository.findByProductIdAndIdInAndIsDeletedFalse(100L, List.of(501L, 502L)))
                .thenReturn(List.of(img1, img2));

        productService.deleteProductImages(100L, List.of(501L, 502L));

        verify(productImageRepository).softDeleteByProductIdAndIds(100L, List.of(501L, 502L));
    }

    @Test
    @DisplayName("deleteProductImages - Danh sách rỗng ném BusinessRuleException")
    void deleteProductImages_emptyList_throwsException() {
        assertThatThrownBy(() -> productService.deleteProductImages(100L, List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không được để trống");
    }
}
