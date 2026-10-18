package com.fitness.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 用于表达「可预期的业务错误」（如限流、参数非法、资源不存在），
 * 由全局异常处理器捕获后转成统一响应格式，不打印堆栈。
 * <p>
 * 与 RuntimeException 的区别：业务异常是流程的一部分，不是 bug。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
