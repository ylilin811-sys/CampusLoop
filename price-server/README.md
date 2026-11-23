# campus-price-ai

校园二手 / 拼单小程序的 **AI 估价后端**。给一件商品 + 几条同款成交样本，返回建议价格区间、建议价、理由和风险提示。

- **零依赖**：只用 Node 内置模块（Node ≥ 22），云函数不用打包 `node_modules`
- **不编价格**：先把样本算成中位数锚点，模型只在锚点附近修正
- **可降级**：模型超时 / 没配 Key / 返回垃圾 JSON，自动降级为规则估算并标记 `source: "fallback"`
- **省钱**：60 秒结果缓存 + 每用户每分钟限流

---

## 30 秒跑起来

```bash
cd price-server
node server.js            # 不用 API Key 也能启动，先看降级效果
```

```bash
curl -X POST http://127.0.0.1:8787/api/price/estimate ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"小米蓝牙耳机 Air2 SE\",\"condition\":\"95新\",\"originalPrice\":199,\"comparables\":[{\"title\":\"小米蓝牙耳机 Air2 SE\",\"price\":90,\"status\":\"sold\"}]}"
```

返回（无 Key 时的规则兜底）：

```json
{
  "ok": true,
  "data": {
    "low": 77, "high": 103, "suggested": 90, "confidence": "low",
    "reasons": ["仅有 1 条同款样本，中位数 90 元", "样本偏少，区间已放宽"],
    "risk": "样本量少，建议再对比几条",
    "comparables": [{ "title": "小米蓝牙耳机 Air2 SE", "price": 90, "condition": "", "status": "sold" }],
    "basis": { "sampleCount": 1, "soldCount": 1, "median": 90, "p25": null, "p75": null },
    "source": "fallback"
  },
  "meta": { "cached": false, "source": "fallback", "degraded": true, "reason": "NOT_CONFIGURED" }
}
```

配好 Key 后同一请求会走模型，`source` 变成 `"model"`：

```bash
copy .env.example .env      # 填入 DEEPSEEK_API_KEY
node --env-file=.env server.js
```

跑测试（不需要网络和 Key）：

```bash
npm test
```

---

## 接口约定

### `POST /api/price/estimate`

**请求**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `title` | string | ✅ | 商品标题，2~80 字 |
| `desc` | string | | 描述，≤ 500 字 |
| `condition` | string | | 成色：全新 / 99新 / 95新 / 9成新 / 8成新 / 7成新 / 6成新及以下，也接受自由文本 |
| `category` | string | | 分类，≤ 30 字 |
| `originalPrice` | number | ⚠️ | 原价，元。与 `comparables` 至少给一个 |
| `daysUsed` | number | | 使用天数 |
| `groupSize` | number | | 拼单人数，≥ 2 时进入 `groupbuy` 模式，返回人均价 |
| `comparables` | array | ⚠️ | 同款样本，≤ 20 条，见下 |

`comparables[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `title` | string | 样本标题 |
| `price` | number | 成交价 / 挂价，元 |
| `condition` | string | 样本成色，可选 |
| `status` | `"sold"` \| `"listing"` | 已成交 / 在售，默认 `listing` |
| `daysAgo` | number | 多少天前，可选 |

**成功响应**

```jsonc
{
  "ok": true,
  "data": {
    "low": 88,            // 区间下限
    "high": 108,          // 区间上限
    "suggested": 98,      // 建议价
    "confidence": "medium", // high | medium | low
    "reasons": ["…"],     // 1~3 条，每条 ≤ 40 字
    "risk": "…",          // 风险提示
    "comparables": [],    // 与建议价最接近的 ≤ 5 条样本
    "basis": { "sampleCount": 3, "soldCount": 2, "median": 100, "p25": 85, "p75": 110 },
    "source": "model"     // model | fallback
  },
  "meta": { "cached": false, "source": "model", "model": "deepseek-chat", "latencyMs": 820, "attempts": 1 }
}
```

**失败响应**

| HTTP | `code` | 含义 |
|---|---|---|
| 400 | `BAD_INPUT` | 参数缺失或格式错误，`field` 指出具体字段 |
| 413 | `BODY_TOO_LARGE` | 请求体超过 64 KB |
| 429 | `RATE_LIMITED` | 超出每分钟限流 |
| 502 | `UPSTREAM_TIMEOUT` / `UPSTREAM_5XX` | 模型侧异常，且规则兜底也失败 |
| 500 | `INTERNAL` | 未预期异常 |

> 注意：**模型失败且有兜底数据时返回 200 + `source: "fallback"`**，这是刻意设计——让客户端永远有结果可展示，同时用 `source` 区分可信度。

### `GET /healthz`

```json
{ "ok": true, "service": "campus-price-ai", "model": "deepseek-chat", "configured": true }
```

---

## 部署

### A. 微信云开发（推荐，最少运维）

1. 把整个 `price-server/` 目录复制为 `cloudfunctions/priceEstimate/`（保留 `src/` 和 `index.js`）
2. 云开发控制台 → 云函数 → `priceEstimate` → 配置 → **环境变量**：`DEEPSEEK_API_KEY=sk-...`
3. 上传并部署（云端安装依赖：不需要，零依赖）
4. 小程序里调用：

```js
const res = await wx.cloud.callFunction({
  name: 'priceEstimate',
  data: {
    title: '高等数学 同济第七版',
    condition: '8成新',
    originalPrice: 49.8,
    comparables: [
      { title: '高等数学 同济第七版', price: 15, status: 'sold' },
      { title: '高等数学 同济第七版', price: 20, status: 'sold' },
      { title: '高等数学 同济第七版', price: 18, status: 'sold' },
    ],
  },
});
if (res.result.ok) {
  console.log(res.result.data.suggested, res.result.data.low, res.result.data.high);
} else {
  console.warn(res.result.code, res.result.message);
}
```

> 云函数的出参没有 HTTP 状态码，客户端只需判断 `ok` 字段。

### B. 自建服务器 / Docker

```bash
node --env-file=.env server.js      # 默认只监听 127.0.0.1:8787
PORT=8787 node --env-file=.env server.js
```

放在 Nginx 后面暴露 HTTPS 域名，并把这个域名加入**小程序后台 → 开发管理 → 服务器域名 → request 合法域名**。域名必须已备案。

---

## Prompt 设计说明

`src/prompt.js` 里做了四件事，改提示词时请保留这些约束：

1. **只输出 JSON**，字段固定为 `low / high / suggested / confidence / reasons / risk`，配合 `response_format: { type: "json_object" }`
2. **以样本中位数为锚点**：偏离 > 30% 必须解释，否则模型容易「感觉定价」
3. **禁止编造样本**：模型只能引用传入的 `comparables`，返回结果里我们甚至不采信模型给的样本列表，直接回填输入
4. **数据不足要认怂**：`sampleCount < 3` 必须放宽区间并给 `low` 置信度

护栏在 `src/validate.js`：

- 解析失败 → 触发重试 → 仍失败则降级
- `low > high` 自动排序修复
- 价格超过原价 1.2 倍 → 钳制
- 理由缺失 → 用统计值补一条，不返回空数组

改完提示词后，跑 `npm test` 会验证解析、钳制、缓存、降级这些硬约束不被破坏。

---

## 成本与限流

- 单次请求 ≈ 1k tokens 输入 + 200 tokens 输出，估价是低频操作，实际费用取决于调用量与服务商定价
- 缓存 key = 归一化后的全部输入（sha256），**同一商品 60 秒内只调一次模型**
- 限流：每用户每分钟 20 次（`PRICE_AI_RATE_LIMIT`），云函数用 `OPENID`，HTTP 用 `X-User-Id`
- 缓存和限流都是**进程内**的：云函数多实例、实例回收后不共享。要跨实例一致就换 Redis（`TtlCache` 的 `get/set` 接口保持不变即可）

---

## 常见问题

**Q：能不能让小程序直接调 DeepSeek 官方 API？**
不能。两个硬约束：① Key 会暴露在客户端；② 微信要求 request 域名 HTTPS + 已备案，且要在后台白名单里。必须走自己的后端。

**Q：为什么不用 DSH 的 API？**
DSH 是本地 agent 运行时，`/api` 是带浏览器 token 的内部 RPC，不是给线上小程序用的公开接口。这个服务只做「结构化估价」这一件事，用 DeepSeek 官方 API 就够；如果你要的是「让 agent 上网比价、多步推理」，再在服务器上跑 `dsh --profile headless --json` 包一层。

**Q：模型说 95 新，但用户实际很旧，怎么办？**
价格只是参考。前端必须展示 `risk` 字段，并提示「AI 估价仅供参考」。真要做准，靠的是样本质量：把平台自己的历史成交价喂进 `comparables`，比换更贵的模型有效得多。

**Q：想支持「拼单人均价」？**
传 `groupSize >= 2` 即可，提示词已要求按人均价给，并在 `mode` 里体现。建议前端明确写「人均」而不是「总价」。
