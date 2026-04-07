package com.zs.ytbx.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchInsuranceRequest {
    
    @NotEmpty(message = "保单ID列表不能为空")
    private List<Long> insuranceIds;
    
    @NotNull(message = "审核意见不能为空")
    private String reviewComment;
    
    private String rejectReason;
}
