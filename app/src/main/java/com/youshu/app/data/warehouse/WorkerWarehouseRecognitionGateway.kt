package com.youshu.app.data.warehouse

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerWarehouseRecognitionGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: WarehouseServiceConfig
) : WarehouseRecognitionGateway {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(55, TimeUnit.SECONDS)
        .build()

    override val isConfigured: Boolean get() = config.read().isConfigured

    override suspend fun recognizePhotos(request: WarehousePhotoRecognitionRequest): WarehouseRecognitionFields =
        withContext(Dispatchers.IO) {
            val first = request.photoUris.firstOrNull() ?: error("请先拍摄或选择一张商品照片")
            val image = prepareJpeg(Uri.parse(first))
            post("/v1/recognize-photo", JSONObject()
                .put("direction", request.direction.name.lowercase())
                .put("image", "data:image/jpeg;base64,${Base64.encodeToString(image, Base64.NO_WRAP)}"))
        }

    override suspend fun interpretTranscript(request: WarehouseTranscriptRecognitionRequest): WarehouseRecognitionFields =
        withContext(Dispatchers.IO) {
            require(request.transcript.isNotBlank()) { "请先录入语音文本" }
            post("/v1/interpret-transcript", JSONObject()
                .put("direction", request.direction.name.lowercase())
                .put("transcript", request.transcript.take(2000)))
        }

    private fun post(path: String, payload: JSONObject): WarehouseRecognitionFields {
        val settings = config.read()
        check(settings.isConfigured) { "请先在设置中填写识别服务地址和访问口令" }
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(settings.baseUrl + path)
            .header("X-Warehouse-Access-Token", settings.accessToken)
            .post(body).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val message = when (response.code) {
                        401, 403 -> "识别服务拒绝访问，请检查访问口令"
                        402 -> "DeepSeek 账户余额不足，请检查服务端账号"
                        413 -> "照片过大，请换一张清晰且较小的照片"
                        429 -> "识别请求过于频繁，请稍后再试"
                        in 500..599 -> "识别服务暂时不可用，请稍后再试"
                        else -> "识别失败（${response.code}），请手动填写"
                    }
                    error(message)
                }
                val raw = response.body?.string() ?: error("识别服务没有返回内容")
                val fields = JSONObject(raw).optJSONObject("fields") ?: error("识别结果格式不正确")
                fun value(key: String): String? = if (fields.isNull(key)) null
                    else fields.optString(key).trim().take(120).ifBlank { null }
                val quantity = fields.optInt("quantity", 0).takeIf { it in 1..1_000_000 }
                WarehouseRecognitionFields(
                    productName = value("productName"), quantity = quantity, unit = value("unit"),
                    batchCode = value("batchCode"), expiry = value("expiry"),
                    location = value("location"), destination = value("destination"),
                    supplier = value("supplier"), purchasePrice = value("purchasePrice")
                )
            }
        } catch (_: SocketTimeoutException) {
            error("连接识别服务超时。请检查手机网络；仍失败时可先手动填写")
        } catch (_: IOException) {
            error("无法连接识别服务。请切换手机网络后重试，或先手动填写")
        }
    }

    private fun prepareJpeg(uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: error("照片读取失败，请重新选择")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "照片格式不受支持" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1800) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("照片解码失败，请重新选择")
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val upright = if (rotation != 0f) {
            Bitmap.createBitmap(original, 0, 0, original.width, original.height,
                Matrix().apply { postRotate(rotation) }, true).also { original.recycle() }
        } else original
        val maxSide = maxOf(upright.width, upright.height)
        val bitmap = if (maxSide > 1536) {
            val scale = 1536f / maxSide
            Bitmap.createScaledBitmap(upright, (upright.width * scale).toInt().coerceAtLeast(1),
                (upright.height * scale).toInt().coerceAtLeast(1), true).also { upright.recycle() }
        } else upright
        try {
            for (quality in listOf(82, 68, 52)) {
                val output = ByteArrayOutputStream()
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "照片压缩失败" }
                val bytes = output.toByteArray()
                if (bytes.size <= 1_200_000) return bytes
            }
            error("照片内容过大，请换一张照片")
        } finally {
            bitmap.recycle()
        }
    }
}
