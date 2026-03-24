package com.zs.ytbx.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRechargeRequest {
    private BigDecimal amount;
    private String method;
    private String remark;
}
