'use strict';

/**
 * 两个进程内小组件，够用且零依赖：
 *  - TtlCache：带过期时间与容量上限的 LRU，防止估价接口被重复调用烧钱
 *  - createRateLimiter：按用户滑窗限流，防止有人脚本刷接口
 *
 * 注意：云函数实例会被回收或水平扩容，进程内缓存只是「尽力而为」。
 * 需要跨实例一致时，把 TtlCache 换成 Redis 即可（接口保持 get/set）。
 */

class TtlCache {
  constructor({ ttlMs = 60000, max = 500 } = {}) {
    this.ttlMs = ttlMs;
    this.max = max;
    this.map = new Map();
  }

  get(key) {
    const entry = this.map.get(key);
    if (!entry) return undefined;
    if (entry.expiresAt <= Date.now()) {
      this.map.delete(key);
      return undefined;
    }
    // LRU：命中后移到队尾
    this.map.delete(key);
    this.map.set(key, entry);
    return entry.value;
  }

  set(key, value) {
    if (this.max <= 0) return;
    this.map.delete(key);
    this.map.set(key, { value, expiresAt: Date.now() + this.ttlMs });
    while (this.map.size > this.max) {
      const oldest = this.map.keys().next().value;
      this.map.delete(oldest);
    }
  }

  clear() {
    this.map.clear();
  }

  get size() {
    return this.map.size;
  }
}

function createRateLimiter({ limit = 20, windowMs = 60000 } = {}) {
  const buckets = new Map();

  function prune(key, now) {
    const hits = buckets.get(key);
    if (!hits) return [];
    const fresh = hits.filter((time) => now - time < windowMs);
    if (fresh.length === 0) buckets.delete(key);
    else buckets.set(key, fresh);
    return fresh;
  }

  return {
    /** 返回 true 表示放行，false 表示超限 */
    take(key, now = Date.now()) {
      if (!key) return true; // 没有身份信息时不做限制，由上游保证
      const hits = prune(key, now);
      if (hits.length >= limit) return false;
      hits.push(now);
      buckets.set(key, hits);
      // 简单防膨胀：桶太多时清理最旧的一批
      if (buckets.size > 5000) {
        const cutoff = now - windowMs;
        for (const [bucketKey, times] of buckets) {
          if (times[times.length - 1] < cutoff) buckets.delete(bucketKey);
        }
      }
      return true;
    },
    reset() {
      buckets.clear();
    },
  };
}

module.exports = { TtlCache, createRateLimiter };
