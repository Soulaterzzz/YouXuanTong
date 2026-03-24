package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_product_guarantee_item")
public class ProductGuaranteeItemEntity extends BaseEntity {

    private Long planId;

    private String itemType;

    private String itemName;

    private String itemValue;

    private String itemDesc;

    private Integer sortNo;
}
