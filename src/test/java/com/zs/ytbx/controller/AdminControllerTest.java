package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.auth.SessionUser;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.dto.UserVO;
import com.zs.ytbx.service.AdminService;
import com.zs.ytbx.service.NoticeService;
import com.zs.ytbx.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest extends ControllerTestSupport {

    @Mock
    private AdminService adminService;

    @Mock
    private NoticeService noticeService;

    @Mock
    private AuthContext authContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(new AdminController(adminService, noticeService, authContext));
    }

    @Test
    void shouldListUsersForAdmin() throws Exception {
        given(authContext.requireCurrentUser()).willReturn(adminUser());
        given(adminService.listUsers(any())).willReturn(PageResponse.<UserVO>builder()
                .records(List.of(UserVO.builder()
                        .id(11L)
                        .username("alice")
                        .status("ACTIVE")
                        .build()))
                .pageNo(2)
                .pageSize(5)
                .total(1)
                .totalPages(1)
                .build());

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "2")
                        .param("size", "5")
                        .param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.records[0].username").value("alice"))
                .andExpect(jsonPath("$.data.pageNo").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        ArgumentCaptor<com.zs.ytbx.dto.UserQuery> captor = ArgumentCaptor.forClass(com.zs.ytbx.dto.UserQuery.class);
        verify(adminService).listUsers(captor.capture());
        assertThat(captor.getValue().getPage()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(5);
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    void shouldRejectNonAdminAccess() throws Exception {
        given(authContext.requireCurrentUser()).willReturn(SessionUser.builder()
                .userId(2L)
                .username("user2")
                .userType("USER")
                .build());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.FORBIDDEN.getMessage()));
    }

    @Test
    void shouldValidateCreateProductRequest() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "P-001",
                                  "productName": "家庭意外险",
                                  "categoryCode": "1-3",
                                  "price": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_PARAM.getCode()))
                .andExpect(jsonPath("$.data").value("价格必须大于0"));
    }

    @Test
    void shouldPassReviewerContextWhenApprovingInsurance() throws Exception {
        given(authContext.requireCurrentUser()).willReturn(adminUser());

        mockMvc.perform(put("/api/admin/insurances/88/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewComment\":\"资料齐全，同意承保\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        verify(adminService).approveInsurance(any(), any(), org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("admin"));
    }

    @Test
    void shouldValidateNoticePublishRequest() throws Exception {
        mockMvc.perform(post("/api/admin/notices/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notices": [
                                    {
                                      "sortNo": 1,
                                      "title": "1234567890123456789012345678901",
                                      "content": "首页公告"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_PARAM.getCode()))
                .andExpect(jsonPath("$.data").value("通知标题长度不能超过30个字符"));
    }

    @Test
    void shouldValidateBatchActivationRequest() throws Exception {
        mockMvc.perform(post("/api/admin/insurances/activate-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_PARAM.getCode()))
                .andExpect(jsonPath("$.data").value("保单列表不能为空"));
    }

    private SessionUser adminUser() {
        return SessionUser.builder()
                .userId(1L)
                .username("admin")
                .userType("ADMIN")
                .build();
    }
}
