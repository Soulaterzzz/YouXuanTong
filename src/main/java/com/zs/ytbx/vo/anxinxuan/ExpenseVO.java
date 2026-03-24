package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ExpenseVO {
    private Long id;
    private String serial;
    private String contact;
    private String product;
    private String createTime;
    private String exportTime;
    private String status;
    private String policyNo;
    private String startDate;
    private String endDate;
    private Integer count;
    private BigDecimal price;
    private BigDecimal total;
}
