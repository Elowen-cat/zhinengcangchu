package com.youshu.app.data.warehouse

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.youshu.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Owner overrides are encrypted and excluded from backups. Competition builds carry a public demo token. */
@Singleton
class WarehouseServiceConfig @Inject constructor(@ApplicationContext context: Context) {
    private val file = File(context.noBackupFilesDir, "warehouse_service_config.json")
    private val alias = "warehouse_service_access_v1"
    private val retiredWorkerUrl = "https://warehouse-recognition.dkwzt87gbz.workers.dev"

    data class Snapshot(val baseUrl: String, val accessToken: String) {
        val isConfigured: Boolean get() = baseUrl.isNotBlank() && accessToken.isNotBlank()
    }

    private fun demoSnapshot() = Snapshot(
        BuildConfig.WAREHOUSE_DEMO_BASE_URL.takeIf { BuildConfig.WAREHOUSE_DEMO_ACCESS_TOKEN.isNotBlank() }.orEmpty(),
        BuildConfig.WAREHOUSE_DEMO_ACCESS_TOKEN
    )

    @Synchronized
    fun read(): Snapshot {
        if (!file.exists()) return demoSnapshot()
        return runCatching {
            val json = JSONObject(file.readText())
            val url = json.optString("url")
            if (url.trimEnd('/') == retiredWorkerUrl &&
                BuildConfig.WAREHOUSE_DEMO_ACCESS_TOKEN.isNotBlank()
            ) return demoSnapshot()
            val iv = Base64.decode(json.getString("iv"), Base64.NO_WRAP)
            val cipherText = Base64.decode(json.getString("token"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            Snapshot(url, String(cipher.doFinal(cipherText), Charsets.UTF_8))
        }.getOrElse { demoSnapshot() }
    }

    @Synchronized
    fun save(url: String, newToken: String) {
        val normalized = url.trim().trimEnd('/')
        val uri = runCatching { URI(normalized) }.getOrNull()
        require(uri != null && uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.query == null && uri.fragment == null && uri.port == -1 && uri.path.orEmpty().isBlank()) {
            "服务地址须是 HTTPS 根地址，例如 https://example.com"
        }
        val token = newToken.trim().ifBlank { read().accessToken }
        require(token.length >= 16 && token.length <= 256) { "访问口令长度须为 16–256 个字符" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val json = JSONObject()
            .put("url", normalized)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("token", Base64.encodeToString(cipher.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        val pending = File(file.parentFile, "${file.name}.tmp")
        pending.writeText(json.toString())
        check(pending.renameTo(file)) { "保存服务配置失败" }
    }

    @Synchronized
    fun clear() {
        if (file.exists() && !file.delete()) error("移除服务配置失败")
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
}
