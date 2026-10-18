# 腾讯云云函数仓储识别接口

此目录把仓储照片识别、聊天和语音转文字迁移到腾讯云云函数 SCF。它使用平台分配的 `*.tencentscf.com` HTTPS 函数 URL，不复用旧版其他人维护的阿里云函数，也不把 DeepSeek API Key 放进 App。照片识别的安卓请求路径与返回字段保持不变。

**当前状态（2026-10-18）：** 广州函数已部署，HTTPS 根地址为 `https://1509339681-dwhhcp4gwl.ap-guangzhou.tencentscf.com`。`GET /health`、入库口述字段提取、文字聊天和合成 WAV 语音转写已在电脑端成功。妈妈的 1.8.2 手机安装包拍照时收到 502；用她拍的矿泉水原图在电脑端复现。原函数给 DeepSeek 的 `max_tokens` 仅 300，而思考模式默认开启：原图的模型响应 `finish_reason=length`、`content` 为空。修复版关闭字段提取的思考模式并将上限设为 1024。用户重新部署后，同一原图调用线上接口返回 200，字段为百岁山饮用天然矿泉水、1 瓶。**手机拍照和两个语音入口仍待妈妈真机复测。**公开首页的正式下载按钮仍指向 1.8.1，1.8.2 测试 APK 有独立下载地址。

**1.8.3 自动填表测试包：** [直接下载](https://elowen-cat.github.io/zhinengcangchu/smart-warehouse-1.8.3-test.apk)。仓储语音转写后自动调用 `/v1/interpret-transcript`，可理解不带“商品、数量”等固定标签的日常口述；用户改好文字后仍可手动重试。拍照或从相册选图后自动识别并填入表单。缺失字段留空，由操作者核对补充；出库识别出的批次若不匹配可用库存，不会自动选其他批次。此包只供真机验证，公开首页暂不切换。

## 更新已有函数：修复拍照识别

打开腾讯云控制台 → 云函数 SCF → 广州 → `warehouse-recognition` → **函数代码**。把本目录的 `warehouse-recognition-scf.zip` 作为新的函数代码上传，然后点击**部署**。保留已有的执行方法 `index.main_handler`、环境变量、运行角色及函数 URL，不需要重新创建函数或让手机重新安装 APK。腾讯云的[更新函数说明](https://cloud.tencent.com/document/product/583/84097)确认代码更新需部署到 `$LATEST`。

部署后用矿泉水原图在手机上再点一次“识别第一张照片”；成功应显示候选商品名称，数量、批次、效期等照片不能确定的字段仍须人工核对或填写。若仍失败，先不要反复拍照，保存错误截图并检查函数代码页是否显示最新部署。

## 你需要准备

1. 注册你自己的[腾讯云账号](https://cloud.tencent.com/)并按页面提示完成实名认证。腾讯云云函数的[创建说明](https://cloud.tencent.com/document/product/583/56125)要求先完成这两步。
2. 保留你自己的 DeepSeek API Key，确认账号有余额。只在腾讯云函数的环境变量里填写，不要发在聊天、提交到 GitHub 或写进 APK。
3. 准备一串**独立于 DeepSeek 密钥**的随机访问口令，建议 32 位小写十六进制字符。它会设置为 `APP_ACCESS_TOKEN`，并预置到比赛 APK 中。妈妈和评委不需要填写任何口令。由于 APK 可以被分析提取，该口令只适用于小范围演示，不能视为真正的私密认证。
4. 先看[腾讯云云函数计费说明](https://cloud.tencent.com/document/product/583/17299)。新用户免费额度有期限，云函数、响应流量和 DeepSeek 调用可能分别产生费用。到[费用中心](https://cloud.tencent.com/document/product/555/65784)设置预算提醒。这里的代码只提供单实例限流，**不是全局消费上限**。

## 创建云函数

1. 登录腾讯云控制台，打开“云函数 SCF” → “函数服务”。选择一个中国大陆地域，例如上海或广州。
2. 点“新建” → “从头开始”；函数类型选“事件函数”，运行环境选 **Node.js 18.15**，函数名称可填 `warehouse-recognition`。腾讯云的[Node.js 环境说明](https://cloud.tencent.com/document/product/583/11060)列有这个版本。
3. 代码提交方式选“本地上传 ZIP 包”，上传同目录下的 `warehouse-recognition-scf.zip`。ZIP 根目录必须直接是 `index.js`，不要多包一层文件夹。
4. 执行方法填 `index.main_handler`；超时时间设为 **60 秒**，内存先用 **256 MB**。照片请求在服务端最长等待 DeepSeek 40 秒。
5. 在“环境变量”中添加 `DEEPSEEK_API_KEY` 和 `APP_ACCESS_TOKEN`，两项的值只在你的腾讯云控制台输入。不要把值写到代码或部署说明中。配置环境变量的入口以控制台当时的界面为准。
6. 创建完成后进入该函数的“函数 URL”，新建 URL。选择 **公网访问**；授权方式选择“开放”。这里的“开放”仅指平台不要求腾讯云 CAM 签名，代码仍会核对 `X-Warehouse-Access-Token`。安卓 App 不用浏览器跨域访问，可保持 CORS 关闭。参见[函数 URL 创建说明](https://cloud.tencent.com/document/product/583/100227)。
7. 复制生成的 `https://...tencentscf.com` 地址，保留到域名根部，不要复制 `/health` 或其他路径。函数 URL 是腾讯云提供的访问地址，[文档说明](https://cloud.tencent.com/document/product/583/96099)无需先绑定自己的域名。

## 语音识别还需要开通腾讯云 ASR

DeepSeek 当前 API 接受文字和图片，录音转文字由腾讯云语音识别 ASR 的“一句话识别”完成。它是独立服务，请在[语音识别控制台](https://console.cloud.tencent.com/asr)阅读服务条款并开通，查看[官方开通说明](https://cloud.tencent.com/document/product/1093/104735)和[当前计费说明](https://cloud.tencent.com/document/product/1093/35686)。

给此云函数设置**运行角色**，并授权该角色调用 `asr:SentenceRecognition`；腾讯云的[运行角色说明](https://cloud.tencent.com/document/product/583/47933)指出，角色临时凭证会在函数运行时提供。不要把腾讯云永久 SecretId/SecretKey 放进 APK、聊天或代码仓库。若控制台需要选择预设策略，可先参考[ASR 访问管理文档](https://cloud.tencent.com/document/product/1093/95491)，部署完成后再收紧为只允许一句话识别。语音端点最多接收 60 秒、约 2 MB 的 16 kHz 单声道 WAV；腾讯云[接口文档](https://cloud.tencent.com/document/api/1093/35646)列明音频时长和格式限制。

函数已绑定运行角色 `warehouse_asr_runtime`。联调时使用了腾讯云预设策略 `QcloudASRFullAccess`，它赋予整个 ASR 服务的读写权限，范围比本项目所需的单一接口更宽。在公开发布前，应改成仅授予以下操作的自定义策略；腾讯云的[可授权资源类型文档](https://cloud.tencent.com/document/product/1093/48422)列出 `SentenceRecognition`：

```json
{
  "version": "2.0",
  "statement": [
    {
      "effect": "allow",
      "action": ["asr:SentenceRecognition"],
      "resource": ["*"]
    }
  ]
}
```

更换策略时，先给角色添加这条自定义策略，确认语音调用仍成功，再移除 `QcloudASRFullAccess`。此策略只允许调用一句话识别，不允许管理语音识别服务。网页文字或截图里的腾讯云永久密钥无需提供给项目。

照片和文字聊天可以先部署；ASR 未开通或运行角色未授权时，语音端点会清楚返回配置错误。不要把此阶段当作“语音已修好”。

## 先在手机上检查，再切换 App

1. 用**实际要用的手机**打开 `<函数 URL>/health`。成功应显示 `{"status":"ready"}`。分别使用 Wi-Fi 和移动数据检查；如果访问不到，先不要更换 App 地址。这个检查仅证明入口可达、环境变量已存在，并不能证明 DeepSeek 密钥有效或识别结果正确。
2. 开发阶段如果用**现有旧安装包**试照片识别，可在“我的” → “设置” → “仓储识别服务”手动填函数 URL 和 `APP_ACCESS_TOKEN`。正式给妈妈或评委的新版 APK 会预置这两项，不要求他们配置。不要把 DeepSeek API Key 填进手机。
3. 在“收货入库” → “拍照录入”中选一张清晰的包装照片，点“识别第一张照片”。出现候选字段后逐项核对，再保存草稿、标记已核对、确认入库。识别成功不等于库存已入账。
4. 已生成 1.8.2 本地预览 APK，内置腾讯云函数 URL 与演示访问口令，尚未更新公开下载页。云端语音调用成功后，还须用真实手机分别验证照片、仓储语音、聊天语音与文字聊天，再更新下载页。重新构建时设置 `WAREHOUSE_DEMO_BASE_URL=<函数 URL 根地址>` 与 `WAREHOUSE_DEMO_ACCESS_TOKEN=<相同的 32 位口令>`。绝不要设置 `DEEPSEEK_API_KEY` 到 Android 构建环境。

## 接口行为

| 路径 | 方法 | 作用 |
| --- | --- | --- |
| `/health` | GET | 仅检查服务和两个环境变量是否存在，不调用 DeepSeek |
| `/v1/recognize-photo` | POST | 接收一张 JPEG 的 base64 data URL，返回候选仓储字段 |
| `/v1/interpret-transcript` | POST | 接收已转写的文字并返回仓储候选字段；仓储表单仍保留本地字段解析 |
| `/v1/transcribe-audio` | POST | 腾讯云 ASR 把短 WAV 转成中文文字 |
| `/v1/deepseek/chat/completions` | POST | 代理受限的 DeepSeek 文字和图片聊天请求 |

POST 请求需带 `Content-Type: application/json` 和 `X-Warehouse-Access-Token`。返回 `{ "fields": { ... } }`，与当前 Android 客户端一致。未经授权、格式错误、超限、余额不足与上游不可用会返回相应 HTTP 状态码；不会把 DeepSeek 密钥返回客户端。

**费用与公开分发：** 实例内限流可能因云函数扩容而被绕过。正式对外开放前，需要独立的用户身份认证、跨实例限流和明确的费用控制。参赛小范围演示也应关注[函数并发额度](https://cloud.tencent.com/document/product/583/49313)及腾讯云、DeepSeek 两边的用量。
