# 第三方资源说明

根目录 MIT 许可证适用于项目自身代码，不改变第三方库、工具和素材的许可。

- Android 依赖列表位于 `app/build.gradle.kts` 与 `gradle/libs.versions.toml`；各依赖遵循各自发布的许可证。
- `gradle/wrapper/gradle-wrapper.jar` 与 Wrapper 脚本为 Gradle 工具文件，保留原始声明。
- 微信云函数使用 `wx-server-sdk`，遵循其自身许可和微信平台条款。
- 示例图片远程引用 `picsum.photos`，部分头像引用 `api.iconify.design` 的 Twemoji 图形或微信图片地址。它们不因本仓库 MIT 许可而获得重新授权；分发或上线前应核对对应资源的许可、署名要求和可用性，或替换为自有资源。
- Google Books、Open Library 和 DeepSeek 是外部服务，使用时遵循各自服务条款。本仓库不附带服务账号或密钥。

这是来源清单，不是完整的第三方版权审计。新增外部素材时请记录来源与许可，不要移除现有声明。
