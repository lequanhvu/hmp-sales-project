package com.hmp.sales.feature.product.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.product.dto.BrandRequest;
import com.hmp.sales.feature.product.dto.BrandResponse;
import com.hmp.sales.feature.product.entity.Brand;
import com.hmp.sales.feature.product.entity.BrandStatus;
import com.hmp.sales.feature.product.repository.BrandRepository;
import com.hmp.sales.feature.product.service.impl.BrandServiceImpl;
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
 * Unit tests cho {@link BrandServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrandServiceImpl Unit Tests")
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Brand brand;

    @BeforeEach
    void setUp() {
        brand = Brand.builder()
                .id(1L)
                .brandName("Tôn Hoa Sen")
                .status(BrandStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("searchBrands - Tìm kiếm phân trang trả về đúng dữ liệu")
    void searchBrands_returnsPage() {
        when(brandRepository.searchBrands(eq("Hoa Sen"), eq(BrandStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(brand)));

        Page<BrandResponse> page = brandService.searchBrands("Hoa Sen", "ACTIVE", 0, 10);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).brandName()).isEqualTo("Tôn Hoa Sen");
    }

    @Test
    @DisplayName("createBrand - Tạo mới thương hiệu thành công")
    void createBrand_success() {
        BrandRequest request = new BrandRequest("Tôn Đông Á");
        when(brandRepository.existsByBrandNameAndIsDeletedFalse("Tôn Đông Á")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> {
            Brand b = inv.getArgument(0);
            b.setId(2L);
            return b;
        });

        BrandResponse response = brandService.createBrand(request);

        assertThat(response).isNotNull();
        assertThat(response.brandName()).isEqualTo("Tôn Đông Á");
        assertThat(response.status()).isEqualTo(BrandStatus.ACTIVE);
    }

    @Test
    @DisplayName("createBrand - Tên thương hiệu trùng ném BusinessRuleException")
    void createBrand_duplicateName_throwsException() {
        BrandRequest request = new BrandRequest("Tôn Hoa Sen");
        when(brandRepository.existsByBrandNameAndIsDeletedFalse("Tôn Hoa Sen")).thenReturn(true);

        assertThatThrownBy(() -> brandService.createBrand(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("updateBrand - Cập nhật tên thương hiệu thành công")
    void updateBrand_success() {
        BrandRequest request = new BrandRequest("Tôn Hoa Sen Group");
        when(brandRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByBrandNameAndIsDeletedFalseAndIdNot("Tôn Hoa Sen Group", 1L)).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.updateBrand(1L, request);

        assertThat(response.brandName()).isEqualTo("Tôn Hoa Sen Group");
    }

    @Test
    @DisplayName("lockBrand và unlockBrand - Đổi trạng thái chính xác")
    void lockAndUnlockBrand_updatesStatus() {
        when(brandRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse locked = brandService.lockBrand(1L);
        assertThat(locked.status()).isEqualTo(BrandStatus.LOCKED);

        BrandResponse unlocked = brandService.unlockBrand(1L);
        assertThat(unlocked.status()).isEqualTo(BrandStatus.ACTIVE);
    }

    @Test
    @DisplayName("deleteBrand - Còn sản phẩm liên kết ném BusinessRuleException (409)")
    void deleteBrand_hasActiveProducts_throwsException() {
        when(brandRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.hasActiveProducts(1L)).thenReturn(true);

        assertThatThrownBy(() -> brandService.deleteBrand(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sản phẩm đang liên kết");
    }

    @Test
    @DisplayName("deleteBrand - Không có sản phẩm liên kết thực hiện xóa mềm thành công")
    void deleteBrand_noActiveProducts_softDeletes() {
        when(brandRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.hasActiveProducts(1L)).thenReturn(false);

        brandService.deleteBrand(1L);

        assertThat(brand.getIsDeleted()).isTrue();
        verify(brandRepository).save(brand);
    }

    @Test
    @DisplayName("getActiveBrands - Lấy danh sách dropdown ACTIVE")
    void getActiveBrands_returnsList() {
        when(brandRepository.findByStatusAndIsDeletedFalseOrderByBrandNameAsc(BrandStatus.ACTIVE))
                .thenReturn(List.of(brand));

        List<BrandResponse> list = brandService.getActiveBrands();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).brandName()).isEqualTo("Tôn Hoa Sen");
    }
}
