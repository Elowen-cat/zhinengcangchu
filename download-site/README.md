# 智能仓储下载页

这页和 APK 一起部署到单独的 Cloudflare Worker。点击首页按钮直接下载同域名下的安装包。由于当前网络上传 25 MB 文件时会超时，APK 在部署时拆成 7 个静态文件；Worker 在下载时按顺序组成完整 APK，访问者只看到一个下载文件。

安装包从 Android 构建结果复制而来，不进入 Git。更新 Android 版本时，要同时更新网页中的版本号、文件名、文件大小、`src/index.js` 中的文件清单与总大小，再重新部署。

部署目录为 `public/`，Wrangler 配置为 `wrangler.jsonc`。此站点与 AI 识别 API Worker 分开，不会改变 API 的密钥和设置。
