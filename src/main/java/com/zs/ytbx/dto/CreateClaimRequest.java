package com.zs.ytbx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateClaimRequest {

    @NotNull(message = "保单ID不能为空")
    private Long policyId;

    @NotBlank(message = "出险时间不能为空")
    private String incidentTime;

    @NotBlank(message = "出险说明不能为空")
    private String incidentDesc;

    @NotBlank(message = "联系手机号不能为空")
    private String contactMobile;
}
