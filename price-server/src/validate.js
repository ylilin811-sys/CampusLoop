'use strict';

/**
 * 模型输出的解析与「护栏」。
 *
 * 模型不一定守规矩：可能包 Markdown 代码块、返回字符串数字、给出
 * low > high、或者把价格报到原价的两倍。这里统一兜住：
 * 解析失败返回 null（上层重试），数值不合法就钳制到合理范围，
 * 绝不让脏数据流到前端。
 */

const MAX_REASON_LENGTH = 40;

function extractJson(text) {
  if (typeof text !== 'string') return null;
  const trimmed = text.trim();
  const fenced = trimmed.match(/^```(?:json)?\s*([\s\S]*?)\s*```$/i);
  const candidate = fenced ? fenced[1] : trimmed;
  const start = candidate.indexOf('{');
  const end = candidate.lastIndexOf('}');
  if (start === -1 || end <= start) return null;
  try {
    const parsed = JSON.parse(candidate.slice(start, end + 1));
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function toPrice(value) {
  const n = typeof value === 'string' ? Number(value.replace(/[^\d.]/g, '')) : Number(value);
  if (!Number.isFinite(n) || n <= 0) return null;
  return Math.round(n);
}

function toReasonList(value) {
  if (!Array.isArray(value)) return [];
  const list = [];
  for (const item of value) {
    if (typeof item !== 'string') continue;
    const text = item.replace(/\s+/g, ' ').trim().slice(0, MAX_REASON_LENGTH);
    if (text) list.push(text);
    if (list.length === 3) break;
  }
  return list;
}

/**
 * @returns {{low:number,high:number,suggested:number,confidence:string,reasons:string[],risk:string}|null}
 */
function validateModelOutput(rawText, input) {
  const parsed = extractJson(rawText);
  if (!parsed) return null;

  const low = toPrice(parsed.low);
  const high = toPrice(parsed.high);
  const suggested = toPrice(parsed.suggested);
  if (low === null || high === null || suggested === null) return null;

  // 顺序修复：模型偶尔把 low/high 写反
  let ordered = [low, suggested, high].sort((a, b) => a - b);
  let [min, mid, max] = ordered;

  // 上限护栏：不能超过原价的 1.2 倍（二手溢价几乎不成立）
  if (input.originalPrice) {
    const cap = Math.max(1, Math.round(input.originalPrice * 1.2));
    max = Math.min(max, cap);
    mid = Math.min(mid, max);
    min = Math.min(min, mid);
  }

  // 下限护栏
  min = Math.max(1, min);
  mid = Math.max(min, mid);

  const confidence = ['high', 'medium', 'low'].includes(parsed.confidence)
    ? parsed.confidence
    : 'low';
  const reasons = toReasonList(parsed.reasons);
  const risk =
    typeof parsed.risk === 'string' && parsed.risk.trim()
      ? parsed.risk.replace(/\s+/g, ' ').trim().slice(0, MAX_REASON_LENGTH)
      : '二手交易有风险，建议当面验货';

  return { low: min, high: max, suggested: mid, confidence, reasons, risk };
}

module.exports = { extractJson, validateModelOutput, toPrice };
