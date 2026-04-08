package com.zs.ytbx.dto;

import lombok.Data;
import java.util.Date;

@Data
public class RechargeQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String username;
    private Date date;
    private String type;
    private String description;
}
