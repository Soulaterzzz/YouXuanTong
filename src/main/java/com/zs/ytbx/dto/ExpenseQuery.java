package com.zs.ytbx.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ExpenseQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String plan;
    private String status;
    private String serialNo;
    private Date startDate;
    private Date endDate;
}
