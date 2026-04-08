package com.zs.ytbx.config;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.enums.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldHideInternalExceptionMessage() {
        ApiResponse<String> response = globalExceptionHandler.handleException(
                new IllegalStateException("database password leaked"));

        assertThat(response.getCode()).isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(response.getMessage()).isEqualTo(ResultCode.SYSTEM_ERROR.getMessage());
        assertThat(response.getData()).isNull();
        assertThat(response.getTraceId()).isNotBlank();
    }
}
