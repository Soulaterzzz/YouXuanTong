package com.zs.ytbx.dto;

import lombok.Data;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class UpdateUserRequest {
    private Long id;

    @Size(min = 3, max = 32, message = "用户名长度必须在3-32位之间")
    private String username;

    @Size(min = 6, max = 32, message = "密码长度必须在6-32位之间")
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    private String status;
}
