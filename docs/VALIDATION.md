# 本地验证记录

日期：2026-11-19。环境：Windows，Node.js v24.21.0。

| 检查 | 结果 |
| --- | --- |
| `price-server` 的 `npm test` | 10 项测试通过（9 项服务逻辑 + 1 项 HTTP 集成测试） |
| JavaScript 语法 | 小程序、云函数和估价服务共 25 个 `.js` 文件通过 `node --check` |
| 忽略规则 | `.env`、`local.properties`、签名文件、APK、Kotlin 日志均被忽略 |
| Android 构建与设备运行 | 未验证：环境未安装 Android SDK |
| 微信开发者工具和云端联调 | 未验证 |
| GitHub Actions | 工作流已配置，尚未在 GitHub 执行 |
| 真实模型调用 | 未执行；测试使用本地替身，不需要密钥 |

HTTP 测试最初因环境禁止回环网络连接而失败；获准使用网络后重跑，全部通过。

本次未修改交易或拼团业务逻辑。现有局限请参阅 `PROJECT_STATUS.md` 和根目录 `SECURITY.md`。
