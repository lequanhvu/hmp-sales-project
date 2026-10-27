package com.hmp.sales.feature.category.entity;

import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thực thể đại diện cho danh mục sản phẩm trong hệ thống (Category).
 * <p>
 * Lưu trữ tại bảng {@code categories}. Hỗ trợ cấu trúc cây tự tham chiếu (self-referencing tree):
 * danh mục gốc có {@code parentCategory = null}; danh mục con trỏ đến danh mục cha qua
 * {@code parent_category_id}.
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Ứng dụng phải ngăn chặn chu trình (cycle) khi gán parentCategory mới.</li>
 *   <li>Xóa mềm (soft delete) được hỗ trợ; cascade xóa cây con khi được xác nhận.</li>
 * </ul>
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    /**
     * Khóa chính định danh danh mục (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    /**
     * Danh mục cha trong cây phân cấp. Null nếu là danh mục gốc (root category).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    /**
     * Danh sách danh mục con trực tiếp. Không eager load để tránh N+1.
     */
    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    /**
     * Tên hiển thị của danh mục (bắt buộc, tối đa 150 ký tự).
     */
    @Column(name = "category_name", nullable = false, length = 150)
    private String categoryName;

    /**
     * Trạng thái hoạt động của danh mục (ACTIVE hoặc LOCKED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CategoryStatus status = CategoryStatus.ACTIVE;

    /**
     * Người dùng tạo danh mục (thường là Manager).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * Cờ đánh dấu xóa mềm. Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm danh mục được tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm danh mục được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.status == null) this.status = CategoryStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
