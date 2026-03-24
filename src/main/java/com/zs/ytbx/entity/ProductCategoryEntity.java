package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_product_category")
public class ProductCategoryEntity extends BaseEntity {

    private String categoryCode;

    private String categoryName;

    private Long parentId;

    private Integer sortNo;

    private String categoryStatus;
}
