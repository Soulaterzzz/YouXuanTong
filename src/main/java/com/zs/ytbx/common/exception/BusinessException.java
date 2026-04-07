package com.zs.ytbx.common.exception;

import com.zs.ytbx.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;
    private final String businessMessage;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.businessMessage = resultCode.getMessage();
    }

    public BusinessException(ResultCode resultCode, String businessMessage) {
        super(businessMessage);
        this.resultCode = resultCode;
        this.businessMessage = businessMessage;
    }
}
