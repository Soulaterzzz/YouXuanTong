package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user")
public class SysUserEntity extends BaseEntity {

    private String username;

    private String passwordHash;

    private String userType;

    private String mobile;

    private String email;

    private String status;
}
