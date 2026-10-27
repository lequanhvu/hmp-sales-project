package com.hmp.sales.feature.product.service;

import com.hmp.sales.feature.product.dto.UnitRequest;
import com.hmp.sales.feature.product.dto.UnitResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý đơn vị tính bán hàng (Unit) cho Admin.
 * Cung cấp CRUD đầy đủ, tìm kiếm/phân trang, khóa/mở khóa,
 * và lấy danh sách dropdown công khai.
 */
public interface UnitService {

    /**
     * Tìm kiếm đơn vị tính với phân trang (dành cho Admin).
     *
     * @param keyword  từ khóa tìm theo tên (null để lấy tất cả)
     * @param status   lọc theo trạng thái (null để lấy tất cả)
     * @param page     chỉ số trang
     * @param size     số bản ghi mỗi trang
     * @return trang đơn vị tính phù hợp
     */
    Page<UnitResponse> searchUnits(String keyword, String status, int page, int size);

    /**
     * Lấy thông tin đơn vị tính theo ID.
     *
     * @param unitId ID đơn vị tính
     * @return thông tin đơn vị tính
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    UnitResponse getUnitById(Long unitId);

    /**
     * Tạo mới đơn vị tính.
     *
     * @param request payload chứa tên đơn vị tính
     * @return thông tin đơn vị tính vừa tạo
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu tên đã tồn tại
     */
    UnitResponse createUnit(UnitRequest request);

    /**
     * Cập nhật tên đơn vị tính.
     *
     * @param unitId  ID đơn vị tính cần cập nhật
     * @param request payload chứa tên mới
     * @return thông tin đơn vị tính sau khi cập nhật
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu tên mới đã tồn tại
     */
    UnitResponse updateUnit(Long unitId, UnitRequest request);

    /**
     * Khóa đơn vị tính.
     *
     * @param unitId ID đơn vị tính cần khóa
     * @return thông tin đơn vị tính sau khi khóa
     */
    UnitResponse lockUnit(Long unitId);

    /**
     * Mở khóa đơn vị tính.
     *
     * @param unitId ID đơn vị tính cần mở khóa
     * @return thông tin đơn vị tính sau khi mở khóa
     */
    UnitResponse unlockUnit(Long unitId);

    /**
     * Xóa mềm đơn vị tính. Chặn xóa nếu còn bảng giá sản phẩm liên kết.
     *
     * @param unitId ID đơn vị tính cần xóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu còn bảng giá liên kết
     */
    void deleteUnit(Long unitId);

    /**
     * Lấy danh sách đơn vị tính đang ACTIVE cho dropdown.
     *
     * @return danh sách đơn vị tính đang hoạt động
     */
    List<UnitResponse> getActiveUnits();
}
