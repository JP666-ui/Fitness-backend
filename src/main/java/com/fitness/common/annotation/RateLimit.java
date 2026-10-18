package com.fitness.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * <p>
 * 基于令牌桶算法，标记在 Controller 方法上即可限制该接口的 QPS。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;RateLimit(qps = 5, message = "请求过于频繁，请稍后再试")
 * &#64;PostMapping("/chat")
 * public Result&lt;?&gt; chat(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 每秒允许的请求数（令牌桶容量）
     */
    double qps() default 10.0;

    /**
     * 是否按用户维度限流
     * <p>
     * true  —— 每个用户独立计数（需登录，从 request 取 userId）
     * false —— 全局限流（所有用户共享一个桶）
     */
    boolean perUser() default false;

    /**
     * 触发限流时的提示语
     */
    String message() default "请求过于频繁，请稍后再试";
}
