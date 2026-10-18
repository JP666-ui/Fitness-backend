package com.fitness.common.limiter;

/**
 * 令牌桶限流器
 * <p>
 * 令牌桶算法：桶以固定速率（qps）生成令牌，每个请求消耗一个令牌；
 * 桶满后新令牌丢弃，桶空时请求被拒绝。
 * <p>
 * 相比固定窗口计数器，令牌桶能容忍短时突发（桶内积累的令牌），
 * 同时保证长期平均速率不超过设定值。
 * <p>
 * 实现要点：
 * <ul>
 *     <li>惰性补充令牌：不在后台起定时任务，而是在每次请求时按时间差计算应补数量，
 *         避免无请求时白白占用 CPU</li>
 *     <li>使用 synchronized 保证多线程下「计算 → 判断 → 扣减」的原子性</li>
 *     <li>只存 double 型令牌数 + 上次填充时间戳，内存占用极小</li>
 * </ul>
 */
public class TokenBucket {

    /** 桶容量（= 每秒令牌数，允许短时突发到该值） */
    private final double capacity;

    /** 每秒生成的令牌数 */
    private final double refillRate;

    /** 当前可用令牌数 */
    private double tokens;

    /** 上次补充令牌的时间戳（纳秒） */
    private long lastRefillNanos;

    public TokenBucket(double qps) {
        if (qps <= 0) {
            throw new IllegalArgumentException("qps 必须大于 0");
        }
        this.capacity = qps;
        this.refillRate = qps;
        this.tokens = qps;                       // 初始满桶，允许首次突发
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * 尝试获取一个令牌
     *
     * @return true 表示放行，false 表示被限流
     */
    public synchronized boolean tryAcquire() {
        refill();

        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    /**
     * 按经过的时间补充令牌（惰性填充）
     */
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillNanos;

        // 不足 1 毫秒不计算，减少浮点运算
        if (elapsedNanos < 1_000_000L) {
            return;
        }

        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double newTokens = elapsedSeconds * refillRate;

        if (newTokens > 0) {
            tokens = Math.min(capacity, tokens + newTokens);
            lastRefillNanos = now;
        }
    }

    /**
     * 当前可用令牌数（仅用于监控/调试）
     */
    public synchronized double availableTokens() {
        refill();
        return tokens;
    }
}
