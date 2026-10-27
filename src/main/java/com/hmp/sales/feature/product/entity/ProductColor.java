package com.hmp.sales.feature.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * Thực thể liên kết biến thể sản phẩm với màu sắc tôn (ProductColor).
 * <p>
 * Lưu trữ tại bảng {@code product_colors} với khóa chính phức hợp {@code (variant_id, color_id)}.
 */
@Entity
@Table(name = "product_colors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductColor {

    /**
     * Khóa chính phức hợp gồm variantId và colorId.
     */
    @EmbeddedId
    private ProductColorId id;

    /**
     * Biến thể sản phẩm liên kết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("variantId")
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    /**
     * Màu sắc liên kết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("colorId")
    @JoinColumn(name = "color_id")
    private Color color;

    /**
     * Cờ đánh dấu xóa mềm. Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductColor that = (ProductColor) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
