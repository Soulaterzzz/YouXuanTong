package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "axx_user", excludeProperty = {"createBy", "updateBy"})
public class AxxUserEntity extends BaseEntity {

    private String username;

    private String password;

    private String mobile;

    private String userType;

    private String status;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;
}
