package com.zs.ytbx.dto;

import lombok.Data;

@Data
public class ActivateRequest {
    private Long productId;
    private String planName;
    private String beneficiaryName;
    private String beneficiaryId;
    private String beneficiaryJob;
    private Integer count;
    private String address;
    private String agent;
}
