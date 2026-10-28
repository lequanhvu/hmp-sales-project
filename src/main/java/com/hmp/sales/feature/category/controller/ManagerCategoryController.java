package com.hmp.sales.feature.category.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.feature.category.dto.CategoryCreateRequest;
import com.hmp.sales.feature.category.dto.CategoryProductsResponse;
import com.hmp.sales.feature.category.dto.CategoryResponse;
import com.hmp.sales.feature.category.dto.CategoryUpdateRequest;
import com.hmp.sales.feature.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller dành cho Quản lý (Manager) thao tác với cây danh mục sản phẩm.
 * <p>
 * Hỗ trợ tạo danh mục gốc, tạo danh mục con dưới thư mục hiện tại,
 * cập nhật tên, khóa/mở khóa và xóa mềm có xác nhận hai bước.
 */
@RestController
@RequestMapping("/api/v1/manager/categories")
public class ManagerCategoryController {

    private final CategoryService categoryService;

    /**
     * Khởi tạo ManagerCategoryController với CategoryService.
     *
     * @param categoryService service quản lý danh mục sản phẩm
     */
    public ManagerCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Tạo danh mục gốc mới (cấp cao nhất trong cây thư mục).
     *
     * @param request payload chứa thông tin danh mục cần tạo
     * @param jwt     JWT của người dùng đang đăng nhập (Manager)
     * @return thông tin danh mục vừa tạo
     */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<CategoryResponse>> createRootCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long managerId = SecurityUtils.extractUserId(jwt);
        CategoryResponse response = categoryService.createRootCategory(request, managerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo danh mục gốc thành công.", response));
    }

    /**
     * Tạo danh mục con dưới một danh mục cha xác định.
     *
     * @param parentId ID danh mục cha
     * @param request  payload chứa thông tin danh mục con
     * @param jwt      JWT của người dùng đang đăng nhập (Manager)
     * @return thông tin danh mục con vừa tạo
     */
    @PostMapping("/{parentId}/children")
    public ResponseEntity<ApiSuccessResponse<CategoryResponse>> createChildCategory(
            @PathVariable Long parentId,
            @Valid @RequestBody CategoryCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long managerId = SecurityUtils.extractUserId(jwt);
        CategoryResponse response = categoryService.createChildCategory(parentId, request, managerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo danh mục con thành công.", response));
    }

    /**
     * Cập nhật thông tin danh mục (tên danh mục).
     *
     * @param categoryId ID danh mục cần cập nhật
     * @param request    payload cập nhật
     * @return thông tin danh mục sau khi cập nhật
     */
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiSuccessResponse<CategoryResponse>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật danh mục thành công.", response));
    }

    /**
     * Khóa danh mục (chuyển sang trạng thái LOCKED).
     *
     * @param categoryId ID danh mục cần khóa
     * @return thông tin danh mục sau khi khóa
     */
    @PostMapping("/{categoryId}/lock")
    public ResponseEntity<ApiSuccessResponse<CategoryResponse>> lockCategory(
            @PathVariable Long categoryId) {
        CategoryResponse response = categoryService.lockCategory(categoryId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa danh mục thành công.", response));
    }

    /**
     * Mở khóa danh mục (chuyển về trạng thái ACTIVE).
     *
     * @param categoryId ID danh mục cần mở khóa
     * @return thông tin danh mục sau khi mở khóa
     */
    @PostMapping("/{categoryId}/unlock")
    public ResponseEntity<ApiSuccessResponse<CategoryResponse>> unlockCategory(
            @PathVariable Long categoryId) {
        CategoryResponse response = categoryService.unlockCategory(categoryId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa danh mục thành công.", response));
    }

    /**
     * Lấy danh sách toàn bộ sản phẩm (bao gồm cả ACTIVE và INACTIVE) thuộc một danh mục.
     * Dành riêng cho Quản lý (Manager) và Quản trị viên (Admin) phục vụ công tác điều hành.
     *
     * @param categoryId ID danh mục cần lấy danh sách sản phẩm
     * @return danh sách sản phẩm (cả active và inactive) trong danh mục
     */
    @GetMapping("/{categoryId}/products")
    public ResponseEntity<ApiSuccessResponse<CategoryProductsResponse>> getProductsByCategory(
            @PathVariable Long categoryId) {
        CategoryProductsResponse response = categoryService.getAllProductsByCategory(categoryId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Xóa mềm danh mục sản phẩm.
     * <p>
     * Nếu danh mục còn chứa danh mục con hoặc sản phẩm:
     * <ul>
     *   <li>Nếu {@code force = false}: Ném lỗi 409 kèm thông tin đếm để frontend hiển thị modal confirm.</li>
     *   <li>Nếu {@code force = true}: Cascade soft-delete toàn bộ cây con sau khi kiểm tra không có đơn dở dang.</li>
     * </ul>
     *
     * @param categoryId ID danh mục cần xóa
     * @param force      cờ xác nhận người dùng đã đồng ý trên popup xác nhận
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "false") boolean force) {
        categoryService.deleteCategory(categoryId, force);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa danh mục thành công.", null));
    }
}
