package com.zs.ytbx.common.enums;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS("00000", "success"),
    INVALID_PARAM("A0400", "请求参数错误"),
    UNAUTHORIZED("A0401", "未登录或登录失效"),
    FORBIDDEN("A0403", "无权限访问"),
    NOT_FOUND("A0404", "资源不存在"),
    CONFLICT("A0409", "数据冲突"),
    LOGIN_FAILED("A0410", "用户名或密码错误"),
    PRODUCT_NOT_FOUND("B1001", "产品不存在或已下架"),
    PRODUCT_COMPARE_UNSUPPORTED("B1002", "产品不支持对比"),
    POLICY_NOT_FOUND("B2001", "保单不存在"),
    POLICY_ELECTRONIC_FILE_NOT_FOUND("B2003", "电子保单不存在"),
    CLAIM_NOT_FOUND("B3001", "理赔案件不存在"),
    ADVISOR_NOT_AVAILABLE("B4001", "顾问不可预约"),
    SYSTEM_ERROR("C5000", "系统内部错误");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
