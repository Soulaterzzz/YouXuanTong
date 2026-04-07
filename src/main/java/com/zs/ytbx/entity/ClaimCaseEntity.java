package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_claim_case")
public class ClaimCaseEntity extends BaseEntity {

    private String claimNo;

    private Long policyId;

    private Long customerId;

    private String claimStatus;

    private LocalDateTime incidentTime;

    private LocalDateTime reportTime;

    private BigDecimal claimAmount;

    private String progressDesc;

    private LocalDateTime closeTime;
}
