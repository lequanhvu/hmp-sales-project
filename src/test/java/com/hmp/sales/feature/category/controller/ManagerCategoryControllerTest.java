package com.hmp.sales.feature.category.controller;

import com.hmp.sales.common.exception.ConflictWithPayloadException;
import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.category.dto.CategoryCreateRequest;
import com.hmp.sales.feature.category.dto.CategoryDeleteWarningResponse;
import com.hmp.sales.feature.category.dto.CategoryProductsResponse;
import com.hmp.sales.feature.category.dto.CategoryResponse;
import com.hmp.sales.feature.category.entity.CategoryStatus;
import com.hmp.sales.feature.category.service.CategoryService;
import com.hmp.sales.feature.product.dto.ProductSummaryResponse;
import com.hmp.sales.feature.product.entity.ProductStatus;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho {@link ManagerCategoryController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManagerCategoryController Unit Tests")
class ManagerCategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ManagerCategoryController controller;

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
    @DisplayName("POST /api/v1/manager/categories - Tạo danh mục gốc 201 Created")
    void createRootCategory_returns201() throws Exception {
        CategoryResponse response = new CategoryResponse(10L, "Tôn lợp mái", CategoryStatus.ACTIVE, null, null, null);
        when(categoryService.createRootCategory(any(CategoryCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/manager/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\": \"Tôn lợp mái\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.categoryName").value("Tôn lợp mái"));
    }

    @Test
    @DisplayName("POST /api/v1/manager/categories/{parentId}/children - Tạo danh mục con 201 Created")
    void createChildCategory_returns201() throws Exception {
        CategoryResponse response = new CategoryResponse(20L, "Tôn sóng vuông", CategoryStatus.ACTIVE, 10L, null, null);
        when(categoryService.createChildCategory(eq(10L), any(CategoryCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/manager/categories/10/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\": \"Tôn sóng vuông\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.categoryName").value("Tôn sóng vuông"))
                .andExpect(jsonPath("$.data.parentId").value(10));
    }

    @Test
    @DisplayName("DELETE /api/v1/manager/categories/{id} - Còn con/sản phẩm mà force=false trả về 409 Conflict kèm payload confirm")
    void deleteCategory_withChildren_forceFalse_returns409WithPayload() throws Exception {
        CategoryDeleteWarningResponse warning = new CategoryDeleteWarningResponse(
                10L, "Tôn lợp mái", 2L, 5L, 7L);
        doThrow(new ConflictWithPayloadException("Danh mục này chứa 7 danh mục và sản phẩm", warning))
                .when(categoryService).deleteCategory(10L, false);

        mockMvc.perform(delete("/api/v1/manager/categories/10?force=false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.data.childCategoryCount").value(2))
                .andExpect(jsonPath("$.data.productCount").value(5))
                .andExpect(jsonPath("$.data.totalImpactCount").value(7));
    }

    @Test
    @DisplayName("DELETE /api/v1/manager/categories/{id} - force=true xóa thành công trả về 200 OK")
    void deleteCategory_forceTrue_returns200() throws Exception {
        doNothing().when(categoryService).deleteCategory(10L, true);

        mockMvc.perform(delete("/api/v1/manager/categories/10?force=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/manager/categories/{id}/products - Lấy danh sách sản phẩm (cả ACTIVE và INACTIVE) 200 OK")
    void getProductsByCategory_returnsAllProducts() throws Exception {
        ProductSummaryResponse p1 = new ProductSummaryResponse(
                101L, "Tôn 5 sóng", ProductStatus.ACTIVE, 10L, "Tôn lợp mái", 1L, "Hoa Sen", null, true);
        ProductSummaryResponse p2 = new ProductSummaryResponse(
                102L, "Tôn ngừng bán", ProductStatus.INACTIVE, 10L, "Tôn lợp mái", 1L, "Hoa Sen", null, true);

        CategoryProductsResponse response = new CategoryProductsResponse(
                10L, "Tôn lợp mái", java.util.List.of(p1, p2));

        when(categoryService.getAllProductsByCategory(10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/manager/categories/10/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.categoryId").value(10))
                .andExpect(jsonPath("$.data.products").isArray())
                .andExpect(jsonPath("$.data.products.length()").value(2))
                .andExpect(jsonPath("$.data.products[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.products[1].status").value("INACTIVE"));
    }
}
