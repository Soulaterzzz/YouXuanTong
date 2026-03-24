package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_insurance_product")
public class InsuranceProductEntity extends BaseEntity {

    private String productCode;

    private String productName;

    private Long categoryId;

    private Long companyId;

    private String productType;

    private String targetPeople;

    private String saleStatus;

    private Integer supportCompare;

    private Integer recommendFlag;

    private String summary;

    private String detailContent;

    private String bannerImageUrl;

    private Integer sortNo;
}
