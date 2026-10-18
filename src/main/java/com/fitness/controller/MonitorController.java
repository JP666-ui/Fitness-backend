package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.common.cache.LocalCache;
import com.fitness.common.limiter.RateLimiterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统监控 Controller
 * <p>
 * 暴露缓存命中率、限流桶数量等运行时指标，便于观察优化效果。
 * <p>
 * 生产环境应加权限控制或接入 Prometheus，此处为演示用途。
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Autowired
    private LocalCache localCache;

    @Autowired
    private RateLimiterManager rateLimiterManager;

    /**
     * 缓存统计
     * GET /monitor/cache
     */
    @GetMapping("/cache")
    public Result<Map<String, Object>> cacheStats() {
        return Result.success(localCache.stats());
    }

    /**
     * 限流器统计
     * GET /monitor/limiter
     */
    @GetMapping("/limiter")
    public Result<Map<String, Object>> limiterStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bucketCount", rateLimiterManager.bucketCount());
        data.put("strategy", "TokenBucket");
        return Result.success(data);
    }

    /**
     * 综合健康信息
     * GET /monitor/health
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Runtime rt = Runtime.getRuntime();
        long totalMb = rt.totalMemory() / 1024 / 1024;
        long freeMb = rt.freeMemory() / 1024 / 1024;
        long usedMb = totalMb - freeMb;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("jvmTotalMb", totalMb);
        data.put("jvmUsedMb", usedMb);
        data.put("jvmFreeMb", freeMb);
        data.put("processors", rt.availableProcessors());
        data.put("cache", localCache.stats());
        data.put("limiterBuckets", rateLimiterManager.bucketCount());
        return Result.success(data);
    }
}
