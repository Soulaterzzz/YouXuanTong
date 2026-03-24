package com.zs.ytbx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String username;
    private String mobile;
    private String userType;
    private String status;
    private BigDecimal balance;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
