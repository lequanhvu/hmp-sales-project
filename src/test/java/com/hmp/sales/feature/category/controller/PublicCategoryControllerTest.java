package com.hmp.sales.feature.category.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.category.dto.CategoryProductsResponse;
import com.hmp.sales.feature.category.dto.CategoryTreeResponse;
import com.hmp.sales.feature.category.entity.CategoryStatus;
import com.hmp.sales.feature.category.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho {@link PublicCategoryController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicCategoryController Unit Tests")
class PublicCategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private PublicCategoryController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/categories/tree - Trả về cấu trúc cây 200 OK")
    void getCategoryTree_returns200() throws Exception {
        CategoryTreeResponse treeNode = new CategoryTreeResponse(1L, "Tôn lợp mái", CategoryStatus.ACTIVE, List.of());
        when(categoryService.getCategoryTree()).thenReturn(List.of(treeNode));

        mockMvc.perform(get("/api/v1/categories/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].categoryName").value("Tôn lợp mái"));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id}/products - Trả về danh sách sản phẩm trong danh mục 200 OK")
    void getProductsByCategory_returns200() throws Exception {
        CategoryProductsResponse resp = new CategoryProductsResponse(1L, "Tôn lợp mái", List.of());
        when(categoryService.getProductsByCategory(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/categories/1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.categoryId").value(1))
                .andExpect(jsonPath("$.data.categoryName").value("Tôn lợp mái"));
    }
}
