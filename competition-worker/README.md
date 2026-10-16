# 仓储识别服务（比赛版）

这个独立 Worker 只负责把照片或语音转写文本送到 DeepSeek，返回候选字段。App 中的人工确认及库存变动仍在手机本地完成。它不使用以前的阿里云云函数。

## 你需要准备

1. 一个你自己的 Cloudflare 账号，开通 Workers。
2. 你已有的 DeepSeek API Key，并确认账号有可用余额。
3. 一串独立的随机访问口令 `APP_ACCESS_TOKEN`，与 DeepSeek API Key 不同。比赛演示版 APK 可预设这一口令，评委安装后无需填写配置；它可从 APK 中提取，因此仅用作演示入口标识，不能视为真正的私密认证。

部署时在 `competition-worker` 目录安装依赖并运行 `npm run deploy`。随后在 Cloudflare Worker 的 **Settings → Variables and Secrets** 中分别添加加密 Secret：`DEEPSEEK_API_KEY` 和 `APP_ACCESS_TOKEN`。DeepSeek 密钥不得写进 `wrangler.jsonc`、源码、APK、提交记录或聊天。比赛 APK 构建时注入 Worker 地址和演示口令，安装后可直接识别；开发版也可在 App「设置 → 仓储识别服务」中手动覆盖配置。

可通过浏览器打开 `https://你的-worker.workers.dev/health` 查看部署是否在线；实际照片识别需要 App 发起，并会产生 DeepSeek 用量。比赛演示前应在自己的真机上用真实样例核对字段和库存流水。

Worker 现有限流：每个 Cloudflare 节点最多 15 次/分钟，同一来源地址最多 6 次/分钟。Cloudflare 文档说明计数按节点进行，这不是严格的每日消费上限。公开分发 APK 前仍应考虑正式用户身份认证和全局预算；如果演示口令被滥用，可在 Cloudflare 轮换 `APP_ACCESS_TOKEN` 并重新打包 APK。DeepSeek Key 始终仅存放在 Worker Secret 中。
