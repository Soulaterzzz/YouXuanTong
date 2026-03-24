package com.zs.ytbx.common.auth;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class SessionUser implements Serializable {

    private Long userId;

    private Long customerId;

    private String username;

    private String customerName;

    private String memberLevel;
}
