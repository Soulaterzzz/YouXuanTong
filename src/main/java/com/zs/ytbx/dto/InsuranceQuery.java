package com.zs.ytbx.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class InsuranceQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String plan;
    private String productName;
    private String status;
    private String serialNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDate;
    private String insuredName;
    private String insuredId;
    private String beneficiaryName;
    private String beneficiaryId;
    private String agent;
}
