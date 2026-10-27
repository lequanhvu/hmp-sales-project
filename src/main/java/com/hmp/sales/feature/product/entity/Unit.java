package com.hmp.sales.feature.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể đại diện cho đơn vị tính bán hàng (Unit).
 * <p>
 * Lưu trữ tại bảng {@code units}. Ví dụ: m (mét), tấm, cuộn, cái.
 * Admin quản trị CRUD đầy đủ.
 * Không được xóa mềm nếu còn bảng giá sản phẩm đang liên kết.
 * </p>
 */
@Entity
@Table(name = "units",
        uniqueConstraints = @UniqueConstraint(name = "uk_units_name", columnNames = {"unit_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unit {

    /**
     * Khóa chính định danh đơn vị tính (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id")
    private Long id;

    /**
     * Tên đơn vị tính (duy nhất trong hệ thống, tối đa 50 ký tự). Ví dụ: m, cái, tấm.
     */
    @Column(name = "unit_name", nullable = false, length = 50)
    private String unitName;

    /**
     * Trạng thái hoạt động của đơn vị tính (ACTIVE hoặc LOCKED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UnitStatus status = UnitStatus.ACTIVE;

    /**
     * Cờ đánh dấu xóa mềm. Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm đơn vị tính được tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm đơn vị tính được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.status == null) this.status = UnitStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Unit unit = (Unit) o;
        return Objects.equals(id, unit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
