package com.zs.ytbx.vo.claim;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaimCaseVO {

    private Long claimId;

    private String claimNo;

    private Long policyId;

    private String policyNo;

    private String productName;

    private String claimStatus;

    private String incidentTime;

    private String reportTime;

    private String progressDesc;
}
