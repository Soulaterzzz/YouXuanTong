package com.zs.ytbx.dto;

import lombok.Data;

@Data
public class CreateRechargeRequest {
    private Long userId;
    private java.math.BigDecimal amount;
    private String method;
    private String remark;
}
