package com.zs.ytbx.vo.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductDetailVO {

    private Long productId;

    private String productName;

    private String companyName;

    private String categoryName;

    private String summary;

    private List<String> tags;

    private List<PlanOption> plans;

    private List<GuaranteeItem> guaranteeItems;

    @Data
    @Builder
    public static class PlanOption {
        private Long planId;
        private String planName;
        private String premiumText;
        private String coverageText;
        private String periodText;
        private String renewalText;
    }

    @Data
    @Builder
    public static class GuaranteeItem {
        private String itemName;
        private String itemValue;
        private String itemDesc;
    }
}
