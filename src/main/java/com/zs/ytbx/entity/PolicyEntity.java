package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_policy")
public class PolicyEntity extends BaseEntity {

    private String policyNo;

    private Long customerId;

    private Long productId;

    private Long planId;

    private String policyStatus;

    private String paymentStatus;

    private String underwritingStatus;

    private BigDecimal coverageAmount;

    private BigDecimal premiumAmount;

    private String insurancePeriodDesc;

    private String paymentPeriodDesc;

    private LocalDateTime effectiveDate;

    private LocalDateTime expiryDate;

    private String ePolicyFileId;

    private Integer trustFlag;
}
