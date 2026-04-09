package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("axx_insurance_record")
public class InsuranceRecordEntity extends BaseEntity {

    private Long expenseId;

    private Long userId;

    private String productName;

    private String insuredName;

    private String insuredIdNo;

    private String insuredMobile;

    private String beneficiaryName;

    private String beneficiaryIdNo;

    private String beneficiaryMobile;

    private String beneficiaryJob;

    private String beneficiaryAddress;

    private String agentName;

    private String insuranceStatus;

    private String reviewComment;

    private Long reviewerId;

    private String reviewerName;

    private LocalDateTime reviewTime;

    private String rejectReason;

    private LocalDateTime submitTime;

    private LocalDateTime underwritingTime;

    private LocalDateTime activateTime;

    private String policyNo;

    private BigDecimal premiumAmount;

    private BigDecimal displayPrice;

    private Integer quantity;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    private LocalDateTime exportTime;
}
