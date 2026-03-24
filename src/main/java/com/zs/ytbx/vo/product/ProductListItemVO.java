package com.zs.ytbx.vo.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductListItemVO {

    private Long productId;

    private Long planId;

    private String productName;

    private String companyName;

    private String categoryName;

    private String summary;

    private String premiumText;

    private String coverageText;

    private List<String> tags;

    private Boolean supportCompare;
}
