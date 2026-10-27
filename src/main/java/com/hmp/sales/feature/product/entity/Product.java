package com.hmp.sales.feature.product.entity;

import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thực thể đại diện cho sản phẩm tôn sóng/tôn lợp mái (Product).
 * <p>
 * Lưu trữ tại bảng {@code products}. Mỗi sản phẩm thuộc một danh mục ({@link Category})
 * và có thể liên kết với một thương hiệu ({@link Brand}).
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Sản phẩm {@code INACTIVE} không cho phép đặt hàng mới.</li>
 *   <li>Xóa mềm được hỗ trợ, trừ khi sản phẩm đang có đơn hàng chưa hoàn thành.</li>
 *   <li>Khi xóa mềm, tất cả biến thể và ảnh liên quan cũng bị đánh dấu {@code isDeleted = true}.</li>
 * </ul>
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /**
     * Khóa chính định danh sản phẩm (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    /**
     * Danh mục chứa sản phẩm (bắt buộc).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Thương hiệu sản xuất (tùy chọn).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    /**
     * Tên sản phẩm (bắt buộc, tối đa 250 ký tự).
     */
    @Column(name = "product_name", nullable = false, length = 250)
    private String productName;

    /**
     * Mô tả chi tiết sản phẩm (tùy chọn, lưu dạng TEXT).
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Trạng thái sản phẩm (ACTIVE hoặc INACTIVE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    /**
     * Người dùng tạo sản phẩm (Manager).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * Danh sách ảnh sản phẩm (1 ảnh chính + tối đa 10 ảnh phụ).
     */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    /**
     * Danh sách biến thể sản phẩm (theo loại sóng, độ dày...).
     */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    /**
     * Cờ đánh dấu xóa mềm. Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm sản phẩm được tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm sản phẩm được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.status == null) this.status = ProductStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
