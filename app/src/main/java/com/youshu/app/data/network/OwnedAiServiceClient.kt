package com.youshu.app.data.network

import com.youshu.app.data.warehouse.WarehouseServiceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** All AI calls use the owner's SCF service. Provider keys stay on the server. */
@Singleton
class OwnedAiServiceClient @Inject constructor(
    private val config: WarehouseServiceConfig
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(55, TimeUnit.SECONDS)
        .build()

    suspend fun postJson(path: String, body: String): String = withContext(Dispatchers.IO) {
        require(path.startsWith('/') && !path.startsWith("//") && "://" !in path)
        val settings = config.read()
        check(settings.isConfigured) { "智能服务尚未配置，请使用新版安装包" }
        val request = Request.Builder()
            .url(settings.baseUrl + path)
            .header("X-Warehouse-Access-Token", settings.accessToken)
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (response.isSuccessful) return@withContext raw
                val errorCode = runCatching { JSONObject(raw).optString("error") }.getOrDefault("")
                val message = when {
                    response.code == 401 || response.code == 403 -> "智能服务认证失败，请更新应用"
                    response.code == 402 -> "服务额度不足，请联系应用维护者"
                    response.code == 413 -> "录音或图片太大，请缩短后重试"
                    response.code == 429 -> "请求较多，请稍后重试"
                    errorCode == "asr_role_not_configured" || errorCode == "asr_role_permission" ->
                        "腾讯云语音识别权限尚未配置"
                    errorCode == "asr_not_opened" -> "腾讯云语音识别服务尚未开通"
                    errorCode == "empty_transcript" -> "语音没有识别出文字，请再说一次"
                    else -> "智能服务暂时不可用（${response.code}），请稍后重试"
                }
                error(message)
            }
        } catch (_: SocketTimeoutException) {
            error("连接智能服务超时，请检查网络后重试")
        } catch (_: IOException) {
            error("无法连接智能服务，请切换网络后重试")
        }
    }
}
