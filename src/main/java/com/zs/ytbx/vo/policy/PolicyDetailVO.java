package com.zs.ytbx.vo.policy;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PolicyDetailVO {

    private Long policyId;

    private String policyNo;

    private String policyStatus;

    private String productName;

    private String categoryName;

    private String coverageAmountText;

    private String insurancePeriodDesc;

    private String paymentPeriodDesc;

    private String effectiveDate;

    private List<PartyInfo> parties;

    private List<BeneficiaryInfo> beneficiaries;

    private List<GuaranteeInfo> guarantees;

    private TrustInfo trustInfo;

    @Data
    @Builder
    public static class PartyInfo {
        private String partyRole;
        private String partyName;
        private String relationDesc;
    }

    @Data
    @Builder
    public static class BeneficiaryInfo {
        private String beneficiaryName;
        private String beneficiaryType;
        private String ratioText;
    }

    @Data
    @Builder
    public static class GuaranteeInfo {
        private String itemName;
        private String itemDesc;
    }

    @Data
    @Builder
    public static class TrustInfo {
        private Boolean trustEnabled;
        private String summary;
        private String agreementUrl;
    }
}
