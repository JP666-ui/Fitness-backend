package com.fitness.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * 本地缓存（LRU + TTL）
 * <p>
 * 设计要点：
 * <ul>
 *     <li><b>LRU 淘汰</b>：基于 LinkedHashMap 的 accessOrder=true，
 *         访问过的元素移到队尾，容量超限时淘汰队首（最久未使用）</li>
 *     <li><b>TTL 过期</b>：每个 value 包装一个过期时间戳，读取时惰性判断</li>
 *     <li><b>读写锁</b>：读多写少场景用 ReentrantReadWriteLock，
 *         读操作可并发，写操作互斥，比 synchronized 吞吐更高</li>
 *     <li><b>防缓存击穿</b>：load 方法内部加锁，避免同一 key 并发回源</li>
 * </ul>
 * <p>
 * 为什么不用 Redis？
 * <p>
 * 食物库这类数据「读多写少、总量小（151 条）、允许短暂不一致」，
 * 放在进程内可省去一次网络往返（本地访问约 100ns，Redis 约 0.5-1ms），
 * 单机部署场景下性价比更高。若要多实例共享缓存，则应换成 Redis。
 */
@Slf4j
@Component
public class LocalCache {

    /** 默认最大条目数，防止内存无限增长 */
    private static final int DEFAULT_MAX_SIZE = 512;

    /** 默认过期时间：10 分钟 */
    private static final long DEFAULT_TTL_MILLIS = 10 * 60 * 1000L;

    private final int maxSize;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * LRU 容器：accessOrder=true 保证按访问顺序排序
     */
    private final LinkedHashMap<String, CacheEntry> store;

    /** 命中/未命中计数（监控用） */
    private long hitCount = 0;
    private long missCount = 0;

    public LocalCache() {
        this(DEFAULT_MAX_SIZE);
    }

    public LocalCache(int maxSize) {
        this.maxSize = maxSize;
        this.store = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                boolean remove = size() > LocalCache.this.maxSize;
                if (remove) {
                    log.debug("本地缓存触发 LRU 淘汰: key={}", eldest.getKey());
                }
                return remove;
            }
        };
    }

    /**
     * 读取缓存；不存在或已过期返回 null
     */
    public Object get(String key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = store.get(key);
            if (entry == null) {
                missCount++;
                return null;
            }
            if (entry.isExpired()) {
                missCount++;
                return null;         // 惰性过期：读取时才发现，由调用方回源覆盖
            }
            hitCount++;
            return entry.value;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 写入缓存（使用默认 TTL）
     */
    public void put(String key, Object value) {
        put(key, value, DEFAULT_TTL_MILLIS);
    }

    /**
     * 写入缓存
     *
     * @param ttlMillis 过期时间（毫秒），&lt;=0 表示永不过期
     */
    public void put(String key, Object value, long ttlMillis) {
        if (key == null || value == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            store.put(key, new CacheEntry(value, ttlMillis));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 读取缓存，未命中时用 loader 回源并写入
     * <p>
     * 双重检查 + 写锁：避免高并发下同一 key 重复回源（缓存击穿）
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, long ttlMillis, Supplier<T> loader) {
        // 第一次检查（无锁，快路径）
        Object cached = get(key);
        if (cached != null) {
            return (T) cached;
        }

        // 回源时加写锁，保证同一 key 只有一个线程真正查库
        lock.writeLock().lock();
        try {
            // 第二次检查：可能已被其他线程写入
            CacheEntry entry = store.get(key);
            if (entry != null && !entry.isExpired()) {
                hitCount++;
                return (T) entry.value;
            }

            T loaded = loader.get();
            if (loaded != null) {
                store.put(key, new CacheEntry(loaded, ttlMillis));
            }
            return loaded;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 删除指定 key
     */
    public void evict(String key) {
        lock.writeLock().lock();
        try {
            store.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 按前缀批量删除（用于数据变更后清理相关缓存）
     */
    public int evictByPrefix(String prefix) {
        lock.writeLock().lock();
        try {
            int removed = 0;
            Iterator<String> it = store.keySet().iterator();
            while (it.hasNext()) {
                if (it.next().startsWith(prefix)) {
                    it.remove();
                    removed++;
                }
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空全部缓存
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            store.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 缓存统计信息（监控接口用）
     */
    public Map<String, Object> stats() {
        lock.readLock().lock();
        try {
            long total = hitCount + missCount;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("size", store.size());
            m.put("maxSize", maxSize);
            m.put("hitCount", hitCount);
            m.put("missCount", missCount);
            m.put("hitRate", total == 0 ? 0.0
                    : Math.round(hitCount * 10000.0 / total) / 100.0);
            return m;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 缓存条目：值 + 过期时间戳
     */
    private static final class CacheEntry {
        final Object value;
        /** 过期时间戳（毫秒）；0 表示永不过期 */
        final long expireAt;

        CacheEntry(Object value, long ttlMillis) {
            this.value = value;
            this.expireAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0;
        }

        boolean isExpired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }
}
