package com.hmp.sales.feature.product.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.repository.CategoryRepository;
import com.hmp.sales.feature.product.dto.*;
import com.hmp.sales.feature.product.entity.*;
import com.hmp.sales.feature.product.repository.*;
import com.hmp.sales.feature.product.service.ProductService;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.security.PermissionEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ quản lý sản phẩm tôn sóng và biến thể sản phẩm.
 * <p>
 * Điểm đáng chú ý:
 * <ul>
 *   <li>Kiểm soát hiển thị giá động qua {@link PermissionEvaluationService} với quyền {@code PRODUCT_PRICE_VIEW}.</li>
 *   <li>Không cho phép sortBy=price khi thiếu quyền (tránh rò rỉ giá gián tiếp qua thứ tự).</li>
 *   <li>Ràng buộc tối đa 1 ảnh chính và 10 ảnh phụ được kiểm tra nghiêm ngặt.</li>
 *   <li>Xóa mềm sản phẩm cascade sang variants và images; chỉ chặn khi có đơn hàng dở dang.</li>
 * </ul>
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private static final int MAX_TOTAL_IMAGES = 11;
    private static final int MAX_SECONDARY_IMAGES = 10;
    private static final String ALLOWED_SORT_FIELDS_WITHOUT_PRICE = "productName,createdAt";

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductColorRepository productColorRepository;
    private final ProductPriceRepository productPriceRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ColorRepository colorRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final PermissionEvaluationService permissionEvaluationService;

    /**
     * Khởi tạo ProductServiceImpl với tất cả dependency cần thiết.
     *
     * @param productRepository           repository quản lý sản phẩm
     * @param productImageRepository      repository quản lý ảnh sản phẩm
     * @param productVariantRepository    repository quản lý biến thể
     * @param productColorRepository      repository liên kết màu sắc-biến thể
     * @param productPriceRepository      repository quản lý bảng giá
     * @param categoryRepository          repository quản lý danh mục
     * @param brandRepository             repository quản lý thương hiệu
     * @param colorRepository             repository quản lý màu sắc
     * @param unitRepository              repository quản lý đơn vị tính
     * @param userRepository              repository quản lý người dùng
     * @param permissionEvaluationService service kiểm tra quyền động PRODUCT_PRICE_VIEW
     */
    public ProductServiceImpl(
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ProductVariantRepository productVariantRepository,
            ProductColorRepository productColorRepository,
            ProductPriceRepository productPriceRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            ColorRepository colorRepository,
            UnitRepository unitRepository,
            UserRepository userRepository,
            PermissionEvaluationService permissionEvaluationService) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productVariantRepository = productVariantRepository;
        this.productColorRepository = productColorRepository;
        this.productPriceRepository = productPriceRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.colorRepository = colorRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.permissionEvaluationService = permissionEvaluationService;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Bảo vệ rò rỉ giá gián tiếp: chặn sortBy=price khi caller thiếu quyền PRODUCT_PRICE_VIEW.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> searchProducts(ProductFilterParams params) {
        boolean priceVisible = permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW");

        // Chặn sắp xếp theo giá khi không có quyền (tránh rò rỉ gián tiếp)
        if ("price".equalsIgnoreCase(params.sortBy()) && !priceVisible) {
            throw new BusinessRuleException("Không thể sắp xếp theo giá khi chưa được cấp quyền xem giá sản phẩm.");
        }

        String sortField = resolveSortField(params.sortBy());
        Sort.Direction direction = "desc".equalsIgnoreCase(params.sortDirection())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(params.page(), params.size(), Sort.by(direction, sortField));

        return productRepository.searchProducts(
                params.productName(), params.categoryName(), params.status(), pageable)
                .map(p -> ProductSummaryResponse.fromEntity(p, priceVisible));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        boolean priceVisible = permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW");
        Product product = findActiveProduct(productId);
        return ProductDetailResponse.fromEntity(product, priceVisible);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductDetailResponse createProduct(Long categoryId, ProductCreateRequest request, Long managerId) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + categoryId));

        var manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + managerId));

        Brand brand = null;
        if (request.brandId() != null) {
            brand = brandRepository.findByIdAndIsDeletedFalse(request.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu ID: " + request.brandId()));
        }

        Product product = Product.builder()
                .productName(request.productName())
                .description(request.description())
                .category(category)
                .brand(brand)
                .createdBy(manager)
                .build();
        product = productRepository.save(product);

        // Khởi tạo các biến thể ban đầu nếu có
        if (request.variants() != null && !request.variants().isEmpty()) {
            for (ProductVariantRequest varReq : request.variants()) {
                createVariantInternal(product, varReq);
            }
        }

        log.info("[ProductService] Tạo sản phẩm '{}' (ID={}) trong danh mục ID={}", product.getProductName(), product.getId(), categoryId);
        // Reload để có full children
        product = findActiveProduct(product.getId());
        boolean priceVisible = permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW");
        return ProductDetailResponse.fromEntity(product, priceVisible);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = findActiveProduct(productId);

        if (request.productName() != null && !request.productName().isBlank()) {
            product.setProductName(request.productName().trim());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.brandId() != null) {
            Brand brand = brandRepository.findByIdAndIsDeletedFalse(request.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu ID: " + request.brandId()));
            product.setBrand(brand);
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findByIdAndIsDeletedFalse(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + request.categoryId()));
            product.setCategory(category);
        }

        product = productRepository.save(product);
        boolean priceVisible = permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW");
        return ProductDetailResponse.fromEntity(product, priceVisible);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductDetailResponse lockProduct(Long productId) {
        Product product = findActiveProduct(productId);
        product.setStatus(ProductStatus.INACTIVE);
        product = productRepository.save(product);
        boolean priceVisible = permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW");
        return ProductDetailResponse.fromEntity(product, priceVisible);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductDetailResponse unlockProduct(Long productId) {
        Product product = findActiveProduct(productId);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);
        boolean priceVisible = permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW");
        return ProductDetailResponse.fromEntity(product, priceVisible);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findActiveProduct(productId);
        if (productRepository.hasIncompleteOrders(productId)) {
            throw new BusinessRuleException(
                    "Không thể xóa sản phẩm vì đang có đơn hàng chưa hoàn thành chứa sản phẩm này.");
        }
        // Cascade soft-delete sang images và variants
        productImageRepository.softDeleteByProductId(productId);
        productVariantRepository.softDeleteByProductId(productId);
        product.setIsDeleted(true);
        productRepository.save(product);
        log.info("[ProductService] Xóa mềm sản phẩm ID={}", productId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductImageResponse uploadProductImage(Long productId, ProductImageUploadRequest request) {
        Product product = findActiveProduct(productId);

        long totalImages = productImageRepository.countByProductIdAndIsDeletedFalse(productId);
        if (totalImages >= MAX_TOTAL_IMAGES) {
            throw new BusinessRuleException("Sản phẩm đã đủ tối đa " + MAX_TOTAL_IMAGES + " ảnh.");
        }

        if (Boolean.TRUE.equals(request.isPrimary())) {
            long primaryCount = productImageRepository.countByProductIdAndIsPrimaryTrueAndIsDeletedFalse(productId);
            if (primaryCount >= 1) {
                throw new BusinessRuleException("Sản phẩm đã có ảnh đại diện. Vui lòng xóa ảnh đại diện cũ trước.");
            }
        } else {
            long secondaryCount = productImageRepository.countByProductIdAndIsPrimaryFalseAndIsDeletedFalse(productId);
            if (secondaryCount >= MAX_SECONDARY_IMAGES) {
                throw new BusinessRuleException("Sản phẩm đã đủ tối đa " + MAX_SECONDARY_IMAGES + " ảnh phụ.");
            }
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(request.imageUrl())
                .isPrimary(request.isPrimary())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();
        image = productImageRepository.save(image);
        return ProductImageResponse.fromEntity(image);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId) {
        findActiveProduct(productId); // validate product exists
        ProductImage image = productImageRepository.findByIdAndIsDeletedFalse(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh ID: " + imageId));
        image.setIsDeleted(true);
        productImageRepository.save(image);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductVariantResponse addVariant(Long productId, ProductVariantRequest request) {
        Product product = findActiveProduct(productId);
        ProductVariant variant = createVariantInternal(product, request);
        boolean priceVisible = permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW");
        return ProductVariantResponse.fromEntity(variant, priceVisible);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        findActiveProduct(productId);
        ProductVariant variant = productVariantRepository.findByIdAndIsDeletedFalse(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId));

        if (request.variantName() != null) variant.setVariantName(request.variantName());
        if (request.variantCode() != null) variant.setVariantCode(request.variantCode());
        if (request.typeValue() != null) variant.setTypeValue(request.typeValue());
        if (request.thickness() != null) variant.setThickness(request.thickness());

        variant = productVariantRepository.save(variant);

        // Cập nhật bảng giá nếu có
        if (request.prices() != null) {
            updatePricesForVariant(variant, request.prices());
        }

        // Cập nhật màu sắc nếu có
        if (request.colorIds() != null) {
            updateColorsForVariant(variant, request.colorIds());
        }

        boolean priceVisible = permissionEvaluationService.hasPermission("PRODUCT_PRICE_VIEW");
        // Reload variant
        variant = productVariantRepository.findByIdAndIsDeletedFalse(variantId).get();
        return ProductVariantResponse.fromEntity(variant, priceVisible);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        findActiveProduct(productId);
        ProductVariant variant = productVariantRepository.findByIdAndIsDeletedFalse(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId));
        productColorRepository.softDeleteByVariantId(variantId);
        productPriceRepository.softDeleteByVariantId(variantId);
        variant.setIsDeleted(true);
        productVariantRepository.save(variant);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductVariantResponse lockVariant(Long productId, Long variantId) {
        findActiveProduct(productId);
        ProductVariant variant = productVariantRepository.findByIdAndIsDeletedFalse(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId));
        variant.setStatus(VariantStatus.INACTIVE);
        variant = productVariantRepository.save(variant);
        boolean priceVisible = permissionEvaluationService.canViewProductPrice();
        return ProductVariantResponse.fromEntity(variant, priceVisible);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductVariantResponse unlockVariant(Long productId, Long variantId) {
        findActiveProduct(productId);
        ProductVariant variant = productVariantRepository.findByIdAndIsDeletedFalse(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId));
        variant.setStatus(VariantStatus.ACTIVE);
        variant = productVariantRepository.save(variant);
        boolean priceVisible = permissionEvaluationService.canViewProductPrice();
        return ProductVariantResponse.fromEntity(variant, priceVisible);
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Tìm sản phẩm theo ID và chưa bị xóa mềm.
     *
     * @param productId ID sản phẩm cần tìm
     * @return entity sản phẩm
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    private Product findActiveProduct(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + productId));
    }

    /**
     * Tạo biến thể sản phẩm nội bộ kèm màu sắc và bảng giá trong transaction.
     *
     * @param product entity sản phẩm cha
     * @param request payload biến thể
     * @return entity biến thể vừa tạo
     */
    private ProductVariant createVariantInternal(Product product, ProductVariantRequest request) {
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .variantCode(request.variantCode())
                .variantName(request.variantName())
                .typeValue(request.typeValue())
                .thickness(request.thickness())
                .build();
        variant = productVariantRepository.save(variant);

        // Gán màu sắc nếu có
        if (request.colorIds() != null && !request.colorIds().isEmpty()) {
            updateColorsForVariant(variant, request.colorIds());
        }

        // Gán bảng giá nếu có
        if (request.prices() != null && !request.prices().isEmpty()) {
            updatePricesForVariant(variant, request.prices());
        }

        return variant;
    }

    /**
     * Cập nhật màu sắc liên kết của biến thể (xóa cũ, thêm mới).
     *
     * @param variant  biến thể cần cập nhật màu
     * @param colorIds danh sách ID màu sắc mới
     */
    private void updateColorsForVariant(ProductVariant variant, List<Long> colorIds) {
        productColorRepository.softDeleteByVariantId(variant.getId());
        for (Long colorId : colorIds) {
            Color color = colorRepository.findByIdAndIsDeletedFalse(colorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu sắc ID: " + colorId));
            ProductColorId pcId = new ProductColorId(variant.getId(), colorId);
            // Tránh tạo trùng nếu đã soft-deleted trước đó
            ProductColor pc = ProductColor.builder()
                    .id(pcId).variant(variant).color(color).isDeleted(false).build();
            productColorRepository.save(pc);
        }
    }

    /**
     * Cập nhật bảng giá của biến thể (xóa cũ, thêm mới).
     *
     * @param variant entity biến thể
     * @param prices  map từ unitId → price (VND integer)
     */
    private void updatePricesForVariant(ProductVariant variant, Map<Long, Integer> prices) {
        productPriceRepository.softDeleteByVariantId(variant.getId());
        for (Map.Entry<Long, Integer> entry : prices.entrySet()) {
            Unit unit = unitRepository.findByIdAndIsDeletedFalse(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vị tính ID: " + entry.getKey()));
            ProductPriceId ppId = new ProductPriceId(variant.getId(), entry.getKey());
            ProductPrice pp = ProductPrice.builder()
                    .id(ppId).variant(variant).unit(unit).price(entry.getValue()).isDeleted(false).build();
            productPriceRepository.save(pp);
        }
    }

    /**
     * Phân giải trường sắp xếp từ request parameter sang tên field entity hợp lệ.
     *
     * @param sortBy trường sắp xếp từ request
     * @return tên field entity tương ứng
     */
    private String resolveSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return "createdAt";
        return switch (sortBy.toLowerCase()) {
            case "productname", "name" -> "productName";
            case "createdat" -> "createdAt";
            default -> "createdAt";
        };
    }
}
