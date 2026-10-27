package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.product.dto.BrandRequest;
import com.hmp.sales.feature.product.dto.BrandResponse;
import com.hmp.sales.feature.product.dto.UnitRequest;
import com.hmp.sales.feature.product.dto.UnitResponse;
import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import com.hmp.sales.feature.product.entity.BrandStatus;
import com.hmp.sales.feature.product.entity.UnitStatus;
import com.hmp.sales.feature.product.entity.ColorStatus;
import com.hmp.sales.feature.product.service.BrandService;
import com.hmp.sales.feature.product.service.ColorService;
import com.hmp.sales.feature.product.service.UnitService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho các Controller quản lý Master Data của Admin:
 * {@link AdminBrandController}, {@link AdminUnitController}, {@link AdminColorController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Master Data Controllers Unit Tests")
class AdminMasterDataControllerTest {

    private MockMvc mockMvcBrand;
    private MockMvc mockMvcUnit;
    private MockMvc mockMvcColor;

    @Mock
    private BrandService brandService;

    @Mock
    private UnitService unitService;

    @Mock
    private ColorService colorService;

    @InjectMocks
    private AdminBrandController brandController;

    @InjectMocks
    private AdminUnitController unitController;

    @InjectMocks
    private AdminColorController colorController;

    @BeforeEach
    void setUp() {
        mockMvcBrand = MockMvcBuilders.standaloneSetup(brandController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        mockMvcUnit = MockMvcBuilders.standaloneSetup(unitController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        mockMvcColor = MockMvcBuilders.standaloneSetup(colorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/admin/brands - Tạo thương hiệu 201 Created")
    void createBrand_returns201() throws Exception {
        BrandResponse response = new BrandResponse(1L, "Tôn Hoa Sen", BrandStatus.ACTIVE, null, null);
        when(brandService.createBrand(any(BrandRequest.class))).thenReturn(response);

        mockMvcBrand.perform(post("/api/v1/admin/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brandName\": \"Tôn Hoa Sen\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.brandName").value("Tôn Hoa Sen"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/brands/{id} - Còn sản phẩm liên kết trả về 400 Bad Request")
    void deleteBrand_conflict_returns400() throws Exception {
        doThrow(new BusinessRuleException("Không thể xóa vì còn sản phẩm liên kết"))
                .when(brandService).deleteBrand(1L);

        mockMvcBrand.perform(delete("/api/v1/admin/brands/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Không thể xóa vì còn sản phẩm liên kết"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/units - Tạo đơn vị tính 201 Created")
    void createUnit_returns201() throws Exception {
        UnitResponse response = new UnitResponse(1L, "mét", UnitStatus.ACTIVE, null, null);
        when(unitService.createUnit(any(UnitRequest.class))).thenReturn(response);

        mockMvcUnit.perform(post("/api/v1/admin/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitName\": \"mét\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.unitName").value("mét"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/colors - Tạo màu sắc 201 Created")
    void createColor_returns201() throws Exception {
        ColorResponse response = new ColorResponse(1L, "Xanh ngọc", ColorStatus.ACTIVE, null, null);
        when(colorService.createColor(any(ColorRequest.class))).thenReturn(response);

        mockMvcColor.perform(post("/api/v1/admin/colors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorName\": \"Xanh ngọc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.colorName").value("Xanh ngọc"));
    }
}
