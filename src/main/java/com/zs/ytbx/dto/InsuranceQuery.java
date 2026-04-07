package com.zs.ytbx.dto;

import lombok.Data;
import java.util.Date;

@Data
public class InsuranceQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String plan;
    private String productName;
    private String status;
    private String serialNo;
    private Date startDate;
    private Date endDate;
    private String insuredName;
    private String insuredId;
    private String beneficiaryName;
    private String beneficiaryId;
    private String agent;
}
