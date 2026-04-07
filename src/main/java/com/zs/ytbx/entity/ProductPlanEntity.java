package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_product_plan")
public class ProductPlanEntity extends BaseEntity {

    private Long productId;

    private String planCode;

    private String planName;

    private BigDecimal premiumStartAmount;

    private BigDecimal coverageAmount;

    private String coveragePeriodDesc;

    private String waitingPeriodDesc;

    private String renewalDesc;

    private String highlightTags;

    private Integer sortNo;

    private String planStatus;
}
