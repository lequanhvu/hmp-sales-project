package com.hmp.sales.feature.product.service;

import com.hmp.sales.feature.product.dto.BrandRequest;
import com.hmp.sales.feature.product.dto.BrandResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý thương hiệu sản phẩm (Brand) cho Admin.
 * Cung cấp CRUD đầy đủ, tìm kiếm/phân trang, khóa/mở khóa,
 * và lấy danh sách dropdown công khai.
 */
public interface BrandService {

    /**
     * Tìm kiếm thương hiệu với phân trang và lọc theo từ khóa/trạng thái (dành cho Admin).
     *
     * @param keyword  từ khóa tìm theo tên (null để lấy tất cả)
     * @param status   lọc theo trạng thái (null để lấy tất cả)
     * @param page     chỉ số trang (bắt đầu từ 0)
     * @param size     số bản ghi mỗi trang
     * @return trang thương hiệu phù hợp
     */
    Page<BrandResponse> searchBrands(String keyword, String status, int page, int size);

    /**
     * Lấy thông tin thương hiệu theo ID.
     *
     * @param brandId ID thương hiệu cần lấy
     * @return thông tin thương hiệu
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    BrandResponse getBrandById(Long brandId);

    /**
     * Tạo mới thương hiệu.
     *
     * @param request payload chứa tên thương hiệu
     * @return thông tin thương hiệu vừa tạo
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu tên đã tồn tại
     */
    BrandResponse createBrand(BrandRequest request);

    /**
     * Cập nhật tên thương hiệu.
     *
     * @param brandId ID thương hiệu cần cập nhật
     * @param request payload chứa tên mới
     * @return thông tin thương hiệu sau khi cập nhật
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu tên mới đã tồn tại
     */
    BrandResponse updateBrand(Long brandId, BrandRequest request);

    /**
     * Khóa thương hiệu (chuyển sang trạng thái LOCKED).
     *
     * @param brandId ID thương hiệu cần khóa
     * @return thông tin thương hiệu sau khi khóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    BrandResponse lockBrand(Long brandId);

    /**
     * Mở khóa thương hiệu (chuyển về trạng thái ACTIVE).
     *
     * @param brandId ID thương hiệu cần mở khóa
     * @return thông tin thương hiệu sau khi mở khóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     */
    BrandResponse unlockBrand(Long brandId);

    /**
     * Xóa mềm thương hiệu. Chặn xóa nếu còn sản phẩm đang liên kết.
     *
     * @param brandId ID thương hiệu cần xóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy
     * @throws com.hmp.sales.common.exception.BusinessRuleException    nếu còn sản phẩm liên kết
     */
    void deleteBrand(Long brandId);

    /**
     * Lấy danh sách thương hiệu đang ACTIVE cho dropdown công khai/quản lý.
     *
     * @return danh sách thương hiệu đang hoạt động, sắp xếp theo tên
     */
    List<BrandResponse> getActiveBrands();
}
