'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { createPriceService } = require('../src/service');
const { validateModelOutput, extractJson } = require('../src/validate');
const { ruleBasedEstimate, summarize } = require('../src/stats');
const { parseInput } = require('../src/input');
const { TtlCache, createRateLimiter } = require('../src/cache');
const defaultConfig = require('../src/config');

const baseConfig = {
  ...defaultConfig,
  retries: 0,
  cacheTtlMs: 5000,
  cacheMax: 100,
};

const GOOD_OUTPUT = JSON.stringify({
  low: 88,
  high: 108,
  suggested: 98,
  confidence: 'medium',
  reasons: ['3 条同款样本中位数 100 元', '95 新按原价 6 折折算'],
  risk: '成色描述主观，建议当面验货',
});

function makeService(chatImpl, configOverrides = {}) {
  return createPriceService({
    config: { ...baseConfig, ...configOverrides },
    deps: { chat: chatImpl },
  });
}

const sampleInput = {
  title: '小米蓝牙耳机 Air2 SE',
  desc: '去年双十一买的，功能正常',
  condition: '95新',
  originalPrice: 199,
  daysUsed: 300,
  comparables: [
    { title: '小米蓝牙耳机 Air2 SE', price: 90, status: 'sold' },
    { title: '小米蓝牙耳机 Air2 SE', price: 100, condition: '95新' },
    { title: '小米蓝牙耳机 Air2 SE', price: 110, status: 'sold' },
  ],
};

test('正常估价：返回区间、建议价、统计锚点', async () => {
  let calls = 0;
  const service = makeService(async () => {
    calls += 1;
    return GOOD_OUTPUT;
  });

  const result = await service.estimate(sampleInput);

  assert.equal(result.ok, true);
  assert.equal(result.data.suggested, 98);
  assert.equal(result.data.low, 88);
  assert.equal(result.data.high, 108);
  assert.equal(result.data.basis.median, 100);
  assert.equal(result.data.basis.sampleCount, 3);
  assert.equal(result.meta.source, 'model');
  assert.equal(result.meta.cached, false);
  assert.equal(calls, 1);
  assert.equal(result.data.reasons.length, 2);
});

test('缓存：相同输入第二次直接命中，不再调用模型', async () => {
  let calls = 0;
  const service = makeService(async () => {
    calls += 1;
    return GOOD_OUTPUT;
  });

  await service.estimate(sampleInput);
  const second = await service.estimate(sampleInput);

  assert.equal(second.ok, true);
  assert.equal(second.meta.cached, true);
  assert.equal(calls, 1);
});

test('模型连续返回非 JSON：降级为规则估算，接口仍可用', async () => {
  let calls = 0;
  const service = makeService(async () => {
    calls += 1;
    return '抱歉，我无法给出估价。';
  }, { retries: 1 });

  const result = await service.estimate(sampleInput);

  assert.equal(result.ok, true);
  assert.equal(result.meta.source, 'fallback');
  assert.equal(result.meta.degraded, true);
  assert.equal(result.data.confidence, 'low');
  assert.equal(calls, 2); // 1 次 + 1 次重试
  assert.ok(result.data.low <= result.data.suggested);
  assert.ok(result.data.suggested <= result.data.high);
});

test('模型未配置（无 Key）：不重试，直接规则兜底', async () => {
  const { ModelError } = require('../src/model');
  let calls = 0;
  const service = makeService(async () => {
    calls += 1;
    throw new ModelError('NOT_CONFIGURED', '未配置 DEEPSEEK_API_KEY', { retryable: false });
  }, { retries: 3 });

  const result = await service.estimate(sampleInput);

  assert.equal(result.ok, true);
  assert.equal(result.meta.source, 'fallback');
  assert.equal(calls, 1);
});

test('输入校验：标题太短 / 既无原价又无样本', async () => {
  const service = makeService(async () => GOOD_OUTPUT);

  const noTitle = await service.estimate({ originalPrice: 100 });
  assert.equal(noTitle.ok, false);
  assert.equal(noTitle.code, 'BAD_INPUT');
  assert.equal(noTitle.field, 'title');

  const noPrice = await service.estimate({ title: '高等数学 同济第七版' });
  assert.equal(noPrice.ok, false);
  assert.equal(noPrice.code, 'BAD_INPUT');
  assert.equal(noPrice.field, 'originalPrice');
});

test('护栏：价格超过原价 1.2 倍会被钳制，low/high 写反会被修复', () => {
  const input = parseInput({
    title: '机械键盘 87 键',
    originalPrice: 200,
    comparables: [],
  }).value;

  const clamped = validateModelOutput(
    JSON.stringify({ low: 150, high: 999, suggested: 400, confidence: 'high', reasons: ['x'] }),
    input,
  );
  assert.equal(clamped.high, 240); // 200 * 1.2
  assert.equal(clamped.suggested, 240);
  assert.equal(clamped.low, 150);

  const reversed = validateModelOutput(
    JSON.stringify({ low: 120, high: 80, suggested: 100, confidence: 'medium', reasons: ['x'] }),
    input,
  );
  assert.deepEqual([reversed.low, reversed.suggested, reversed.high], [80, 100, 120]);
});

test('解析：兼容 Markdown 代码块与前后废话', () => {
  const text = '好的，结果如下：\n```json\n{"low":10,"high":20,"suggested":15}\n```\n希望有帮助';
  const parsed = extractJson(text);
  assert.equal(parsed.suggested, 15);
});

test('规则兜底：样本 >= 3 用四分位，样本不足用原价系数', () => {
  const withSamples = parseInput(sampleInput).value;
  const stats = summarize(withSamples);
  const est = ruleBasedEstimate(withSamples);
  assert.equal(stats.median, 100);
  assert.equal(est.source, 'fallback');
  assert.ok(est.low <= est.suggested && est.suggested <= est.high);

  const noSamples = parseInput({ title: '显示器 24 寸', originalPrice: 600, condition: '9成新' }).value;
  const est2 = ruleBasedEstimate(noSamples);
  assert.equal(est2.suggested, Math.round(600 * 0.55));
});

test('缓存与限流组件', () => {
  const cache = new TtlCache({ ttlMs: 50, max: 2 });
  cache.set('a', 1);
  cache.set('b', 2);
  cache.set('c', 3); // 超出容量，淘汰最旧的 a
  assert.equal(cache.get('a'), undefined);
  assert.equal(cache.get('c'), 3);

  const limiter = createRateLimiter({ limit: 2, windowMs: 1000 });
  assert.equal(limiter.take('u1', 0), true);
  assert.equal(limiter.take('u1', 10), true);
  assert.equal(limiter.take('u1', 20), false);
  assert.equal(limiter.take('u2', 20), true);
  assert.equal(limiter.take('u1', 1500), true); // 窗口滑过
});
