package com.hmp.sales.feature.category.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ConflictWithPayloadException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.category.dto.CategoryCreateRequest;
import com.hmp.sales.feature.category.dto.CategoryProductsResponse;
import com.hmp.sales.feature.category.dto.CategoryResponse;
import com.hmp.sales.feature.category.dto.CategoryTreeResponse;
import com.hmp.sales.feature.category.dto.CategoryUpdateRequest;
import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.entity.CategoryStatus;
import com.hmp.sales.feature.category.repository.CategoryRepository;
import com.hmp.sales.feature.category.service.impl.CategoryServiceImpl;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.repository.ProductRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link CategoryServiceImpl}.
 * Kiểm thử xây dựng cây phân cấp, tạo, sửa, khóa/mở khóa, và cơ chế xác nhận hai bước khi xóa mềm.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Unit Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionEvaluationService permissionEvaluationService;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User manager;
    private Category rootCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        manager = User.builder().id(1L).fullName("Manager Test").build();

        rootCategory = Category.builder()
                .id(10L)
                .categoryName("Tôn lợp mái")
                .status(CategoryStatus.ACTIVE)
                .parentCategory(null)
                .createdBy(manager)
                .isDeleted(false)
                .build();

        childCategory = Category.builder()
                .id(20L)
                .categoryName("Tôn sóng vuông")
                .status(CategoryStatus.ACTIVE)
                .parentCategory(rootCategory)
                .createdBy(manager)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("getCategoryTree - Dựng đúng cây phân cấp n-cấp")
    void getCategoryTree_buildsHierarchicalTree() {
        when(categoryRepository.findAllByIsDeletedFalse()).thenReturn(List.of(rootCategory, childCategory));

        List<CategoryTreeResponse> tree = categoryService.getCategoryTree();

        assertThat(tree).hasSize(1);
        CategoryTreeResponse rootNode = tree.get(0);
        assertThat(rootNode.id()).isEqualTo(10L);
        assertThat(rootNode.categoryName()).isEqualTo("Tôn lợp mái");
        assertThat(rootNode.children()).hasSize(1);
        assertThat(rootNode.children().get(0).id()).isEqualTo(20L);
        assertThat(rootNode.children().get(0).categoryName()).isEqualTo("Tôn sóng vuông");
    }

    @Test
    @DisplayName("createRootCategory - Tạo danh mục gốc thành công")
    void createRootCategory_success() {
        CategoryCreateRequest request = new CategoryCreateRequest("Phụ kiện tôn");
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(categoryRepository.existsByCategoryNameAndParentCategoryIsNullAndIsDeletedFalse("Phụ kiện tôn")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(30L);
            return c;
        });

        CategoryResponse response = categoryService.createRootCategory(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.categoryName()).isEqualTo("Phụ kiện tôn");
        assertThat(response.parentId()).isNull();
    }

    @Test
    @DisplayName("createRootCategory - Trùng tên ở cấp root ném BusinessRuleException")
    void createRootCategory_duplicateName_throwsException() {
        CategoryCreateRequest request = new CategoryCreateRequest("Tôn lợp mái");
        when(categoryRepository.existsByCategoryNameAndParentCategoryIsNullAndIsDeletedFalse("Tôn lợp mái")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createRootCategory(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("createChildCategory - Tạo danh mục con thành công")
    void createChildCategory_success() {
        CategoryCreateRequest request = new CategoryCreateRequest("Tôn sóng ngói");
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(categoryRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByCategoryNameAndParentCategory_IdAndIsDeletedFalse("Tôn sóng ngói", 10L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(21L);
            return c;
        });

        CategoryResponse response = categoryService.createChildCategory(10L, request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.categoryName()).isEqualTo("Tôn sóng ngói");
        assertThat(response.parentId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("deleteCategory - Danh mục rỗng xóa mềm ngay lập tức")
    void deleteCategory_empty_softDeletesImmediately() {
        when(categoryRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.findAllDescendantIds(20L)).thenReturn(List.of(20L));
        when(productRepository.countByCategoryIdInAndIsDeletedFalse(List.of(20L))).thenReturn(0L);

        categoryService.deleteCategory(20L, false);

        assertThat(childCategory.getIsDeleted()).isTrue();
        verify(categoryRepository).save(childCategory);
    }

    @Test
    @DisplayName("deleteCategory - Còn con/sản phẩm và force=false ném ConflictWithPayloadException (409)")
    void deleteCategory_hasChildren_forceFalse_throwsConflictWithPayloadException() {
        when(categoryRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findAllDescendantIds(10L)).thenReturn(List.of(10L, 20L));
        when(productRepository.countByCategoryIdInAndIsDeletedFalse(List.of(10L, 20L))).thenReturn(5L);

        assertThatThrownBy(() -> categoryService.deleteCategory(10L, false))
                .isInstanceOf(ConflictWithPayloadException.class)
                .hasMessageContaining("Vui lòng xác nhận xóa toàn bộ");
    }

    @Test
    @DisplayName("deleteCategory - Còn con/sản phẩm, force=true nhưng có đơn dở dang ném BusinessRuleException")
    void deleteCategory_hasChildren_forceTrue_withIncompleteOrders_throwsBusinessRuleException() {
        when(categoryRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findAllDescendantIds(10L)).thenReturn(List.of(10L, 20L));
        when(productRepository.countByCategoryIdInAndIsDeletedFalse(List.of(10L, 20L))).thenReturn(5L);
        when(productRepository.findIdsByCategoryIdIn(List.of(10L, 20L))).thenReturn(List.of(100L, 101L));
        when(productRepository.anyHasIncompleteOrders(List.of(100L, 101L))).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(10L, true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đang nằm trong đơn hàng chưa hoàn thành");
    }

    @Test
    @DisplayName("deleteCategory - force=true và không có đơn dở dang thực hiện cascade soft-delete")
    void deleteCategory_hasChildren_forceTrue_noIncompleteOrders_cascadesSoftDelete() {
        when(categoryRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findAllDescendantIds(10L)).thenReturn(List.of(10L, 20L));
        when(productRepository.countByCategoryIdInAndIsDeletedFalse(List.of(10L, 20L))).thenReturn(5L);
        when(productRepository.findIdsByCategoryIdIn(List.of(10L, 20L))).thenReturn(List.of(100L));
        when(productRepository.anyHasIncompleteOrders(List.of(100L))).thenReturn(false);

        categoryService.deleteCategory(10L, true);

        verify(categoryRepository).softDeleteByIds(List.of(10L, 20L));
        verify(productRepository).softDeleteByIds(List.of(100L));
    }

    @Test
    @DisplayName("lockCategory và unlockCategory thay đổi trạng thái chính xác")
    void lockAndUnlockCategory_updatesStatus() {
        when(categoryRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse locked = categoryService.lockCategory(10L);
        assertThat(locked.status()).isEqualTo(CategoryStatus.LOCKED);

        CategoryResponse unlocked = categoryService.unlockCategory(10L);
        assertThat(unlocked.status()).isEqualTo(CategoryStatus.ACTIVE);
    }

    @Test
    @DisplayName("getAllProductsByCategory trả về cả sản phẩm ACTIVE và INACTIVE cho quản lý")
    void getAllProductsByCategory_returnsActiveAndInactiveProducts() {
        when(categoryRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(rootCategory));

        Product activeProduct = Product.builder()
                .id(101L)
                .productName("Tôn 5 sóng")
                .status(ProductStatus.ACTIVE)
                .category(rootCategory)
                .images(List.of())
                .isDeleted(false)
                .build();

        Product inactiveProduct = Product.builder()
                .id(102L)
                .productName("Tôn cũ ngừng bán")
                .status(ProductStatus.INACTIVE)
                .category(rootCategory)
                .images(List.of())
                .isDeleted(false)
                .build();

        when(productRepository.findByCategoryIdAndIsDeletedFalse(10L))
                .thenReturn(List.of(activeProduct, inactiveProduct));
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(true);

        CategoryProductsResponse response = categoryService.getAllProductsByCategory(10L);

        assertThat(response).isNotNull();
        assertThat(response.categoryId()).isEqualTo(10L);
        assertThat(response.categoryName()).isEqualTo("Tôn lợp mái");
        assertThat(response.products()).hasSize(2);
        assertThat(response.products()).extracting("status")
                .containsExactly(ProductStatus.ACTIVE, ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("getProductsByCategory chỉ trả về sản phẩm ACTIVE cho public")
    void getProductsByCategory_returnsOnlyActiveProducts() {
        when(categoryRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(rootCategory));

        Product activeProduct = Product.builder()
                .id(101L)
                .productName("Tôn 5 sóng")
                .status(ProductStatus.ACTIVE)
                .category(rootCategory)
                .images(List.of())
                .isDeleted(false)
                .build();

        when(productRepository.findByCategoryIdAndStatusAndIsDeletedFalse(10L, ProductStatus.ACTIVE))
                .thenReturn(List.of(activeProduct));
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(false);

        CategoryProductsResponse response = categoryService.getProductsByCategory(10L);

        assertThat(response).isNotNull();
        assertThat(response.categoryId()).isEqualTo(10L);
        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(response.products().get(0).priceVisible()).isFalse();
    }
}
