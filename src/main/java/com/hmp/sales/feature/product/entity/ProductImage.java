package com.hmp.sales.feature.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể đại diện cho ảnh của sản phẩm (ProductImage).
 * <p>
 * Lưu trữ tại bảng {@code product_images}.
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Mỗi sản phẩm có tối đa 1 ảnh chính ({@code isPrimary = true}).</li>
 *   <li>Tối đa 10 ảnh phụ ({@code isPrimary = false}).</li>
 *   <li>Tổng không quá 11 ảnh cho một sản phẩm.</li>
 * </ul>
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    /**
     * Khóa chính định danh ảnh sản phẩm (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id")
    private Long id;

    /**
     * Sản phẩm chứa ảnh này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * URL ảnh lưu trên cloud/object storage (tối đa 1000 ký tự).
     */
    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    /**
     * Cờ xác định đây có phải ảnh đại diện (primary) hay không.
     * Tối đa 1 ảnh primary cho mỗi sản phẩm.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Thứ tự hiển thị của ảnh trong danh sách.
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Cờ đánh dấu xóa mềm. Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm ảnh được tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.isPrimary == null) this.isPrimary = false;
        if (this.displayOrder == null) this.displayOrder = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductImage that = (ProductImage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
