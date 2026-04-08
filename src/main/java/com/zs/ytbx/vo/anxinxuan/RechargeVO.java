package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class RechargeVO {
    private Long id;
    private String username;
    private String date;
    private BigDecimal amount;
    private String type;
    private String description;
    private String serial;
    private BigDecimal balance;
}
