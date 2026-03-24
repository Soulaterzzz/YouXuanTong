package com.zs.ytbx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConsultOrderRequest {

    @NotNull(message = "顾问ID不能为空")
    private Long advisorId;

    @NotBlank(message = "联系手机号不能为空")
    private String contactMobile;

    @NotBlank(message = "期望联系时间不能为空")
    private String expectContactTime;

    private String remark;
}
