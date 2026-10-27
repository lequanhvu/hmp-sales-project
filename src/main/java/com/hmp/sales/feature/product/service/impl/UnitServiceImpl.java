package com.hmp.sales.feature.product.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.product.dto.UnitRequest;
import com.hmp.sales.feature.product.dto.UnitResponse;
import com.hmp.sales.feature.product.entity.Unit;
import com.hmp.sales.feature.product.entity.UnitStatus;
import com.hmp.sales.feature.product.repository.UnitRepository;
import com.hmp.sales.feature.product.service.UnitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ quản lý đơn vị tính bán hàng (Unit) cho Admin.
 */
@Slf4j
@Service
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;

    /**
     * Khởi tạo UnitServiceImpl với repository đơn vị tính.
     *
     * @param unitRepository repository quản lý đơn vị tính
     */
    public UnitServiceImpl(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<UnitResponse> searchUnits(String keyword, String status, int page, int size) {
        UnitStatus unitStatus = parseStatus(status);
        var pageable = PageRequest.of(page, size, Sort.by("unitName").ascending());
        return unitRepository.searchUnits(keyword, unitStatus, pageable)
                .map(UnitResponse::fromEntity);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UnitResponse getUnitById(Long unitId) {
        return UnitResponse.fromEntity(findActiveUnit(unitId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UnitResponse createUnit(UnitRequest request) {
        if (unitRepository.existsByUnitNameAndIsDeletedFalse(request.unitName())) {
            throw new BusinessRuleException("Tên đơn vị tính '" + request.unitName() + "' đã tồn tại.");
        }
        Unit unit = Unit.builder().unitName(request.unitName()).build();
        unit = unitRepository.save(unit);
        log.info("[UnitService] Tạo đơn vị tính: {} (ID={})", unit.getUnitName(), unit.getId());
        return UnitResponse.fromEntity(unit);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UnitResponse updateUnit(Long unitId, UnitRequest request) {
        Unit unit = findActiveUnit(unitId);
        if (unitRepository.existsByUnitNameAndIsDeletedFalseAndIdNot(request.unitName(), unitId)) {
            throw new BusinessRuleException("Tên đơn vị tính '" + request.unitName() + "' đã tồn tại.");
        }
        unit.setUnitName(request.unitName());
        return UnitResponse.fromEntity(unitRepository.save(unit));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UnitResponse lockUnit(Long unitId) {
        Unit unit = findActiveUnit(unitId);
        unit.setStatus(UnitStatus.LOCKED);
        return UnitResponse.fromEntity(unitRepository.save(unit));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UnitResponse unlockUnit(Long unitId) {
        Unit unit = findActiveUnit(unitId);
        unit.setStatus(UnitStatus.ACTIVE);
        return UnitResponse.fromEntity(unitRepository.save(unit));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteUnit(Long unitId) {
        Unit unit = findActiveUnit(unitId);
        if (unitRepository.hasActivePrices(unitId)) {
            throw new BusinessRuleException("Không thể xóa đơn vị tính vì còn bảng giá sản phẩm đang liên kết.");
        }
        unit.setIsDeleted(true);
        unitRepository.save(unit);
        log.info("[UnitService] Xóa mềm đơn vị tính ID={}", unitId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> getActiveUnits() {
        return unitRepository.findByStatusAndIsDeletedFalseOrderByUnitNameAsc(UnitStatus.ACTIVE)
                .stream().map(UnitResponse::fromEntity).collect(Collectors.toList());
    }

    private Unit findActiveUnit(Long unitId) {
        return unitRepository.findByIdAndIsDeletedFalse(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vị tính ID: " + unitId));
    }

    private UnitStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try { return UnitStatus.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
