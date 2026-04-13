package com.zs.ytbx.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActivateRequest {
    private Long productId;
    private String planName;
    @DecimalMin(value = "0.01", message = "显示价格必须大于0")
    private BigDecimal displayPrice;
    private String policyHolderName;
    private String policyHolderId;
    private String beneficiaryName;
    private String beneficiaryId;
    private String beneficiaryJob;
    private Integer count;
    private String agent;
}
