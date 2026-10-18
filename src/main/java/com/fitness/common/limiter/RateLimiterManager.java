package com.fitness.common.limiter;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器管理器
 * <p>
 * 为每个「限流维度」维护独立的令牌桶：
 * <ul>
 *     <li>全局限流：key = 方法签名</li>
 *     <li>按用户限流：key = 方法签名 + ":" + userId</li>
 * </ul>
 * <p>
 * 使用 ConcurrentHashMap 保证线程安全；computeIfAbsent 保证同一个 key
 * 只创建一个桶（CAS 实现，避免重复创建）。
 */
@Component
public class RateLimiterManager {

    /**
     * key -> 令牌桶
     */
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * 获取（或创建）指定维度的令牌桶
     *
     * @param key 限流维度标识
     * @param qps 每秒令牌数
     */
    public TokenBucket getBucket(String key, double qps) {
        return buckets.computeIfAbsent(key, k -> new TokenBucket(qps));
    }

    /**
     * 尝试获取令牌
     *
     * @param key 限流维度标识
     * @param qps 每秒令牌数
     * @return true 放行 / false 限流
     */
    public boolean tryAcquire(String key, double qps) {
        return getBucket(key, qps).tryAcquire();
    }

    /**
     * 当前维护的桶数量（监控用）
     */
    public int bucketCount() {
        return buckets.size();
    }
}
