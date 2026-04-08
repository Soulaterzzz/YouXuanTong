package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductVO {
    private Long id;
    private String productCode;
    private String name;
    private String description;
    private String features;
    private BigDecimal price;
    private boolean isNew;
    private boolean isHot;
    private String categoryCode;
    private String companyName;
    private String saleStatus;
    private Integer sortNo;
    private String imageUrl;
    private String templateFileName;
    private String alias;
}
