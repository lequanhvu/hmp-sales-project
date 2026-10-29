package com.hmp.sales.feature.product.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import com.hmp.sales.feature.product.entity.Color;
import com.hmp.sales.feature.product.entity.ColorStatus;
import com.hmp.sales.feature.product.repository.ColorRepository;
import com.hmp.sales.feature.product.service.impl.ColorServiceImpl;
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
 * Unit tests cho {@link ColorServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ColorServiceImpl Unit Tests")
class ColorServiceImplTest {

    @Mock
    private ColorRepository colorRepository;

    @InjectMocks
    private ColorServiceImpl colorService;

    private Color color;

    @BeforeEach
    void setUp() {
        color = Color.builder()
                .id(1L)
                .colorName("Xanh ngọc")
                .status(ColorStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("searchColors - Phân trang màu sắc thành công")
    void searchColors_returnsPage() {
        when(colorRepository.searchColors(eq("ngọc"), eq(ColorStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(color)));

        Page<ColorResponse> page = colorService.searchColors("ngọc", "ACTIVE", 0, 10);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).colorName()).isEqualTo("Xanh ngọc");
    }

    @Test
    @DisplayName("createColor - Tạo mới màu sắc thành công")
    void createColor_success() {
        ColorRequest request = new ColorRequest("Đỏ đậm");
        when(colorRepository.existsByColorNameAndIsDeletedFalse("Đỏ đậm")).thenReturn(false);
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> {
            Color c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        ColorResponse response = colorService.createColor(request);

        assertThat(response).isNotNull();
        assertThat(response.colorName()).isEqualTo("Đỏ đậm");
    }

    @Test
    @DisplayName("createColor - Tạo mới màu sắc kèm ảnh thành công")
    void createColor_withImage_success() {
        ColorRequest request = new ColorRequest("Đỏ đậm", "https://cdn.example.com/do-dam.png");
        when(colorRepository.existsByColorNameAndIsDeletedFalse("Đỏ đậm")).thenReturn(false);
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> {
            Color c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        ColorResponse response = colorService.createColor(request);

        assertThat(response).isNotNull();
        assertThat(response.colorName()).isEqualTo("Đỏ đậm");
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/do-dam.png");
    }

    @Test
    @DisplayName("createColor - Tên màu sắc trùng ném BusinessRuleException")
    void createColor_duplicate_throwsException() {
        ColorRequest request = new ColorRequest("Xanh ngọc");
        when(colorRepository.existsByColorNameAndIsDeletedFalse("Xanh ngọc")).thenReturn(true);

        assertThatThrownBy(() -> colorService.createColor(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("deleteColor - Còn biến thể liên kết ném BusinessRuleException")
    void deleteColor_hasVariants_throwsException() {
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(color));
        when(colorRepository.hasActiveVariants(1L)).thenReturn(true);

        assertThatThrownBy(() -> colorService.deleteColor(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("biến thể sản phẩm đang liên kết");
    }

    @Test
    @DisplayName("deleteColor - Không có biến thể liên kết xóa mềm thành công")
    void deleteColor_noVariants_softDeletes() {
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(color));
        when(colorRepository.hasActiveVariants(1L)).thenReturn(false);

        colorService.deleteColor(1L);

        assertThat(color.getIsDeleted()).isTrue();
        verify(colorRepository).save(color);
    }
}
