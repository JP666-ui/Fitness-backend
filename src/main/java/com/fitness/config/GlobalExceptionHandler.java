package com.fitness.config;

import com.fitness.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * <p>
 * 保证任何异常都返回统一的 JSON 结构，前端不会拿到 HTML 错误页。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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