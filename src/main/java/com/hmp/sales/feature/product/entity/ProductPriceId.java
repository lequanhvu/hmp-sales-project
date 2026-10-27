package com.hmp.sales.feature.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Khóa chính phức hợp cho bảng {@code product_prices}.
 * Bao gồm {@code variantId} và {@code unitId}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductPriceId implements Serializable {

    /**
     * ID biến thể sản phẩm.
     */
    @Column(name = "variant_id")
    private Long variantId;

    /**
     * ID đơn vị tính.
     */
    @Column(name = "unit_id")
    private Long unitId;
}
