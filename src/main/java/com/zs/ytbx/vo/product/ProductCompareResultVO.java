package com.zs.ytbx.vo.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductCompareResultVO {

    private Integer selectedCount;

    private Long recommendedPlanId;

    private List<ComparedPlan> items;

    private List<CompareSection> compareSections;

    @Data
    @Builder
    public static class ComparedPlan {
        private Long planId;
        private String productName;
        private String companyName;
        private String premiumText;
        private String recommendationTag;
    }

    @Data
    @Builder
    public static class CompareSection {
        private String sectionName;
        private List<CompareField> fields;
    }

    @Data
    @Builder
    public static class CompareField {
        private String fieldName;
        private List<String> fieldValues;
    }
}
