package com.zs.ytbx.dto;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    private Long id;
    
    @NotBlank(message = "产品编码不能为空")
    private String productCode;
    
    @NotBlank(message = "产品名称不能为空")
    private String productName;
    
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;
    
    private String companyName;
    
    private String description;
    
    private String features;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    private Integer stock = 0;
    
    private Integer isNew = 0;
    
    private Integer isHot = 0;
    
    private String saleStatus = "ON_SALE";
    
    private Integer sortNo = 0;

    private String alias;
}
