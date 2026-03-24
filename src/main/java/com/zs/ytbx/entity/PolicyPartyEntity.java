package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_policy_party")
public class PolicyPartyEntity extends BaseEntity {

    private Long policyId;

    private String partyRole;

    private String partyName;

    private String gender;

    private String idNo;

    private String mobile;

    private String relationDesc;
}
