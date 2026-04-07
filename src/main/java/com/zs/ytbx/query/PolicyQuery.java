package com.zs.ytbx.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PolicyQuery {

    private String keyword;

    private String policyStatus;

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 50, message = "每页条数不能超过50")
    private Integer pageSize = 10;
}
