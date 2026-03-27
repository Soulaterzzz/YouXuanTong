package com.zs.ytbx.controller;

import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.dto.NoticeVO;
import com.zs.ytbx.service.NoticeService;
import com.zs.ytbx.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NoticeControllerTest extends ControllerTestSupport {

    @Mock
    private NoticeService noticeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(new NoticeController(noticeService));
    }

    @Test
    void shouldListPublishedNotices() throws Exception {
        given(noticeService.listPublishedNotices()).willReturn(List.of(
                NoticeVO.builder().id(1L).title("系统升级").content("3月29日凌晨维护").sortNo(1).build()
        ));

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].title").value("系统升级"))
                .andExpect(jsonPath("$.data[0].content").value("3月29日凌晨维护"));
    }
}
