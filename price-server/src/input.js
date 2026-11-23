'use strict';

/**
 * 输入解析与校验。
 * 对外只暴露 parseInput(raw) -> { ok, value } | { ok: false, code, field, message }
 * 所有字符串都会裁剪长度、去掉控制字符，避免把脏数据带进提示词。
 */

const LIMITS = {
  title: 80,
  desc: 500,
  condition: 20,
  category: 30,
  maxComparables: 20,
  maxPrice: 1000000,
  maxDaysUsed: 3650,
  maxGroupSize: 200,
};

const VALID_CONDITIONS = ['全新', '99新', '95新', '9成新', '8成新', '7成新', '6成新及以下'];

function cleanText(value, max) {
  if (typeof value !== 'string') return '';
  return value
    .replace(/[\u0000-\u001f\u007f]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, max);
}

function positiveNumber(value, max) {
  const n = typeof value === 'string' ? Number(value.replace(/[^\d.]/g, '')) : Number(value);
  if (!Number.isFinite(n) || n <= 0) return null;
  return Math.min(Math.round(n * 100) / 100, max);
}

function normalizeCondition(value) {
  const text = cleanText(value, LIMITS.condition);
  if (!text) return '';
  for (const condition of VALID_CONDITIONS) {
    if (text.includes(condition)) return condition;
  }
  // 兼容「九五新」「9.5 成新」这类写法
  if (/9\s*[.．5]?\s*5?\s*成|九五成/.test(text)) return '95新';
  if (/9\s*成/.test(text)) return '9成新';
  if (/8\s*成/.test(text)) return '8成新';
  if (/7\s*成/.test(text)) return '7成新';
  return text;
}

function parseComparables(raw) {
  if (!Array.isArray(raw)) return [];
  const seen = new Set();
  const list = [];
  for (const item of raw.slice(0, LIMITS.maxComparables)) {
    if (!item || typeof item !== 'object') continue;
    const title = cleanText(item.title, LIMITS.title);
    const price = positiveNumber(item.price, LIMITS.maxPrice);
    if (!title || price === null) continue;
    const key = `${title}::${price}`;
    if (seen.has(key)) continue;
    seen.add(key);
    list.push({
      title,
      price,
      condition: normalizeCondition(item.condition) || '',
      daysAgo: positiveNumber(item.daysAgo, 3650) || null,
      status: item.status === 'sold' ? 'sold' : 'listing',
    });
  }
  return list.sort((a, b) => a.price - b.price);
}

function parseInput(raw) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    return { ok: false, code: 'BAD_INPUT', field: 'body', message: '请求体必须是 JSON 对象' };
  }

  const title = cleanText(raw.title, LIMITS.title);
  if (title.length < 2) {
    return { ok: false, code: 'BAD_INPUT', field: 'title', message: '商品标题至少 2 个字' };
  }

  const originalPrice = positiveNumber(raw.originalPrice, LIMITS.maxPrice);
  const comparables = parseComparables(raw.comparables);
  const groupSize = positiveNumber(raw.groupSize, LIMITS.maxGroupSize) || 1;

  if (originalPrice === null && comparables.length === 0) {
    return {
      ok: false,
      code: 'BAD_INPUT',
      field: 'originalPrice',
      message: '至少提供 originalPrice（原价）或 comparables（同款成交样本）之一',
    };
  }

  const value = {
    title,
    desc: cleanText(raw.desc, LIMITS.desc),
    condition: normalizeCondition(raw.condition),
    category: cleanText(raw.category, LIMITS.category),
    originalPrice,
    daysUsed: positiveNumber(raw.daysUsed, LIMITS.maxDaysUsed),
    groupSize: groupSize >= 2 ? Math.floor(groupSize) : 1,
    mode: groupSize >= 2 ? 'groupbuy' : 'resale',
    comparables,
  };

  return { ok: true, value };
}

module.exports = { parseInput, normalizeCondition, cleanText, LIMITS, VALID_CONDITIONS };
