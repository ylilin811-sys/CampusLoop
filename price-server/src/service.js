'use strict';

/**
 * 估价编排：
 *   校验输入 → 查缓存 → 统计锚点 → 调模型（带重试）→ 校验输出 → 兜底 → 回填 basis
 *
 * 对外两个出口：
 *   createPriceService({ config, deps })  便于测试注入假的 chat
 *   estimatePrice(raw, options)           默认单例，直接给 HTTP / 云函数用
 */

const crypto = require('node:crypto');
const defaultConfig = require('./config');
const { parseInput } = require('./input');
const { summarize, ruleBasedEstimate } = require('./stats');
const { buildMessages } = require('./prompt');
const { chat, ModelError } = require('./model');
const { validateModelOutput } = require('./validate');
const { TtlCache } = require('./cache');

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function cacheKey(input) {
  const canonical = JSON.stringify({
    t: input.title,
    d: input.desc,
    c: input.condition,
    k: input.category,
    o: input.originalPrice,
    u: input.daysUsed,
    g: input.groupSize,
    m: input.mode,
    s: input.comparables,
  });
  return crypto.createHash('sha256').update(canonical).digest('hex');
}

function pickComparables(input, suggested, limit = 5) {
  return input.comparables
    .slice()
    .sort((a, b) => Math.abs(a.price - suggested) - Math.abs(b.price - suggested))
    .slice(0, limit)
    .map((item) => ({
      title: item.title,
      price: item.price,
      condition: item.condition || '',
      status: item.status,
    }));
}

function fillReasons(value, input, stats) {
  if (value.reasons && value.reasons.length > 0) return value.reasons;
  if (stats.sampleCount > 0) {
    return [`同款样本 ${stats.sampleCount} 条，中位数 ${stats.median} 元`];
  }
  return [`参考原价 ${input.originalPrice} 元与成色「${input.condition || '未标注'}」折算`];
}

function finalize(value, input, stats, source) {
  return {
    low: value.low,
    high: value.high,
    suggested: value.suggested,
    confidence: value.confidence,
    reasons: fillReasons(value, input, stats),
    risk: value.risk || '二手交易有风险，建议当面验货',
    comparables: pickComparables(input, value.suggested),
    basis: {
      sampleCount: stats.sampleCount,
      soldCount: stats.soldCount,
      median: stats.median,
      p25: stats.p25,
      p75: stats.p75,
    },
    source,
  };
}

function createPriceService({ config = defaultConfig, deps = {} } = {}) {
  const cache = new TtlCache({ ttlMs: config.cacheTtlMs, max: config.cacheMax });
  const callModel = deps.chat || chat;
  const now = deps.now || (() => new Date());

  async function estimate(rawInput) {
    const startedAt = Date.now();

    const parsed = parseInput(rawInput);
    if (!parsed.ok) {
      return {
        ok: false,
        code: parsed.code,
        field: parsed.field,
        message: parsed.message,
      };
    }
    const input = parsed.value;

    const key = cacheKey(input);
    const cached = cache.get(key);
    if (cached) {
      return {
        ok: true,
        data: cached,
        meta: {
          cached: true,
          source: cached.source,
          model: config.model,
          latencyMs: Date.now() - startedAt,
        },
      };
    }

    const stats = summarize(input);
    const messages = buildMessages(input, stats, now());

    const attempts = config.retries + 1;
    let lastError = null;
    let usedAttempts = 0;

    for (let attempt = 0; attempt < attempts; attempt += 1) {
      if (attempt > 0) await sleep(300 * 2 ** (attempt - 1));
      usedAttempts = attempt + 1;
      try {
        const text = await callModel({ messages, config });
        const value = validateModelOutput(text, input);
        if (!value) {
          lastError = new ModelError('BAD_OUTPUT', '模型输出不是合法 JSON 或字段缺失', {
            retryable: true,
          });
          continue;
        }
        const data = finalize(value, input, stats, 'model');
        cache.set(key, data);
        return {
          ok: true,
          data,
          meta: {
            cached: false,
            source: 'model',
            model: config.model,
            latencyMs: Date.now() - startedAt,
            attempts: usedAttempts,
          },
        };
      } catch (error) {
        lastError = error;
        if (!(error instanceof ModelError) || !error.retryable) break;
      }
    }

    // 模型彻底不可用：退到规则估算，保证接口可用
    const fallback = ruleBasedEstimate(input);
    if (fallback) {
      return {
        ok: true,
        data: finalize(fallback, input, stats, 'fallback'),
        meta: {
          cached: false,
          source: 'fallback',
          model: config.model,
          latencyMs: Date.now() - startedAt,
          attempts: usedAttempts,
          degraded: true,
          reason: lastError ? lastError.code : 'UNKNOWN',
        },
      };
    }

    return {
      ok: false,
      code: (lastError && lastError.code) || 'UPSTREAM_FAILED',
      message: (lastError && lastError.message) || '估价失败，请稍后重试',
    };
  }

  return { estimate, cache };
}

const defaultService = createPriceService();

function estimatePrice(rawInput) {
  return defaultService.estimate(rawInput);
}

module.exports = { createPriceService, estimatePrice, defaultService, cacheKey };
