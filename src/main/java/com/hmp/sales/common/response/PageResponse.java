package com.hmp.sales.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Record đóng gói dữ liệu phân trang trả về cho client theo hợp đồng API.
 * <p>
 * Cấu trúc bao gồm:
 * <ul>
 * <li>{@code result}: Danh sách phần tử dữ liệu của trang hiện tại.</li>
 * <li>{@code meta}: Thông tin siêu dữ liệu về phân trang (trang hiện tại, kích
 * thước trang, tổng số bản ghi, tổng số trang).</li>
 * </ul>
 * </p>
 *
 * @param result danh sách dữ liệu phần tử của trang hiện tại
 * @param meta   thông tin phân trang chi tiết
 * @param <T>    kiểu dữ liệu của các phần tử trong danh sách
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> result,
        PageMeta meta) {

    /**
     * Record đại diện cho thông tin siêu dữ liệu phân trang (metadata).
     *
     * @param page          chỉ số trang hiện tại (bắt đầu từ 0)
     * @param size          kích thước tối đa của mỗi trang
     * @param totalElements tổng số lượng phần tử trên toàn bộ các trang
     * @param totalPages    tổng số trang tính toán được
     */
    public record PageMeta(
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    /**
     * Phương thức tiện ích chuyển đổi trực tiếp từ đối tượng {@link Page} của
     * Spring Data sang {@link PageResponse}.
     *
     * @param page đối tượng phân trang của Spring Data JPA
     * @param <T>  kiểu dữ liệu của phần tử
     * @return đối tượng {@link PageResponse} chứa danh sách kết quả và metadata
     *         tương ứng
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        PageMeta meta = new PageMeta(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
        return new PageResponse<>(page.getContent(), meta);
    }

    /**
     * Khởi tạo nhanh một đối tượng {@link PageResponse} từ danh sách dữ liệu và các
     * chỉ số phân trang tùy biến.
     *
     * @param result        danh sách dữ liệu của trang
     * @param page          chỉ số trang hiện tại
     * @param size          kích thước mỗi trang
     * @param totalElements tổng số bản ghi
     * @param totalPages    tổng số trang
     * @param <T>           kiểu dữ liệu của phần tử
     * @return đối tượng {@link PageResponse} đã được đóng gói
     */
    public static <T> PageResponse<T> of(List<T> result, int page, int size, long totalElements, int totalPages) {
        return new PageResponse<>(result, new PageMeta(page, size, totalElements, totalPages));
    }
}
