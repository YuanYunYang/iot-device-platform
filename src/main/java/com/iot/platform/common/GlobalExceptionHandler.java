package com.iot.platform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 拦截 Controller 层抛出的异常，统一转为 Result 结构返回，
 * 避免异常堆栈直接暴露给前端，同时记录错误日志便于排查。
 *
 * @author iot-platform
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.failed(ResultCode.FAILED, e.getMessage());
    }

    /**
     * 参数校验异常（@RequestBody @Valid 触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.failed(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return Result.failed(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 请求资源不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("请求资源不存在: {}", e.getRequestURL());
        return Result.failed(ResultCode.NOT_FOUND);
    }

    /**
     * 非法参数
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.failed(ResultCode.PARAM_ERROR, e.getMessage());
    }

    /**
     * 其他未捕获异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.failed(ResultCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
    }

    /**
     * 业务异常内部类
     * <p>
     * Service 层可抛出该异常，由全局处理器统一捕获。
     */
    public static class BusinessException extends RuntimeException {

        private final int code;

        public BusinessException(String message) {
            this(ResultCode.FAILED.getCode(), message);
        }

        public BusinessException(ResultCode resultCode) {
            this(resultCode.getCode(), resultCode.getMessage());
        }

        /**
         * 指定状态码与自定义提示信息构造业务异常
         *
         * @param resultCode 状态码枚举
         * @param message    自定义提示信息
         */
        public BusinessException(ResultCode resultCode, String message) {
            super(message);
            this.code = resultCode.getCode();
        }

        public BusinessException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
