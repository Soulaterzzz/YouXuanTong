package com.zs.ytbx.vo.policy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PolicyListItemVO {

    private Long policyId;

    private String policyNo;

    private String productName;

    private String categoryName;

    private String effectiveDate;

    private String policyStatus;
}
