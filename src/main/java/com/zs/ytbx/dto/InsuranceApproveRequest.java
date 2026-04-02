package com.zs.ytbx.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InsuranceApproveRequest {

    @NotBlank(message = "审核意见不能为空")
    private String reviewComment;
}
