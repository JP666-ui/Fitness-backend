package com.fitness.common.aspect;

import com.fitness.common.annotation.Idempotent;
import com.fitness.common.cache.LocalCache;
import com.fitness.common.exception.BusinessException;
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
 * 幂等切面（防重复提交）
 * <p>
 * 实现思路：
 * <ol>
 *     <li>用「用户ID + 方法签名 + 请求参数」生成唯一 key</li>
 *     <li>检查缓存中是否已存在该 key，存在则说明是重复提交，直接拒绝</li>
 *     <li>不存在则写入 key（TTL = 注解指定的窗口时间），然后执行业务</li>
 * </ol>
 * <p>
 * 为什么用「用户ID + 方法 + 参数」而不是仅用方法？
 * <p>
 * 仅用方法会误伤：A 用户提交后，B 用户在同一秒提交也会被拦。
 * 加入参数后，只有「同一用户、同一接口、同一份数据」才算重复。
 * <p>
 * 局限：单机有效。多实例部署时需换成 Redis 的 SETNX 实现。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final String CACHE_PREFIX = "idem:";

    private final LocalCache localCache;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildKey(joinPoint);

        // 已存在 => 窗口内重复提交
        if (localCache.get(key) != null) {
            log.warn("拦截重复提交: {}", key);
            throw new BusinessException(429, idempotent.message());
        }

        // 先占位，再执行业务（TTL 即为幂等窗口）
        localCache.put(key, Boolean.TRUE, idempotent.seconds() * 1000L);

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 业务失败时释放占位，允许用户立即重试（避免失败后要等窗口结束）
            localCache.evict(key);
            throw e;
        }
    }

    /**
     * 构造幂等 key：前缀 + userId + 类名.方法名 + 参数hash
     */
    private String buildKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodKey = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        Long userId = currentUserId();
        String userPart = userId == null ? "anon" : String.valueOf(userId);

        // 参数参与 hash：同参数才视为重复
        Object[] args = joinPoint.getArgs();
        int argsHash = 0;
        if (args != null) {
            for (Object arg : args) {
                // 跳过 request/response 等容器对象
                if (arg == null || arg instanceof HttpServletRequest) {
                    continue;
                }
                argsHash = 31 * argsHash + String.valueOf(arg).hashCode();
            }
        }

        return CACHE_PREFIX + userPart + ":" + methodKey + ":" + argsHash;
    }

    private Long currentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object userId = attrs.getRequest().getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
    }
}
