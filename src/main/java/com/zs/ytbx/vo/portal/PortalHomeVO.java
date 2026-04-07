package com.zs.ytbx.vo.portal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PortalHomeVO {

    private Hero hero;

    private List<CategoryCard> categories;

    private List<RecommendedProduct> recommendedProducts;

    private List<ClaimStep> claimSteps;

    @Data
    @Builder
    public static class Hero {
        private String title;
        private String subtitle;
        private String primaryAction;
        private String secondaryAction;
    }

    @Data
    @Builder
    public static class CategoryCard {
        private Long categoryId;
        private String categoryName;
        private String summary;
        private String productCountText;
        private String actionText;
    }

    @Data
    @Builder
    public static class RecommendedProduct {
        private Long productId;
        private Long planId;
        private String productName;
        private String sellingPoint;
        private String premiumText;
        private List<String> tags;
    }

    @Data
    @Builder
    public static class ClaimStep {
        private Integer sortNo;
        private String stepName;
        private String summary;
    }
}
