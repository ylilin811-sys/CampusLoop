'use strict';

/**
 * 本地 / 自建服务器入口。
 *   node --env-file=.env server.js
 *   curl -X POST http://127.0.0.1:8787/api/price/estimate -d '...'
 */

require('./src/http').startServer();
