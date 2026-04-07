package com.zs.ytbx.vo.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthUserVO {

    private Long userId;

    private Long customerId;

    private String username;

    private String customerName;

    private String memberLevel;
}
