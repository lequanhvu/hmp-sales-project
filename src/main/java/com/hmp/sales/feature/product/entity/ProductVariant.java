package com.hmp.sales.feature.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thực thể đại diện cho biến thể sản xuất của sản phẩm tôn (ProductVariant).
 * <p>
 * Lưu trữ tại bảng {@code product_variants}.
 * Mỗi biến thể đặc trưng bởi loại sóng ({@code typeValue}) và độ dày ({@code thickness}).
 * <p>
 * Lưu ý nghiệp vụ quan trọng: chiều dài/kích thước cắt do khách hàng yêu cầu khi đặt hàng
 * KHÔNG lưu tĩnh ở đây mà thuộc về snapshot của {@code OrderItem}.
 */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    /**
     * Khóa chính định danh biến thể (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long id;

    /**
     * Sản phẩm mà biến thể này thuộc về.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Mã biến thể nội bộ (tùy chọn, tối đa 80 ký tự).
     */
    @Column(name = "variant_code", length = 80)
    private String variantCode;

    /**
     * Tên hiển thị của biến thể (bắt buộc, tối đa 150 ký tự). Ví dụ: "Tôn 0.35mm - 5 sóng".
     */
    @Column(name = "variant_name", nullable = false, length = 150)
    private String variantName;

    /**
     * Loại sóng/loại tôn sản xuất (tùy chọn). Ví dụ: "5 sóng", "9 sóng".
     */
    @Column(name = "type_value", length = 100)
    private String typeValue;

    /**
     * Độ dày tôn tính theo mm (tùy chọn, độ chính xác DECIMAL(10,3)).
     */
    @Column(name = "thickness", precision = 10, scale = 3)
    private BigDecimal thickness;

    /**
     * Trạng thái biến thể (ACTIVE hoặc INACTIVE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VariantStatus status = VariantStatus.ACTIVE;

    /**
     * Danh sách màu sắc liên kết với biến thể này.
     */
    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProductColor> colors = new ArrayList<>();

    /**
     * Danh sách bảng giá của biến thể theo từng đơn vị tính.
     */
    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProductPrice> prices = new ArrayList<>();

    /**
     * Cờ đánh dấu xóa mềm. Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm biến thể được tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm biến thể được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.status == null) this.status = VariantStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductVariant that = (ProductVariant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
