package com.fitness.common.aspect;

import com.fitness.common.annotation.RateLimit;
import com.fitness.common.exception.BusinessException;
import com.fitness.common.limiter.RateLimiterManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 限流切面
 * <p>
 * 拦截标注了 {@link RateLimit} 的方法，在方法执行前检查令牌桶，
 * 无可用令牌时抛出 {@link BusinessException}（由全局异常处理器转成统一响应）。
 * <p>
 * 为什么用 AOP 而不是过滤器？
 * <ul>
 *     <li>注解式：粒度精确到方法，可对不同接口设不同阈值</li>
 *     <li>无需改动原有 Controller 代码，符合开闭原则</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiterManager rateLimiterManager;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 构造限流 key
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodKey = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        String bucketKey = methodKey;
        if (rateLimit.perUser()) {
            Long userId = currentUserId();
            bucketKey = methodKey + ":" + (userId == null ? "anonymous" : userId);
        }

        // 尝试取令牌
        boolean allowed = rateLimiterManager.tryAcquire(bucketKey, rateLimit.qps());
        if (!allowed) {
            log.warn("接口限流触发: key={}, qps={}", bucketKey, rateLimit.qps());
            throw new BusinessException(429, rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 从请求上下文取当前登录用户 ID（JwtInterceptor 已写入）
     */
    private Long currentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        Object userId = request.getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
    }
}
