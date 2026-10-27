package com.hmp.sales.feature.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * Thực thể đại diện cho bảng giá sản phẩm theo biến thể và đơn vị tính (ProductPrice).
 * <p>
 * Lưu trữ tại bảng {@code product_prices} với khóa chính phức hợp {@code (variant_id, unit_id)}.
 * Giá lưu theo số nguyên VND (INT) để tránh sai số dấu phẩy động.
 * <p>
 * Công thức tính giá sản xuất: {@code TotalPrice = UnitPrice × Size × Quantity}.
 */
@Entity
@Table(name = "product_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPrice {

    /**
     * Khóa chính phức hợp gồm variantId và unitId.
     */
    @EmbeddedId
    private ProductPriceId id;

    /**
     * Biến thể sản phẩm được định giá.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("variantId")
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    /**
     * Đơn vị tính tương ứng với mức giá này (m, cái, tấm...).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("unitId")
    @JoinColumn(name = "unit_id")
    private Unit unit;

    /**
     * Đơn giá theo VND (số nguyên, KHÔNG dùng kiểu floating-point).
     * Ví dụ: 100000 nghĩa là 100.000 đồng/m.
     */
    @Column(name = "price", nullable = false)
    private Integer price;

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
        ProductPrice that = (ProductPrice) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
