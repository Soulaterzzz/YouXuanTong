package com.zs.ytbx.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CompareProductsRequest {

    @NotEmpty(message = "对比计划不能为空")
    @Size(min = 2, max = 4, message = "对比计划数量必须在2到4之间")
    private List<Long> productPlanIds;
}
