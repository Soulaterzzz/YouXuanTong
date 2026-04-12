package com.zs.ytbx.controller;

import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.auth.SessionUser;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.ExpenseQuery;
import com.zs.ytbx.service.AnXinXuanService;
import com.zs.ytbx.service.impl.AnXinXuanServiceImpl;
import com.zs.ytbx.support.ControllerTestSupport;
import com.zs.ytbx.vo.anxinxuan.ExpenseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnXinXuanControllerTest extends ControllerTestSupport {

    @Mock
    private AnXinXuanService anxinXuanService;

    @Mock
    private AnXinXuanServiceImpl anxinXuanServiceImpl;

    @Mock
    private AuthContext authContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(new AnXinXuanController(anxinXuanService, anxinXuanServiceImpl, authContext));
    }

    @Test
    void shouldReturnStatsForCurrentUser() throws Exception {
        given(authContext.requireCurrentUser()).willReturn(SessionUser.builder().userId(3L).username("user3").userType("USER").build());
        given(anxinXuanServiceImpl.getTotalProducts()).willReturn(12L);
        given(anxinXuanServiceImpl.getTotalPolicies(3L)).willReturn(4L);
        given(anxinXuanServiceImpl.getTotalExpenses(3L)).willReturn(new BigDecimal("256.50"));
        given(anxinXuanServiceImpl.getBalance(3L)).willReturn(new BigDecimal("1000.00"));

        mockMvc.perform(get("/api/anxinxuan/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.totalProducts").value(12))
                .andExpect(jsonPath("$.data.totalPolicies").value(4))
                .andExpect(jsonPath("$.data.totalExpenses").value(256.5))
                .andExpect(jsonPath("$.data.balance").value(1000.0));
    }

    @Test
    void shouldActivateProductWithCurrentUser() throws Exception {
        given(authContext.requireCurrentUser()).willReturn(SessionUser.builder().userId(5L).username("user5").userType("USER").build());

        mockMvc.perform(post("/api/anxinxuan/products/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":99,\"count\":1,\"beneficiaryName\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        verify(anxinXuanService).activateProduct(ArgumentMatchers.any(), ArgumentMatchers.eq(5L));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserMissing() throws Exception {
        given(authContext.requireCurrentUser()).willThrow(new BusinessException(ResultCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/anxinxuan/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.UNAUTHORIZED.getMessage()));
    }

    @Test
    void shouldBindExpenseUsernameFilter() throws Exception {
        given(authContext.requireCurrentUser()).willReturn(SessionUser.builder().userId(3L).username("user3").userType("USER").build());
        given(anxinXuanService.listExpenses(ArgumentMatchers.any(), eq(3L))).willReturn(PageResponse.<ExpenseVO>builder()
                .records(List.of())
                .pageNo(1)
                .pageSize(10)
                .total(0)
                .totalPages(0)
                .build());

        mockMvc.perform(get("/api/anxinxuan/expenses")
                        .param("page", "1")
                        .param("size", "10")
                        .param("username", "张伟"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        ArgumentCaptor<ExpenseQuery> captor = ArgumentCaptor.forClass(ExpenseQuery.class);
        verify(anxinXuanService).listExpenses(captor.capture(), eq(3L));
        assertThat(captor.getValue().getPage()).isEqualTo(1);
        assertThat(captor.getValue().getSize()).isEqualTo(10);
        assertThat(captor.getValue().getUsername()).isEqualTo("张伟");
    }
}
