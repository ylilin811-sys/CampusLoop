'use strict';

/**
 * 统计与规则兜底。
 *
 * 思路：模型不该直接「凭感觉」报价。先用同款样本算出中位数 / 分位数作为锚点，
 * 模型只负责在锚点附近做修正并给出理由。模型不可用时，规则估算仍能返回一个
 * 宽区间，保证接口不 500。
 */

const CONDITION_FACTOR = {
  全新: 0.8,
  '99新': 0.72,
  '95新': 0.62,
  '9成新': 0.55,
  '8成新': 0.45,
  '7成新': 0.35,
  '6成新及以下': 0.28,
};

function round(value) {
  return Math.max(1, Math.round(value));
}

function quantile(sorted, q) {
  if (sorted.length === 0) return null;
  if (sorted.length === 1) return sorted[0];
  const pos = (sorted.length - 1) * q;
  const base = Math.floor(pos);
  const rest = pos - base;
  const next = sorted[base + 1];
  return next === undefined ? sorted[base] : sorted[base] + rest * (next - sorted[base]);
}

function median(sorted) {
  return quantile(sorted, 0.5);
}

function summarize(input) {
  const prices = input.comparables.map((item) => item.price).sort((a, b) => a - b);
  const sold = input.comparables.filter((item) => item.status === 'sold').length;
  return {
    sampleCount: prices.length,
    soldCount: sold,
    median: prices.length ? round(median(prices)) : null,
    p25: prices.length >= 4 ? round(quantile(prices, 0.25)) : null,
    p75: prices.length >= 4 ? round(quantile(prices, 0.75)) : null,
    min: prices.length ? round(prices[0]) : null,
    max: prices.length ? round(prices[prices.length - 1]) : null,
  };
}

function conditionFactor(condition) {
  if (!condition) return 0.5;
  if (CONDITION_FACTOR[condition]) return CONDITION_FACTOR[condition];
  // 自由文本里再找一次「几成新」
  const match = condition.match(/(\d)\s*成/);
  if (match) {
    const level = Number(match[1]);
    if (level >= 9.5) return 0.55;
    if (level >= 9) return 0.5;
    if (level >= 8) return 0.42;
    if (level >= 7) return 0.32;
    return 0.25;
  }
  return 0.5;
}

/**
 * 规则估算：样本 >= 3 条时用四分位区间；否则用「原价 × 成色系数」。
 * 返回 null 表示信息不足以估算。
 */
function ruleBasedEstimate(input) {
  const stats = summarize(input);

  if (stats.sampleCount >= 3) {
    const low = stats.p25 || Math.round(stats.median * 0.85);
    const high = stats.p75 || Math.round(stats.median * 1.15);
    const suggested = stats.median;
    return {
      low: round(Math.min(low, suggested)),
      high: round(Math.max(high, suggested)),
      suggested: round(suggested),
      confidence: 'low',
      reasons: [
        `按 ${stats.sampleCount} 条同款样本估算，中位数 ${stats.median} 元`,
        stats.soldCount > 0 ? `其中 ${stats.soldCount} 条为已成交` : '样本均为在售挂价，可能偏高',
      ],
      risk: '模型暂不可用，以上为统计估算，建议人工复核',
      source: 'fallback',
    };
  }

  // 1~2 条样本：真实成交比「原价 × 成色」更可信，优先用样本中位数
  if (stats.sampleCount >= 1) {
    const base = stats.median;
    return {
      low: round(base * 0.85),
      high: round(base * 1.15),
      suggested: round(base),
      confidence: 'low',
      reasons: [
        `仅有 ${stats.sampleCount} 条同款样本，中位数 ${stats.median} 元`,
        '样本偏少，区间已放宽',
      ],
      risk: stats.soldCount === 0 ? '样本均为在售挂价，成交价通常更低' : '样本量少，建议再对比几条',
      source: 'fallback',
    };
  }

  if (input.originalPrice) {
    const factor = conditionFactor(input.condition);
    const base = input.originalPrice * factor;
    const low = base * 0.85;
    const high = base * 1.15;
    return {
      low: round(low),
      high: round(high),
      suggested: round(base),
      confidence: 'low',
      reasons: [
        `按原价 ${input.originalPrice} 元、成色「${input.condition || '未标注'}」折算`,
        '样本不足 3 条，区间已放宽',
      ],
      risk: '缺少同款成交样本，仅供参考',
      source: 'fallback',
    };
  }

  return null;
}

module.exports = { summarize, ruleBasedEstimate, conditionFactor, median, quantile };
