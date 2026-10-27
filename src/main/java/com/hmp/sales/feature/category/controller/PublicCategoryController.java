package com.hmp.sales.feature.category.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.feature.category.dto.CategoryProductsResponse;
import com.hmp.sales.feature.category.dto.CategoryTreeResponse;
import com.hmp.sales.feature.category.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller công khai phục vụ tra cứu danh mục sản phẩm (Category).
 * <p>
 * Cho phép khách vãng lai và khách hàng xem cấu trúc cây danh mục
 * và danh sách sản phẩm thuộc danh mục mà không cần đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class PublicCategoryController {

    private final CategoryService categoryService;

    /**
     * Khởi tạo PublicCategoryController với dependency CategoryService.
     *
     * @param categoryService service quản lý danh mục sản phẩm
     */
    public PublicCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Lấy toàn bộ cây danh mục sản phẩm theo cấu trúc phân cấp n-cấp.
     * <p>
     * Công khai cho mọi đối tượng người dùng.
     *
     * @return danh sách các danh mục cấp gốc, mỗi danh mục chứa danh sách con lồng nhau
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiSuccessResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> tree = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiSuccessResponse.success(tree));
    }

    /**
     * Lấy danh sách sản phẩm đang hoạt động thuộc một danh mục cụ thể.
     * <p>
     * Giá sản phẩm sẽ được ẩn nếu người gọi chưa đăng nhập hoặc không có quyền xem giá.
     *
     * @param categoryId ID danh mục cần tra cứu
     * @return danh sách sản phẩm thuộc danh mục
     */
    @GetMapping("/{categoryId}/products")
    public ResponseEntity<ApiSuccessResponse<CategoryProductsResponse>> getProductsByCategory(
            @PathVariable Long categoryId) {
        CategoryProductsResponse response = categoryService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }
}
