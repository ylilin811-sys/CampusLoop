'use strict';

/**
 * 提示词。
 *
 * 设计原则：
 *  1. 模型只做「在统计锚点附近修正 + 解释」，不做自由发挥 —— 二手价格是强数据题。
 *  2. 输出必须是严格 JSON，字段少而固定，方便校验与前端渲染。
 *  3. 明确禁止编造样本里不存在的成交记录（这是模型最常见的幻觉）。
 *  4. 数据不足时要求「放宽区间 + 降置信度」，而不是硬给一个精确数。
 */

const SYSTEM_PROMPT = `你是「校园二手 + 拼单」小程序的定价助手。你的唯一任务：根据商品信息与同款成交/在售样本，给出人民币价格区间、建议价和理由。

严格遵守以下规则：
1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释性文字。
2. JSON 字段固定为：low、high、suggested、confidence、reasons、risk，不得增删字段。
   - low / high / suggested：整数，单位元，最小 1；必须满足 low ≤ suggested ≤ high。
   - confidence：只能是 "high"、"medium"、"low" 三者之一。
   - reasons：1~3 条中文短句，每条不超过 40 字。
   - risk：一句中文提示，不超过 40 字。
3. 价格以 stats.median（同款样本中位数）为主锚点；若你给出的建议价偏离中位数超过 30%，必须在 reasons 里说明原因（成色、使用时长、拼单人数、季节等）。
4. 严禁编造 comparables 中不存在的成交记录、价格或平台数据。你只能引用我给你的字段。
5. stats.sampleCount < 3 时：区间必须放宽，confidence 只能是 "low"；不要假装精确。
6. mode 为 "groupbuy" 时，给出的是「人均拼单价 / 单件价」，不是整单总价；groupSize 越大，单件价可适当下浮。
7. 校园场景常识：学生预算敏感，成交价通常比同款闲鱼挂价低 10%~20%；6~7 月毕业季供给多、价格偏低；9 月开学季需求旺、价格略高。仅在信息支持时才引用，不要硬套。
8. reasons 必须引用具体数字或字段（例如「样本中位数 100 元」「原价 199 元、95 新」），不要写空话。
9. 不承诺「市场价」，只给参考区间。

输出示例（仅示意格式，内容不要照抄）：
{"low":85,"high":105,"suggested":95,"confidence":"medium","reasons":["3 条同款样本中位数 100 元","95 新按原价 6 折折算约 90 元"],"risk":"成色描述主观，建议当面验货"}`;

function compactComparables(comparables, suggestedHint) {
  const picked = comparables.slice(0, 20).map((item) => ({
    title: item.title,
    price: item.price,
    condition: item.condition || undefined,
    status: item.status,
    daysAgo: item.daysAgo || undefined,
  }));
  // 建议价附近的最有参考价值，简单按距离排序但不裁剪条数，交给模型权衡
  if (suggestedHint) {
    picked.sort((a, b) => Math.abs(a.price - suggestedHint) - Math.abs(b.price - suggestedHint));
  }
  return picked;
}

function buildMessages(input, stats, now = new Date()) {
  const payload = {
    task: 'estimate_price',
    now: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`,
    mode: input.mode,
    item: {
      title: input.title,
      desc: input.desc || undefined,
      condition: input.condition || '未标注',
      category: input.category || undefined,
      originalPrice: input.originalPrice || undefined,
      daysUsed: input.daysUsed || undefined,
      groupSize: input.mode === 'groupbuy' ? input.groupSize : undefined,
    },
    stats,
    comparables: compactComparables(input.comparables, stats.median),
  };

  return [
    { role: 'system', content: SYSTEM_PROMPT },
    {
      role: 'user',
      content:
        '请根据下面的数据估价，只输出 JSON 对象：\n' +
        JSON.stringify(payload, null, 2),
    },
  ];
}

module.exports = { SYSTEM_PROMPT, buildMessages };
