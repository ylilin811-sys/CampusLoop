'use strict';

/**
 * 零依赖 HTTP 入口（node:http）。
 *
 * 适合：本地联调、自建服务器、Docker。
 * 微信云函数请用根目录的 index.js（逻辑相同，只是入参是 event）。
 */

const http = require('node:http');
const defaultConfig = require('./config');
const { createPriceService } = require('./service');
const { createRateLimiter } = require('./cache');

function sendJson(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, X-User-Id',
    'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
  });
  res.end(body);
}

function readBody(req, maxBytes) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > maxBytes) {
        reject(Object.assign(new Error('请求体过大'), { code: 'BODY_TOO_LARGE' }));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

function statusFor(code) {
  switch (code) {
    case 'BAD_INPUT':
      return 400;
    case 'BODY_TOO_LARGE':
      return 413;
    case 'RATE_LIMITED':
      return 429;
    case 'UPSTREAM_TIMEOUT':
    case 'UPSTREAM_5XX':
    case 'UPSTREAM_NETWORK':
    case 'RATE_LIMITED_UPSTREAM':
      return 502;
    default:
      return 500;
  }
}

function createHandler({ config = defaultConfig } = {}) {
  const service = createPriceService({ config });
  const limiter = createRateLimiter({ limit: config.rateLimit, windowMs: config.rateWindowMs });

  return async function handler(req, res) {
    const url = new URL(req.url, 'http://localhost');

    if (req.method === 'OPTIONS') {
      res.writeHead(204, {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type, X-User-Id',
        'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
      });
      res.end();
      return;
    }

    if (req.method === 'GET' && url.pathname === '/healthz') {
      sendJson(res, 200, {
        ok: true,
        service: 'campus-price-ai',
        model: config.model,
        configured: Boolean(config.apiKey),
      });
      return;
    }

    if (req.method === 'POST' && url.pathname === '/api/price/estimate') {
      const userKey = req.headers['x-user-id'] || req.socket.remoteAddress || 'anonymous';
      if (!limiter.take(String(userKey))) {
        sendJson(res, 429, { ok: false, code: 'RATE_LIMITED', message: '请求过于频繁，请稍后再试' });
        return;
      }

      let raw;
      try {
        const text = await readBody(req, config.maxBodyBytes);
        raw = text ? JSON.parse(text) : {};
      } catch (error) {
        const code = error.code === 'BODY_TOO_LARGE' ? 'BODY_TOO_LARGE' : 'BAD_INPUT';
        sendJson(res, statusFor(code), {
          ok: false,
          code,
          message: code === 'BAD_INPUT' ? '请求体不是合法 JSON' : error.message,
        });
        return;
      }

      try {
        const result = await service.estimate(raw);
        sendJson(res, result.ok ? 200 : statusFor(result.code), result);
      } catch (error) {
        console.error('[price-ai] 未捕获异常', error);
        sendJson(res, 500, { ok: false, code: 'INTERNAL', message: '服务内部错误' });
      }
      return;
    }

    sendJson(res, 404, { ok: false, code: 'NOT_FOUND', message: '接口不存在' });
  };
}

function startServer({ config = defaultConfig, port = config.port, host = process.env.HOST || '127.0.0.1' } = {}) {
  const handler = createHandler({ config });
  const server = http.createServer(handler);
  server.listen(port, host, () => {
    console.log(`[price-ai] http://${host}:${port}  model=${config.model} configured=${Boolean(config.apiKey)}`);
  });
  return server;
}

module.exports = { createHandler, startServer };

if (require.main === module) {
  startServer();
}
