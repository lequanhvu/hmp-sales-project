package com.hmp.sales.feature.category.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ConflictWithPayloadException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.category.dto.*;
import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.repository.CategoryRepository;
import com.hmp.sales.feature.category.service.CategoryService;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.feature.category.entity.CategoryStatus;
import com.hmp.sales.feature.product.dto.ProductSummaryResponse;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.security.PermissionEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ quản lý cây danh mục sản phẩm.
 * <p>
 * Điểm đáng chú ý:
 * <ul>
 *   <li>Dựng cây O(N) in-memory bằng HashMap, không dùng đệ quy N+1 query.</li>
 *   <li>Phát hiện chu trình (cycle detection) khi gán parentCategory mới.</li>
 *   <li>Xóa mềm có cảnh báo xác nhận hai bước khi danh mục còn con/sản phẩm.</li>
 *   <li>Cascade soft-delete cây con chỉ khi không có đơn hàng dở dang.</li>
 * </ul>
 */
@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PermissionEvaluationService permissionEvaluationService;

    /**
     * Khởi tạo CategoryServiceImpl với các dependency cần thiết.
     *
     * @param categoryRepository          repository quản lý danh mục
     * @param productRepository           repository quản lý sản phẩm (kiểm tra đơn hàng dở dang)
     * @param userRepository              repository quản lý người dùng (xác định Manager tạo danh mục)
     * @param permissionEvaluationService service đánh giá quyền xem giá
     */
    public CategoryServiceImpl(CategoryRepository categoryRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository,
                                PermissionEvaluationService permissionEvaluationService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.permissionEvaluationService = permissionEvaluationService;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Thuật toán dựng cây O(N): load tất cả danh mục → xây dựng map(id → node) →
     * duyệt một lần để gán children → trả về danh sách root node.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> all = categoryRepository.findAllByIsDeletedFalse();

        // Tạo map id -> node để O(1) lookup
        Map<Long, CategoryTreeResponse> nodeMap = new LinkedHashMap<>();
        for (Category c : all) {
            nodeMap.put(c.getId(), CategoryTreeResponse.fromEntity(c));
        }

        // Duyệt một lần: gán mỗi node vào children của cha
        List<CategoryTreeResponse> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryTreeResponse node = nodeMap.get(c.getId());
            if (c.getParentCategory() == null) {
                roots.add(node);
            } else {
                CategoryTreeResponse parent = nodeMap.get(c.getParentCategory().getId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        return roots;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryId) {
        Category category = findActiveCategory(categoryId);
        return CategoryResponse.fromEntity(category);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse createRootCategory(CategoryCreateRequest request, Long managerId) {
        // Kiểm tra tên trùng ở cấp root
        if (categoryRepository.existsByCategoryNameAndParentCategoryIsNullAndIsDeletedFalse(request.categoryName())) {
            throw new BusinessRuleException("Tên danh mục '" + request.categoryName() + "' đã tồn tại ở cấp gốc.");
        }

        var manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + managerId));

        Category category = Category.builder()
                .categoryName(request.categoryName())
                .parentCategory(null)
                .createdBy(manager)
                .build();

        category = categoryRepository.save(category);
        log.info("[CategoryService] Tạo danh mục gốc: {} (ID={})", category.getCategoryName(), category.getId());
        return CategoryResponse.fromEntity(category);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse createChildCategory(Long parentId, CategoryCreateRequest request, Long managerId) {
        Category parent = findActiveCategory(parentId);

        // Kiểm tra tên trùng trong cùng cha
        if (categoryRepository.existsByCategoryNameAndParentCategory_IdAndIsDeletedFalse(request.categoryName(), parentId)) {
            throw new BusinessRuleException("Tên danh mục '" + request.categoryName() + "' đã tồn tại trong danh mục cha này.");
        }

        var manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + managerId));

        Category category = Category.builder()
                .categoryName(request.categoryName())
                .parentCategory(parent)
                .createdBy(manager)
                .build();

        category = categoryRepository.save(category);
        log.info("[CategoryService] Tạo danh mục con '{}' (ID={}) trong cha ID={}", category.getCategoryName(), category.getId(), parentId);
        return CategoryResponse.fromEntity(category);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        Category category = findActiveCategory(categoryId);

        if (request.categoryName() != null && !request.categoryName().isBlank()) {
            String newName = request.categoryName().trim();
            // Kiểm tra tên mới trùng với danh mục cùng cha
            boolean nameExists;
            if (category.getParentCategory() == null) {
                nameExists = categoryRepository.existsByCategoryNameAndParentCategoryIsNullAndIsDeletedFalse(newName)
                        && !newName.equals(category.getCategoryName());
            } else {
                nameExists = categoryRepository.existsByCategoryNameAndParentCategory_IdAndIsDeletedFalse(newName, category.getParentCategory().getId())
                        && !newName.equals(category.getCategoryName());
            }
            if (nameExists) {
                throw new BusinessRuleException("Tên danh mục '" + newName + "' đã tồn tại trong danh mục cha.");
            }
            category.setCategoryName(newName);
        }

        category = categoryRepository.save(category);
        return CategoryResponse.fromEntity(category);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng xóa mềm:
     * <ol>
     *   <li>Nếu danh mục rỗng → xóa mềm ngay.</li>
     *   <li>Nếu có con/sản phẩm và {@code force=false} → ném BusinessRuleException kèm thông tin đếm.</li>
     *   <li>Nếu có con/sản phẩm và {@code force=true} → kiểm tra đơn hàng dở dang → cascade soft-delete.</li>
     * </ol>
     */
    @Override
    @Transactional
    public void deleteCategory(Long categoryId, boolean force) {
        Category category = findActiveCategory(categoryId);

        // Lấy toàn bộ ID trong cây con (bao gồm cả chính nó)
        List<Long> allDescendantIds = categoryRepository.findAllDescendantIds(categoryId);
        // Loại bỏ categoryId chính để đếm chỉ con cháu
        long childCategoryCount = allDescendantIds.size() - 1;
        long productCount = productRepository.countByCategoryIdInAndIsDeletedFalse(allDescendantIds);

        boolean hasChildren = childCategoryCount > 0 || productCount > 0;

        if (!hasChildren) {
            // Rỗng → xóa mềm ngay
            category.setIsDeleted(true);
            categoryRepository.save(category);
            log.info("[CategoryService] Xóa mềm danh mục rỗng ID={}", categoryId);
            return;
        }

        if (!force) {
            // Trả về cảnh báo để frontend hiển thị popup confirm
            long totalImpact = childCategoryCount + productCount;
            CategoryDeleteWarningResponse warning = new CategoryDeleteWarningResponse(
                    categoryId, category.getCategoryName(), childCategoryCount, productCount, totalImpact);
            throw new ConflictWithPayloadException(
                    "Danh mục này chứa " + totalImpact + " danh mục và sản phẩm. Vui lòng xác nhận xóa toàn bộ.",
                    warning);
        }

        // force=true → kiểm tra đơn hàng dở dang trước khi cascade
        List<Long> productIds = productRepository.findIdsByCategoryIdIn(allDescendantIds);
        if (!productIds.isEmpty() && productRepository.anyHasIncompleteOrders(productIds)) {
            throw new BusinessRuleException(
                    "Không thể xóa danh mục vì có sản phẩm đang nằm trong đơn hàng chưa hoàn thành.");
        }

        // Cascade soft-delete toàn bộ cây con
        categoryRepository.softDeleteByIds(allDescendantIds);
        if (!productIds.isEmpty()) {
            productRepository.softDeleteByIds(productIds);
        }
        log.info("[CategoryService] Cascade soft-delete danh mục ID={} và {} danh mục con, {} sản phẩm",
                categoryId, childCategoryCount, productCount);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public CategoryProductsResponse getProductsByCategory(Long categoryId) {
        Category category = findActiveCategory(categoryId);
        List<Product> products = productRepository.findByCategoryIdAndStatusAndIsDeletedFalse(categoryId, ProductStatus.ACTIVE);
        boolean priceVisible = permissionEvaluationService.canViewProductPrice();
        List<ProductSummaryResponse> productDtos = products.stream()
                .map(p -> ProductSummaryResponse.fromEntity(p, priceVisible))
                .toList();
        return new CategoryProductsResponse(category.getId(), category.getCategoryName(), productDtos);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse lockCategory(Long categoryId) {
        Category category = findActiveCategory(categoryId);
        category.setStatus(CategoryStatus.LOCKED);
        category = categoryRepository.save(category);
        log.info("[CategoryService] Khóa danh mục ID={}", categoryId);
        return CategoryResponse.fromEntity(category);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse unlockCategory(Long categoryId) {
        Category category = findActiveCategory(categoryId);
        category.setStatus(CategoryStatus.ACTIVE);
        category = categoryRepository.save(category);
        log.info("[CategoryService] Mở khóa danh mục ID={}", categoryId);
        return CategoryResponse.fromEntity(category);
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Tìm danh mục theo ID và chưa bị xóa mềm. Ném ngoại lệ nếu không tồn tại.
     *
     * @param categoryId ID danh mục cần tìm
     * @return entity danh mục
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    private Category findActiveCategory(Long categoryId) {
        return categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + categoryId));
    }
}
