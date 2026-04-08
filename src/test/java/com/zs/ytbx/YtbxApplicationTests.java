package com.zs.ytbx;

import com.zs.ytbx.common.enums.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "ytbx.mock.enabled=true",
        "spring.main.lazy-initialization=true",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
class YtbxApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldBootAndServeMockModeEndpoint() throws Exception {
        mockMvc.perform(get("/api/portal/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void shouldBootAuthInterceptorAndExceptionHandler() throws Exception {
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.UNAUTHORIZED.getMessage()));
    }
}
