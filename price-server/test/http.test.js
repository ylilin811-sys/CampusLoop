'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');

const { createHandler } = require('../src/http');
const defaultConfig = require('../src/config');

function listen(handler) {
  const server = http.createServer(handler);
  return new Promise((resolve) => {
    server.listen(0, '127.0.0.1', () => {
      const { port } = server.address();
      resolve({ server, base: `http://127.0.0.1:${port}` });
    });
  });
}

function close(server) {
  return new Promise((resolve) => server.close(resolve));
}

test('HTTP：健康检查、估价、限流、404', async () => {
  const handler = createHandler({
    config: { ...defaultConfig, retries: 0, rateLimit: 2, apiKey: '' },
  });
  const { server, base } = await listen(handler);

  try {
    const health = await fetch(`${base}/healthz`).then((r) => r.json());
    assert.equal(health.ok, true);
    assert.equal(health.configured, false);

    const body = {
      title: 'iPad 2021 64G',
      condition: '9成新',
      originalPrice: 2499,
      comparables: [
        { title: 'iPad 2021 64G', price: 1500, status: 'sold' },
        { title: 'iPad 2021 64G', price: 1650, status: 'sold' },
        { title: 'iPad 2021 64G', price: 1750 },
      ],
    };

    // 没有 Key 时走规则兜底，仍然返回 200
    const first = await fetch(`${base}/api/price/estimate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-User-Id': 'test-user' },
      body: JSON.stringify(body),
    });
    assert.equal(first.status, 200);
    const result = await first.json();
    assert.equal(result.ok, true);
    assert.equal(result.meta.source, 'fallback');

    // 第二次触发限流（limit=2 已被上面 1 次 + health? 不，health 不限流）
    const second = await fetch(`${base}/api/price/estimate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-User-Id': 'test-user' },
      body: JSON.stringify(body),
    });
    const third = await fetch(`${base}/api/price/estimate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-User-Id': 'test-user' },
      body: JSON.stringify(body),
    });
    assert.equal(second.status, 200);
    assert.equal(third.status, 429);

    const badJson = await fetch(`${base}/api/price/estimate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-User-Id': 'other-user' },
      body: '{not json',
    });
    assert.equal(badJson.status, 400);

    const missing = await fetch(`${base}/nope`).then((r) => r.status);
    assert.equal(missing, 404);
  } finally {
    await close(server);
  }
});
