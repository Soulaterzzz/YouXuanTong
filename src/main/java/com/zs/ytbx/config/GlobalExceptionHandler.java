package com.zs.ytbx.config;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.<Void>builder()
                .code(exception.getResultCode().getCode())
                .message(exception.getBusinessMessage())
                .traceId(traceId())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.<String>builder()
                .code(ResultCode.INVALID_PARAM.getCode())
                .message(ResultCode.INVALID_PARAM.getMessage())
                .data(message)
                .traceId(traceId())
                .build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<String> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = ResultCode.INVALID_PARAM.getMessage();
        }
        return ApiResponse.<String>builder()
                .code(ResultCode.INVALID_PARAM.getCode())
                .message(ResultCode.INVALID_PARAM.getMessage())
                .data(message)
                .traceId(traceId())
                .build();
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception exception) {
        String traceId = traceId();
        log.error("Unhandled exception, traceId={}", traceId, exception);
        return ApiResponse.<String>builder()
                .code(ResultCode.SYSTEM_ERROR.getCode())
                .message(ResultCode.SYSTEM_ERROR.getMessage())
                .traceId(traceId)
                .build();
    }

    private String traceId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}
