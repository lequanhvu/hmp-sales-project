package com.hmp.sales.feature.product.service;

import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý màu sắc tôn sản xuất (Color) cho Admin.
 * Cung cấp CRUD đầy đủ, tìm kiếm/phân trang, khóa/mở khóa,
 * và lấy danh sách dropdown công khai.
 */
public interface ColorService {

    /**
     * Tìm kiếm màu sắc với phân trang (dành cho Admin).
     *
     * @param keyword  từ khóa tìm theo tên (null để lấy tất cả)
     * @param status   lọc theo trạng thái (null để lấy tất cả)
     * @param page     chỉ số trang
     * @param size     số bản ghi mỗi trang
     * @return trang màu sắc phù hợp
     */
    Page<ColorResponse> searchColors(String keyword, String status, int page, int size);

    /**
     * Lấy thông tin màu sắc theo ID.
     *
     * @param colorId ID màu sắc
     * @return thông tin màu sắc
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    ColorResponse getColorById(Long colorId);

    /**
     * Tạo mới màu sắc.
     *
     * @param request payload chứa tên màu sắc
     * @return thông tin màu sắc vừa tạo
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu tên đã tồn tại
     */
    ColorResponse createColor(ColorRequest request);

    /**
     * Cập nhật tên màu sắc.
     *
     * @param colorId ID màu sắc cần cập nhật
     * @param request payload chứa tên mới
     * @return thông tin màu sắc sau khi cập nhật
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu tên mới đã tồn tại
     */
    ColorResponse updateColor(Long colorId, ColorRequest request);

    /**
     * Khóa màu sắc.
     *
     * @param colorId ID màu sắc cần khóa
     * @return thông tin màu sắc sau khi khóa
     */
    ColorResponse lockColor(Long colorId);

    /**
     * Mở khóa màu sắc.
     *
     * @param colorId ID màu sắc cần mở khóa
     * @return thông tin màu sắc sau khi mở khóa
     */
    ColorResponse unlockColor(Long colorId);

    /**
     * Xóa mềm màu sắc. Chặn xóa nếu còn biến thể sản phẩm liên kết.
     *
     * @param colorId ID màu sắc cần xóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu còn biến thể liên kết
     */
    void deleteColor(Long colorId);

    /**
     * Lấy danh sách màu sắc đang ACTIVE cho dropdown.
     *
     * @return danh sách màu sắc đang hoạt động
     */
    List<ColorResponse> getActiveColors();
}
