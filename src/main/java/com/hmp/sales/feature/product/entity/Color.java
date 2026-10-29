package com.hmp.sales.feature.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể đại diện cho màu sắc tôn sản xuất (Color).
 * <p>
 * Lưu trữ tại bảng {@code colors}. Admin quản trị CRUD đầy đủ.
 * Không được xóa mềm nếu còn biến thể sản phẩm đang liên kết.
 * </p>
 */
@Entity
@Table(name = "colors",
        uniqueConstraints = @UniqueConstraint(name = "uk_colors_name", columnNames = {"color_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Color {

    /**
     * Khóa chính định danh màu sắc (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "color_id")
    private Long id;

    /**
     * Tên màu sắc (duy nhất trong hệ thống, tối đa 100 ký tự). Ví dụ: Xanh ngọc, Đỏ đậm.
     */
    @Column(name = "color_name", nullable = false, length = 100)
    private String colorName;

    /**
     * Đường dẫn hình ảnh mẫu màu sắc (tùy chọn, tối đa 500 ký tự).
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Trạng thái hoạt động của màu sắc (ACTIVE hoặc LOCKED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ColorStatus status = ColorStatus.ACTIVE;

    /**
     * Cờ đánh dấu xóa mềm. Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm màu sắc được tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm màu sắc được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.status == null) this.status = ColorStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Color color = (Color) o;
        return Objects.equals(id, color.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
