# 仓储识别 API 接入状态（2026-10-16）

## 旧实现梳理

早期家庭物品功能通过 `BackendApiClient` 访问阿里云函数。客户端先向 `POST /v1/session` 申请临时会话，再携带 Bearer token 请求 `/v1/deepseek/chat/completions`、`/v1/qwen/chat/completions` 与 `/v1/amap/*`。图片用 Base64 放入 JSON，语音也可使用 Base64；文本对话支持 SSE。旧服务端代码保留在 `server/` 供查阅。

目前主导航没有早期的智能助手或家庭物品保存页面。仓储入库、出库、盘点、移库与备份均使用本地数据；照片可附于草稿，主动点击识别时才发往新服务。语音使用设备提供的识别服务并由本地规则提取字段。设备的语音服务是否联网取决于系统实现。

## 本阶段调整

- 移除旧云函数地址的构建参数与环境变量读取，正式 App 注入的旧客户端没有服务地址，调用会在发起网络请求前停止。旧客户端的可注入地址构造保留用于现有单元测试与历史代码查阅。
- 新增独立的 `WarehouseRecognitionGateway`，其输入为照片 URI 或语音转写文本，输出为可编辑的仓储字段建议。第七阶段已接入独立 Cloudflare Worker 适配器；只有用户主动点击识别才上传照片。
- “我的 → 智能服务状态”显示仓储 API 的真实接入状态，不再把旧模型配置显示成当前仓储服务。

## 第七阶段进度

服务端源码位于 `competition-worker/`，使用参赛者自己的 Cloudflare 账号和 DeepSeek API Key。App 可在比赛构建时预设 Worker HTTPS 根地址和演示口令，评委安装后无需配置。这个口令可从 APK 提取，只用于演示访问，Worker 通过 Cloudflare 限流绑定限制请求速度；它不是正式用户身份认证，也不是严格消费上限。App 设置页仍可保存自定义服务配置，自定义口令由 Android Keystore 加密并放在不参与备份的目录。DeepSeek 密钥只应放在 Worker Secret 中。

App 已有照片识别请求和候选字段核对界面。识别结果不会直接入账，仍需表单校验、草稿审核和人工确认。Worker 也准备了语音转写文本接口，但 App 当前语音流程仍使用设备转写和本地标签解析。

Worker 已于 2026-10-16 部署至 `https://warehouse-recognition.dkwzt87gbz.workers.dev`，健康检查可用。`DEEPSEEK_API_KEY` 已由参赛者在 Cloudflare 的生产环境 Secret 中设置，演示口令和真机照片识别尚未完成，因此不能在参赛材料中声称 AI 闭环已实测可用。配置步骤见 `competition-worker/README.md`。

## 仍需确认

1. 参赛者部署后的 Worker HTTPS 地址与访问口令；不要把 DeepSeek API Key 发给 AI 助手。
2. DeepSeek 账号是否有可用余额，真机能否访问 Worker，以及实物照片的识别效果。
3. 真实样例里商品、数量、单位、批次、效期、库位是否能被准确读取；错误须由参赛者核对修正。

新方案不复用旧云函数的会话协议或密钥。上线前还需真机联调、错误场景检查与演示录屏。

## 2026-10-18 手机网络问题与国内云迁移

真机截图显示连接 `warehouse-recognition.dkwzt87gbz.workers.dev` 在 10 秒后超时，目标地址为 `199.59.149.236`，与公共 DNS 对该域名的解析结果不符；从开发电脑访问 `/health` 返回 200。由此只能确认手机到 `workers.dev` 的网络路径存在问题，尚不能证明 DeepSeek 调用本身可用。旧版其他 AI 功能访问的是不同的阿里云域名，且服务不属于本项目，不再复用。

已新增 `tencent-scf/` 事件函数，保持 `/v1/recognize-photo` 和原有 JSON 字段约定。Android 的默认服务根地址可通过 `WAREHOUSE_DEMO_BASE_URL` 在构建时注入，设置页也能覆盖地址和访问口令。连接异常会显示可理解的中文提示。广州腾讯云函数现已部署，电脑端健康检查和一次文字提取成功；1.8.2 本地预览 APK 已构建。语音端点仍缺运行角色，手机网络、照片和两个语音入口尚未验证，因此公开下载页继续提供 1.8.1。部署细节及计费风险见 `tencent-scf/README.md`。
