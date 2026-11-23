'use strict';

/**
 * 微信云开发云函数入口。
 *
 * 部署方式：把本目录（含 src/）整个作为云函数目录上传，函数名建议 priceEstimate。
 * 环境变量在「云开发控制台 → 云函数 → 配置 → 环境变量」里设置 DEEPSEEK_API_KEY。
 *
 * 小程序端调用：
 *   wx.cloud.callFunction({
 *     name: 'priceEstimate',
 *     data: { title, condition, originalPrice, comparables, groupSize },
 *   })
 * 返回结构与本目录 README 中的 HTTP 接口一致：{ ok, data, meta } 或 { ok:false, code, message }。
 */

const config = require('./src/config');
const { estimatePrice } = require('./src/service');
const { createRateLimiter } = require('./src/cache');

// 云函数实例复用时，限流计数会保留；实例回收后重置，属于尽力而为
const limiter = createRateLimiter({ limit: config.rateLimit, windowMs: config.rateWindowMs });

function unwrapEvent(event) {
  if (!event || typeof event !== 'object') return event;
  // HTTP 触发器会包一层 body；普通 callFunction 直接是业务对象
  if (typeof event.body === 'string') {
    try {
      return JSON.parse(event.body);
    } catch {
      return event;
    }
  }
  if (event.body && typeof event.body === 'object') return event.body;
  return event;
}

exports.main = async (event, context = {}) => {
  const userKey = context.OPENID || context.SOURCE_IP || 'anonymous';
  if (!limiter.take(userKey)) {
    return { ok: false, code: 'RATE_LIMITED', message: '请求过于频繁，请稍后再试' };
  }

  try {
    return await estimatePrice(unwrapEvent(event));
  } catch (error) {
    console.error('[price-ai] 云函数未捕获异常', error);
    return { ok: false, code: 'INTERNAL', message: '服务内部错误' };
  }
};
