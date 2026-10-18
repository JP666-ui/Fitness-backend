package com.fitness.config;

import com.fitness.common.Result;
import com.fitness.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * <p>
 * 保证任何异常都返回统一的 JSON 结构，前端不会拿到 HTML 错误页。
 * <p>
 * 异常处理优先级：Spring 会优先匹配「最具体的异常类型」，
 * 因此 BusinessException 会先于 Exception 被捕获。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（可预期，不打印堆栈）
     * <p>
     * 涵盖限流、幂等拦截、参数非法等业务规则校验失败场景。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败（@Valid 触发）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<String> handleValidationException(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex) {
            FieldError fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null) {
                msg = fieldError.getDefaultMessage();
            }
        } else if (e instanceof BindException ex) {
            FieldError fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null) {
                msg = fieldError.getDefaultMessage();
            }
        }
        log.warn("参数校验失败: {}", msg);
        return Result.fail(400, msg);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<String> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(400, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("请求处理异常: {}", e.getMessage(), e);

        String msg = e.getMessage();
        // 数据库字段/语法错误给出可读提示
        if (msg != null && msg.contains("Unknown column")) {
            return Result.fail(500, "数据库字段不匹配，请检查表结构：" + extractColumn(msg));
        }
        if (msg != null && msg.contains("doesn't exist")) {
            return Result.fail(500, "数据表不存在，请先执行数据库初始化脚本");
        }
        if (msg != null && msg.contains("BadSqlGrammar")) {
            return Result.fail(500, "数据库查询错误，请检查表结构");
        }

        return Result.fail(500, "服务器内部错误：" + (msg == null ? "未知异常" : msg));
    }

    private String extractColumn(String msg) {
        int start = msg.indexOf("'");
        int end = msg.indexOf("'", start + 1);
        if (start >= 0 && end > start) {
            return msg.substring(start + 1, end);
        }
        return "";
    }
}
