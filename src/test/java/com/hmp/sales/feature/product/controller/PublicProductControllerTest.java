package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.product.dto.ProductDetailResponse;
import com.hmp.sales.feature.product.dto.ProductFilterParams;
import com.hmp.sales.feature.product.dto.ProductSummaryResponse;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho {@link PublicProductController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicProductController Unit Tests")
class PublicProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private PublicProductController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/products - Tìm kiếm sản phẩm 200 OK")
    void searchProducts_returns200() throws Exception {
        ProductSummaryResponse item = new ProductSummaryResponse(
                100L, "Tôn Hoa Sen 11 sóng", ProductStatus.ACTIVE,
                10L, "Tôn sóng vuông", 5L, "Tôn Hoa Sen", "https://img.com/1.png", true);

        when(productService.searchProducts(any(ProductFilterParams.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/v1/products")
                        .param("productName", "Hoa Sen")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.result[0].productName").value("Tôn Hoa Sen 11 sóng"));
    }

    @Test
    @DisplayName("GET /api/v1/products - Sort theo giá khi thiếu quyền ném 400 Bad Request")
    void searchProducts_sortByPrice_throws400() throws Exception {
        when(productService.searchProducts(any(ProductFilterParams.class)))
                .thenThrow(new BusinessRuleException("Không thể sắp xếp theo giá"));

        mockMvc.perform(get("/api/v1/products")
                        .param("sortBy", "price")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Lấy chi tiết sản phẩm 200 OK")
    void getProductDetail_returns200() throws Exception {
        ProductDetailResponse detail = new ProductDetailResponse(
                100L, "Tôn Hoa Sen 11 sóng", "Mô tả", ProductStatus.ACTIVE,
                10L, "Tôn sóng vuông", 5L, "Tôn Hoa Sen", true, List.of(), List.of(), null, null);

        when(productService.getProductDetail(100L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/products/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.productName").value("Tôn Hoa Sen 11 sóng"));
    }
}
