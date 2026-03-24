package com.zs.ytbx.vo.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCategoryVO {

    private Long categoryId;

    private String categoryCode;

    private String categoryName;
}
