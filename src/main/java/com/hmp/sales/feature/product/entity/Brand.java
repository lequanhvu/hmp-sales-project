package com.hmp.sales.feature.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể đại diện cho thương hiệu/nhà sản xuất tôn sản phẩm (Brand).
 * <p>
 * Lưu trữ tại bảng {@code brands}. Admin quản trị CRUD đầy đủ.
 * Không được xóa mềm nếu còn sản phẩm đang liên kết.
 * </p>
 */
@Entity
@Table(name = "brands",
        uniqueConstraints = @UniqueConstraint(name = "uk_brands_name", columnNames = {"brand_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    /**
     * Khóa chính định danh thương hiệu (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    private Long id;

    /**
     * Tên thương hiệu (duy nhất trong hệ thống, tối đa 150 ký tự).
     */
    @Column(name = "brand_name", nullable = false, length = 150)
    private String brandName;

    /**
     * Trạng thái hoạt động của thương hiệu (ACTIVE hoặc LOCKED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BrandStatus status = BrandStatus.ACTIVE;

    /**
     * Cờ đánh dấu xóa mềm. Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm thương hiệu được tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm thương hiệu được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.status == null) this.status = BrandStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Brand brand = (Brand) o;
        return Objects.equals(id, brand.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
