package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.product.dto.*;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.entity.VariantStatus;
import com.hmp.sales.feature.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho {@link ManagerProductController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManagerProductController Unit Tests")
class ManagerProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ManagerProductController controller;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(Jwt.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return Jwt.withTokenValue("mock-token")
                        .header("alg", "none")
                        .subject("1")
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(jwtResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/manager/categories/{id}/products - Tạo sản phẩm 201 Created")
    void createProduct_returns201() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                100L, "Tôn Hoa Sen 11 sóng", "Mô tả", ProductStatus.ACTIVE,
                10L, "Tôn sóng vuông", 5L, "Tôn Hoa Sen", true, List.of(), List.of(), null, null);

        when(productService.createProduct(eq(10L), any(ProductCreateRequest.class), eq(1L)))
                .thenReturn(response);

        String json = """
                {
                    "productName": "Tôn Hoa Sen 11 sóng",
                    "description": "Mô tả",
                    "brandId": 5,
                    "variants": []
                }
                """;

        mockMvc.perform(post("/api/v1/manager/categories/10/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.productName").value("Tôn Hoa Sen 11 sóng"));
    }

    @Test
    @DisplayName("DELETE /api/v1/manager/products/{id} - Có đơn dở dang trả về 400 Bad Request")
    void deleteProduct_incompleteOrder_returns400() throws Exception {
        doThrow(new BusinessRuleException("Sản phẩm đang nằm trong đơn hàng chưa hoàn thành"))
                .when(productService).deleteProduct(100L);

        mockMvc.perform(delete("/api/v1/manager/products/100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Sản phẩm đang nằm trong đơn hàng chưa hoàn thành"));
    }

    @Test
    @DisplayName("DELETE /api/v1/manager/products/{id} - Không có đơn dở dang trả về 200 OK")
    void deleteProduct_success_returns200() throws Exception {
        doNothing().when(productService).deleteProduct(100L);

        mockMvc.perform(delete("/api/v1/manager/products/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("POST /api/v1/manager/products/{id}/images - Thêm ảnh 201 Created")
    void uploadProductImage_returns201() throws Exception {
        ProductImageResponse img = new ProductImageResponse(1L, "https://img.com/1.png", true, 0);
        when(productService.uploadProductImage(eq(100L), any(ProductImageUploadRequest.class)))
                .thenReturn(img);

        String json = """
                {
                    "imageUrl": "https://img.com/1.png",
                    "isPrimary": true,
                    "displayOrder": 0
                }
                """;

        mockMvc.perform(post("/api/v1/manager/products/100/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.imageUrl").value("https://img.com/1.png"));
    }

    @Test
    @DisplayName("POST /api/v1/manager/products/{id}/variants - Thêm biến thể 201 Created")
    void addVariant_returns201() throws Exception {
        ProductVariantResponse varResp = new ProductVariantResponse(
                200L, "VAR-01", "Tôn 0.40mm", "11 sóng vuông", new BigDecimal("0.40"),
                VariantStatus.ACTIVE, List.of(), null);

        when(productService.addVariant(eq(100L), any(ProductVariantRequest.class)))
                .thenReturn(varResp);

        String json = """
                {
                    "variantCode": "VAR-01",
                    "variantName": "Tôn 0.40mm",
                    "typeValue": "11 sóng vuông",
                    "thickness": 0.40,
                    "colorIds": [],
                    "prices": {}
                }
                """;

        mockMvc.perform(post("/api/v1/manager/products/100/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.variantCode").value("VAR-01"));
    }
}
