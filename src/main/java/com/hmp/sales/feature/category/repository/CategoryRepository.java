package com.hmp.sales.feature.category.repository;

import com.hmp.sales.feature.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link Category}.
 * Cung cấp các phương thức tìm kiếm, kiểm tra tên trùng và truy vấn cây danh mục.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Tìm danh mục theo ID và chưa bị xóa mềm.
     *
     * @param id ID danh mục cần tìm
     * @return Optional chứa danh mục nếu tồn tại và chưa xóa
     */
    Optional<Category> findByIdAndIsDeletedFalse(Long id);

    /**
     * Lấy toàn bộ danh mục chưa bị xóa mềm (dùng để dựng cây in-memory).
     *
     * @return danh sách tất cả danh mục đang hoạt động
     */
    List<Category> findAllByIsDeletedFalse();

    /**
     * Lấy danh sách danh mục con trực tiếp của một danh mục cha, chưa bị xóa.
     *
     * @param parentId ID danh mục cha
     * @return danh sách danh mục con
     */
    List<Category> findByParentCategory_IdAndIsDeletedFalse(Long parentId);

    /**
     * Lấy danh sách danh mục gốc (parentCategory là null) chưa bị xóa.
     *
     * @return danh sách danh mục cấp cao nhất
     */
    List<Category> findByParentCategoryIsNullAndIsDeletedFalse();

    /**
     * Kiểm tra tên danh mục đã tồn tại trong cùng cha hay chưa (tránh trùng tên).
     *
     * @param categoryName tên danh mục cần kiểm tra
     * @param parentId     ID danh mục cha (null nếu là root)
     * @return true nếu đã tồn tại tên trong cùng cha
     */
    boolean existsByCategoryNameAndParentCategory_IdAndIsDeletedFalse(String categoryName, Long parentId);

    /**
     * Tìm danh mục gốc theo tên (parentCategory là null) và chưa bị xóa mềm.
     *
     * @param categoryName tên danh mục
     * @return Optional chứa danh mục nếu tìm thấy
     */
    Optional<Category> findByCategoryNameAndParentCategoryIsNullAndIsDeletedFalse(String categoryName);

    /**
     * Kiểm tra tên danh mục đã tồn tại ở cấp root hay chưa (parentCategory là null).
     *
     * @param categoryName tên danh mục cần kiểm tra
     * @return true nếu đã tồn tại tên ở cấp root
     */
    boolean existsByCategoryNameAndParentCategoryIsNullAndIsDeletedFalse(String categoryName);

    /**
     * Đếm số danh mục con trực tiếp chưa xóa trong một danh mục cha.
     *
     * @param parentId ID danh mục cha
     * @return số lượng danh mục con
     */
    long countByParentCategory_IdAndIsDeletedFalse(Long parentId);

    /**
     * Lấy toàn bộ ID của cây con bắt đầu từ danh mục gốc cho trước (DFS/BFS trên DB).
     * Dùng để cascade soft-delete và kiểm tra đơn hàng dở dang trong cây con.
     *
     * @param rootId ID danh mục gốc của cây cần lấy
     * @return danh sách ID tất cả danh mục trong cây (bao gồm rootId)
     */
    @Query(value = """
            WITH RECURSIVE category_tree AS (
                SELECT category_id FROM categories WHERE category_id = :rootId AND is_deleted = false
                UNION ALL
                SELECT c.category_id FROM categories c
                INNER JOIN category_tree ct ON c.parent_category_id = ct.category_id
                WHERE c.is_deleted = false
            )
            SELECT category_id FROM category_tree
            """, nativeQuery = true)
    List<Long> findAllDescendantIds(@Param("rootId") Long rootId);

    /**
     * Soft-delete hàng loạt danh mục theo danh sách ID (cascade soft-delete cây con).
     *
     * @param ids danh sách ID danh mục cần xóa mềm
     */
    @Query("UPDATE Category c SET c.isDeleted = true WHERE c.id IN :ids")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void softDeleteByIds(@Param("ids") List<Long> ids);
}
