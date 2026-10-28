package com.hmp.sales.feature.category.service;

import com.hmp.sales.feature.category.dto.*;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý cây danh mục sản phẩm (Category Tree).
 * <p>
 * Định nghĩa các hành vi: dựng cây phân cấp, tạo/cập nhật/xóa danh mục,
 * kiểm tra chu trình trong cây và xóa mềm cascade theo xác nhận của người dùng.
 */
public interface CategoryService {

    /**
     * Lấy toàn bộ cây danh mục đang hoạt động (chưa xóa mềm) dạng phân cấp n-cấp.
     * Thuật toán dựng cây O(N) in-memory để tránh N+1 query.
     *
     * @return danh sách các node cây cấp root, mỗi node chứa children lồng nhau
     */
    List<CategoryTreeResponse> getCategoryTree();

    /**
     * Lấy thông tin danh mục theo ID.
     *
     * @param categoryId ID danh mục cần lấy
     * @return thông tin danh mục
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy danh mục
     */
    CategoryResponse getCategoryById(Long categoryId);

    /**
     * Tạo danh mục gốc (root category, parentCategory = null).
     *
     * @param request   payload chứa tên danh mục
     * @param managerId ID người dùng tạo danh mục
     * @return thông tin danh mục vừa tạo
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu tên đã tồn tại ở cấp root
     */
    CategoryResponse createRootCategory(CategoryCreateRequest request, Long managerId);

    /**
     * Tạo danh mục con dưới một danh mục cha.
     *
     * @param parentId  ID danh mục cha
     * @param request   payload chứa tên danh mục con
     * @param managerId ID người dùng tạo danh mục
     * @return thông tin danh mục con vừa tạo
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu danh mục cha không tồn tại
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu tên đã tồn tại trong cùng cha
     */
    CategoryResponse createChildCategory(Long parentId, CategoryCreateRequest request, Long managerId);

    /**
     * Cập nhật thông tin danh mục (tên).
     *
     * @param categoryId ID danh mục cần cập nhật
     * @param request    payload chứa thông tin cập nhật
     * @return thông tin danh mục sau khi cập nhật
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy danh mục
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu tên mới đã tồn tại trong cùng cha
     */
    CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request);

    /**
     * Lấy thông tin danh mục và danh sách sản phẩm ACTIVE trực thuộc.
     *
     * @param categoryId ID danh mục
     * @return danh sách sản phẩm thuộc danh mục
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy danh mục
     */
    CategoryProductsResponse getProductsByCategory(Long categoryId);

    /**
     * Lấy thông tin danh mục và danh sách tất cả sản phẩm (bao gồm cả ACTIVE và INACTIVE) trực thuộc.
     * Dành cho màn hình điều hành của Quản lý (Manager) và Quản trị viên (Admin).
     *
     * @param categoryId ID danh mục
     * @return danh sách tất cả sản phẩm thuộc danh mục
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy danh mục
     */
    CategoryProductsResponse getAllProductsByCategory(Long categoryId);

    /**
     * Khóa danh mục (chuyển sang LOCKED).
     *
     * @param categoryId ID danh mục
     * @return danh mục sau khi khóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy danh mục
     */
    CategoryResponse lockCategory(Long categoryId);

    /**
     * Mở khóa danh mục (chuyển sang ACTIVE).
     *
     * @param categoryId ID danh mục
     * @return danh mục sau khi mở khóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy danh mục
     */
    CategoryResponse unlockCategory(Long categoryId);

    /**
     * Xóa mềm danh mục với cơ chế xác nhận hai bước khi còn con/sản phẩm.
     * <p>
     * Luồng xử lý:
     * <ol>
     *   <li>Nếu danh mục rỗng: xóa mềm ngay ({@code isDeleted = true}).</li>
     *   <li>Nếu còn con/sản phẩm và {@code force = false}: ném {@code BusinessRuleException}
     *       kèm payload {@link CategoryDeleteWarningResponse} để frontend hiển thị popup xác nhận.</li>
     *   <li>Nếu còn con/sản phẩm và {@code force = true}: kiểm tra đơn hàng dở dang;
     *       nếu an toàn thì cascade soft-delete toàn cây con.</li>
     * </ol>
     *
     * @param categoryId ID danh mục cần xóa
     * @param force      true nếu người dùng đã xác nhận popup cảnh báo
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy danh mục
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu cần xác nhận (force=false có con/sp)
     *                                                                  hoặc có đơn hàng dở dang trong cây con
     */
    void deleteCategory(Long categoryId, boolean force);
}
