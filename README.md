# 闲圈 · Xianquan

面向学生的校园二手交易与拼团项目，让闲置物品找到新主人，也让同学更方便地一起购买所需物品。

项目包含 **Android 客户端、微信小程序和独立的商品估价服务**。当前以课程项目和功能原型为定位，适合学习、演示与继续开发。

## 功能与当前状态

| 模块 | 已有实现 | 当前边界 |
| --- | --- | --- |
| 二手交易 | 商品浏览、分类、发布、详情、收藏及订单请求 | Android 主要使用本地 Room 数据库，不是多用户联网交易平台 |
| 拼团 | 发起拼团、加入及人数展示、相关消息 | 小程序包含演示逻辑；云函数仍需完善并发与权限控制 |
| 个人中心与消息 | 资料编辑、消息列表、引导与交互动效 | 本地资料与演示数据不等于服务端身份认证 |
| 图书辅助发布 | ISBN 扫描、Google Books / Open Library 查询 | 需要网络，第三方服务可用性取决于运行环境 |
| 商品估价 | Node.js 接口、模型估价、规则兜底、缓存与限流 | 默认可无密钥运行；不自动采集真实市场成交数据 |

没有接入真实支付、资金托管或完整的线上交易后台。

## 目录

```text
app/                 Android 应用（Kotlin / Jetpack Compose）
miniprogram/         微信小程序页面和组件
cloudfunctions/     joinGroup / sendMessage 云函数原型
price-server/       Node.js 商品估价服务及测试
domain/             原始包保留的用例参考文件，不参与 Android 构建
gradle/             Gradle Wrapper 与依赖版本配置
docs/               项目状态与检查记录
```

Android 实际使用的业务用例位于 `app/src/main/java/com/example/homework2/domain/`。

## 启动估价服务

使用 Node.js 22 或更高版本。在仓库根目录执行：

```sh
cd price-server
node server.js
```

默认监听 `http://127.0.0.1:8787`，健康检查为 `GET /healthz`。无需密钥即可使用规则估价。

若要启用模型估价，将 `.env.example` 复制为 `.env`，仅在本机或服务器填写自己的 `DEEPSEEK_API_KEY`，然后运行：

```sh
node --env-file=.env server.js
```

接口字段与示例见 [估价服务文档](price-server/README.md)。密钥只保存在服务端，不写入客户端或提交到 Git。

## 运行 Android 客户端

1. 在 Android Studio 中打开仓库根目录，安装 Android SDK 34。
2. 由 Android Studio 生成本机 `local.properties`，不要提交该文件。
3. 按原项目的 Gradle / JDK 配置同步依赖，选择 API 24 或更高版本的模拟器或设备运行 `app`。
4. 如需估价，先启动 `price-server`。Android 模拟器默认通过 `http://10.0.2.2:8787` 访问电脑本机。

真机调试需要把 `PriceEstimateClient.kt` 中的 `BASE_URL` 改为电脑局域网地址，并让服务监听局域网接口。例如在 PowerShell 中：

```powershell
$env:HOST = '0.0.0.0'
node server.js
```

**构建状态：** 保留了原包的 AGP 8.5.2、Kotlin 2.0.21、Gradle 9.6.0 和 Gradle Daemon JDK 25 配置；这组版本尚未通过本次完整构建验证，不能视为已验证的兼容组合。源码目标为 Java 17。本次环境没有 Android SDK，未生成 APK。版本兼容与设备验证是后续待办，详见 [项目状态](docs/PROJECT_STATUS.md)。

## 运行微信小程序

1. 在微信开发者工具中导入仓库根目录的 `project.config.json`。
2. 默认 `touristappid` 用于原型预览；云开发功能需要自己的 AppID 和云开发环境。
3. 在 `miniprogram/app.js` 的 `wx.cloud.init` 中配置自己的环境。
4. 按实际业务权限创建 `goods`、`messages` 集合，部署 `cloudfunctions/joinGroup` 和 `cloudfunctions/sendMessage`（云端安装依赖）。
5. 如需云端估价，按估价服务文档单独部署 `priceEstimate`。

部分页面仍使用内置样例或本地状态，部署云函数不会自动完成所有页面的数据联通。不要为了跑通演示而把数据库设置为公开可写。

## 验证

```sh
cd price-server
npm test
```

GitHub Actions 已配置估价服务测试与小程序 / 云函数 JavaScript 语法检查；不依赖 API 密钥，不调用真实模型。它不代表 Android 构建或微信云端端到端验证。

## 参与开发

欢迎通过 Issue 描述问题或提出改进，再通过 Pull Request 提交修改。请阅读 [贡献指南](CONTRIBUTING.md)、[安全说明](SECURITY.md) 和 [项目状态](docs/PROJECT_STATUS.md)。

## 许可证

项目自身代码采用 [MIT](LICENSE) 许可证。依赖、远程图片及其他第三方资源仍遵循各自许可，见 [第三方资源说明](THIRD_PARTY_NOTICES.md)。
