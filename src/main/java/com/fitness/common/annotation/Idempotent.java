package com.fitness.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解
 * <p>
 * 在指定时间窗口内，同一用户对同一接口的重复请求会被拦截，
 * 用于防止用户连点、网络重试导致的重复数据。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Idempotent(seconds = 3, message = "请勿重复提交")
 * &#64;PostMapping("/add")
 * public Result&lt;?&gt; add(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等窗口（秒）：该时间内相同请求只允许成功一次
     */
    int seconds() default 3;

    /**
     * 触发时的提示语
     */
    String message() default "操作过于频繁，请勿重复提交";
}
