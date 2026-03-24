package com.zs.ytbx.dto;

import lombok.Data;

@Data
public class UserQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String username;
    private String mobile;
    private String userType;
    private String status;
}
