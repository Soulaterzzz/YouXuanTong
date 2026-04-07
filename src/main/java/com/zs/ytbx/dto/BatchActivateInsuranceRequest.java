package com.zs.ytbx.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BatchActivateInsuranceRequest {

    @Valid
    @NotEmpty(message = "保单列表不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "保单ID不能为空")
        private Long insuranceId;

        @NotBlank(message = "保单号不能为空")
        private String policyNo;

        @NotNull(message = "起保日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate effectiveDate;

        @NotNull(message = "结束日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expiryDate;
    }
}
