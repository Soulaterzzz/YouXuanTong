package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ProductDetailVO {
    private Long id;
    private String productCode;
    private String name;
    private String description;
    private String features;
    private String detailText;
    private BigDecimal price;
    private BigDecimal displayPrice;
    private boolean isNew;
    private String categoryCode;
    private String companyName;
    private String alias;
}
