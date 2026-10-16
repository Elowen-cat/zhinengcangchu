package com.youshu.app.data.warehouse

import javax.inject.Inject

/** Fields suggested by an external recognizer. The user must review them before saving a draft. */
data class WarehouseRecognitionFields(
    val productName: String? = null,
    val quantity: Int? = null,
    val unit: String? = null,
    val batchCode: String? = null,
    val expiry: String? = null,
    val location: String? = null,
    val destination: String? = null,
    val supplier: String? = null,
    val purchasePrice: String? = null
)

enum class RecognitionDirection { INBOUND, OUTBOUND }

/** Local URI references are passed only after the user starts photo recognition. */
data class WarehousePhotoRecognitionRequest(
    val direction: RecognitionDirection,
    val photoUris: List<String>
)

data class WarehouseTranscriptRecognitionRequest(
    val direction: RecognitionDirection,
    val transcript: String
)

interface WarehouseRecognitionGateway {
    val isConfigured: Boolean

    suspend fun recognizePhotos(request: WarehousePhotoRecognitionRequest): WarehouseRecognitionFields

    suspend fun interpretTranscript(request: WarehouseTranscriptRecognitionRequest): WarehouseRecognitionFields
}

/** No endpoint or credential is stored in the app until the owner's API contract is supplied. */
class UnconfiguredWarehouseRecognitionGateway @Inject constructor() : WarehouseRecognitionGateway {
    override val isConfigured: Boolean = false

    override suspend fun recognizePhotos(request: WarehousePhotoRecognitionRequest): WarehouseRecognitionFields = unavailable()

    override suspend fun interpretTranscript(request: WarehouseTranscriptRecognitionRequest): WarehouseRecognitionFields = unavailable()

    private fun unavailable(): Nothing = error("仓储识别 API 尚未配置")
}
