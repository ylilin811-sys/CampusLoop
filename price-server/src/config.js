'use strict';

/**
 * 全部配置来自环境变量，方便在微信云函数、Docker、本机之间迁移。
 * 未配置 DEEPSEEK_API_KEY 时服务不会崩溃：会退化成「规则估算」并标记 source=fallback。
 */

function positiveInt(value, fallback) {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : fallback;
}

const config = {
  // 模型凭据
  apiKey: process.env.DEEPSEEK_API_KEY || '',
  baseUrl: (process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com').replace(/\/+$/, ''),
  model: process.env.DEEPSEEK_MODEL || 'deepseek-chat',

  // 请求行为
  timeoutMs: positiveInt(process.env.PRICE_AI_TIMEOUT_MS, 20000),
  retries: Number.isFinite(Number(process.env.PRICE_AI_RETRIES))
    ? Math.max(0, Math.floor(Number(process.env.PRICE_AI_RETRIES)))
    : 1,

  // 缓存（进程内，云函数实例复用期间有效）
  cacheTtlMs: positiveInt(process.env.PRICE_AI_CACHE_TTL_MS, 60 * 1000),
  cacheMax: positiveInt(process.env.PRICE_AI_CACHE_MAX, 500),

  // 限流：每个用户每分钟请求数
  rateLimit: positiveInt(process.env.PRICE_AI_RATE_LIMIT, 20),
  rateWindowMs: positiveInt(process.env.PRICE_AI_RATE_WINDOW_MS, 60 * 1000),

  // HTTP 入口
  port: positiveInt(process.env.PORT, 8787),
  maxBodyBytes: positiveInt(process.env.PRICE_AI_MAX_BODY_BYTES, 64 * 1024),
};

module.exports = config;
