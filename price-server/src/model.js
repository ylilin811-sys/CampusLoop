'use strict';

/**
 * DeepSeek（OpenAI 兼容）调用层。
 *
 * 只负责「把消息发出去，把文本拿回来」，不解析业务 JSON。
 * 错误分类成 ModelError，让上层决定要不要重试：
 *   NOT_CONFIGURED  没有 Key，重试无意义
 *   UPSTREAM_TIMEOUT / RATE_LIMITED / UPSTREAM_5XX  可重试
 *   UPSTREAM_REJECTED（4xx，通常是 Key 或参数错）  不可重试
 *   EMPTY_OUTPUT  可重试
 */

class ModelError extends Error {
  constructor(code, message, { retryable = false, status = 0 } = {}) {
    super(message);
    this.name = 'ModelError';
    this.code = code;
    this.retryable = retryable;
    this.status = status;
  }
}

async function chat({ messages, config, signal, fetchImpl = globalThis.fetch }) {
  if (!config.apiKey) {
    throw new ModelError('NOT_CONFIGURED', '未配置 DEEPSEEK_API_KEY', { retryable: false });
  }
  if (typeof fetchImpl !== 'function') {
    throw new ModelError('NO_FETCH', '当前 Node 版本缺少 fetch，请使用 Node 18.17+', { retryable: false });
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.timeoutMs);
  const onAbort = () => controller.abort();
  if (signal) signal.addEventListener('abort', onAbort, { once: true });

  let response;
  try {
    response = await fetchImpl(`${config.baseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${config.apiKey}`,
      },
      body: JSON.stringify({
        model: config.model,
        messages,
        temperature: 0.2,
        max_tokens: 900,
        response_format: { type: 'json_object' },
        stream: false,
      }),
      signal: controller.signal,
    });
  } catch (error) {
    if (error && (error.name === 'AbortError' || controller.signal.aborted)) {
      throw new ModelError('UPSTREAM_TIMEOUT', `模型请求超时（${config.timeoutMs}ms）`, {
        retryable: true,
      });
    }
    throw new ModelError('UPSTREAM_NETWORK', `模型请求失败：${error.message}`, { retryable: true });
  } finally {
    clearTimeout(timer);
    if (signal) signal.removeEventListener('abort', onAbort);
  }

  if (!response.ok) {
    const status = response.status;
    const bodyText = await response.text().catch(() => '');
    if (status === 429) {
      throw new ModelError('RATE_LIMITED', '模型限流（429）', { retryable: true, status });
    }
    if (status >= 500) {
      throw new ModelError('UPSTREAM_5XX', `模型服务异常（${status}）`, { retryable: true, status });
    }
    throw new ModelError(
      'UPSTREAM_REJECTED',
      `模型请求被拒绝（${status}）：${bodyText.slice(0, 200)}`,
      { retryable: false, status },
    );
  }

  const body = await response.json().catch(() => null);
  const content = body && body.choices && body.choices[0] && body.choices[0].message
    ? body.choices[0].message.content
    : '';
  if (typeof content !== 'string' || content.trim() === '') {
    throw new ModelError('EMPTY_OUTPUT', '模型返回了空内容', { retryable: true });
  }
  return content;
}

module.exports = { chat, ModelError };
