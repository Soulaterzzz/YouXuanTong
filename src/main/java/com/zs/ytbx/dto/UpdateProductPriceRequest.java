package com.zs.ytbx.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProductPriceRequest {

    @NotNull(message = "产品ID不能为空")
    private Long id;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private Double price;
}
