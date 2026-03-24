package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("axx_product")
public class AxxProductEntity extends BaseEntity {

    private String productCode;

    private String productName;

    private String categoryCode;

    private String companyName;

    private String description;

    private String features;

    private BigDecimal price;

    private Integer stock;

    private Integer isNew;

    private Integer isHot;

    private String saleStatus;

    private Integer sortNo;

    private String imageUrl;

    private String imagePath;

    private String imageContentType;

    private Long imageSize;
}
