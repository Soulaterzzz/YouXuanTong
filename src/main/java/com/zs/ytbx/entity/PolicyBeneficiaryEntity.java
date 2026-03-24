package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_policy_beneficiary")
public class PolicyBeneficiaryEntity extends BaseEntity {

    private Long policyId;

    private String beneficiaryName;

    private String beneficiaryType;

    private BigDecimal beneficiaryRatio;

    private String relationDesc;
}
