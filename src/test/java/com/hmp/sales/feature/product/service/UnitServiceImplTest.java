package com.hmp.sales.feature.product.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.feature.product.dto.UnitRequest;
import com.hmp.sales.feature.product.dto.UnitResponse;
import com.hmp.sales.feature.product.entity.Unit;
import com.hmp.sales.feature.product.entity.UnitStatus;
import com.hmp.sales.feature.product.repository.UnitRepository;
import com.hmp.sales.feature.product.service.impl.UnitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link UnitServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnitServiceImpl Unit Tests")
class UnitServiceImplTest {

    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private UnitServiceImpl unitService;

    private Unit unit;

    @BeforeEach
    void setUp() {
        unit = Unit.builder()
                .id(1L)
                .unitName("m")
                .status(UnitStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("searchUnits - Phân trang đơn vị tính thành công")
    void searchUnits_returnsPage() {
        when(unitRepository.searchUnits(eq("m"), eq(UnitStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unit)));

        Page<UnitResponse> page = unitService.searchUnits("m", "ACTIVE", 0, 10);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).unitName()).isEqualTo("m");
    }

    @Test
    @DisplayName("createUnit - Tạo mới đơn vị tính thành công")
    void createUnit_success() {
        UnitRequest request = new UnitRequest("tấm");
        when(unitRepository.existsByUnitNameAndIsDeletedFalse("tấm")).thenReturn(false);
        when(unitRepository.save(any(Unit.class))).thenAnswer(inv -> {
            Unit u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UnitResponse response = unitService.createUnit(request);

        assertThat(response).isNotNull();
        assertThat(response.unitName()).isEqualTo("tấm");
    }

    @Test
    @DisplayName("createUnit - Tên đơn vị tính trùng ném BusinessRuleException")
    void createUnit_duplicate_throwsException() {
        UnitRequest request = new UnitRequest("m");
        when(unitRepository.existsByUnitNameAndIsDeletedFalse("m")).thenReturn(true);

        assertThatThrownBy(() -> unitService.createUnit(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("deleteUnit - Còn bảng giá liên kết ném BusinessRuleException")
    void deleteUnit_hasPrices_throwsException() {
        when(unitRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(unit));
        when(unitRepository.hasActivePrices(1L)).thenReturn(true);

        assertThatThrownBy(() -> unitService.deleteUnit(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bảng giá sản phẩm đang liên kết");
    }

    @Test
    @DisplayName("deleteUnit - Không có bảng giá liên kết xóa mềm thành công")
    void deleteUnit_noPrices_softDeletes() {
        when(unitRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(unit));
        when(unitRepository.hasActivePrices(1L)).thenReturn(false);

        unitService.deleteUnit(1L);

        assertThat(unit.getIsDeleted()).isTrue();
        verify(unitRepository).save(unit);
    }
}
