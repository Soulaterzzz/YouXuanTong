package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class InsuranceVO {
    private Long id;
    private String product;
    private String insuredName;
    private String insuredId;
    private String beneficiaryName;
    private String beneficiaryId;
    private String createTime;
    private String exportTime;
    private String status;
    private String agent;
    private String policyNo;
    private String startDate;
    private String endDate;
    private Integer count;
    private BigDecimal premiumAmount;
}
