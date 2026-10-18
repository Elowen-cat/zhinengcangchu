package com.youshu.app.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.ai.AiInferenceRepository
import com.youshu.app.data.warehouse.RecognitionDirection
import com.youshu.app.data.warehouse.WarehousePhotoRecognitionRequest
import com.youshu.app.data.warehouse.WarehouseRecognitionFields
import com.youshu.app.data.warehouse.WarehouseRecognitionGateway
import com.youshu.app.data.warehouse.WarehouseTranscriptRecognitionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WarehouseRecognitionUiState(
    val sourcePhoto: String? = null,
    val working: Boolean = false,
    val fields: WarehouseRecognitionFields? = null,
    val message: String? = null,
    val resultId: String? = null
)

data class WarehouseVoiceUiState(
    val working: Boolean = false,
    val transcript: String? = null,
    val message: String? = null,
    val transcriptId: String? = null
)

data class WarehouseTranscriptUiState(
    val sourceTranscript: String? = null,
    val working: Boolean = false,
    val fields: WarehouseRecognitionFields? = null,
    val message: String? = null,
    val resultId: String? = null
)

@HiltViewModel
class WarehouseRecognitionViewModel @Inject constructor(
    private val gateway: WarehouseRecognitionGateway,
    private val aiInferenceRepository: AiInferenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(WarehouseRecognitionUiState())
    val state = _state.asStateFlow()
    private val _voiceState = MutableStateFlow(WarehouseVoiceUiState())
    val voiceState = _voiceState.asStateFlow()
    private val _transcriptState = MutableStateFlow(WarehouseTranscriptUiState())
    val transcriptState = _transcriptState.asStateFlow()
    private var photoJob: Job? = null
    private var transcriptJob: Job? = null

    fun transcribeVoice(audioPath: String) {
        if (_voiceState.value.working) return
        _voiceState.value = WarehouseVoiceUiState(working = true)
        viewModelScope.launch {
            aiInferenceRepository.transcribeSpeech(audioPath)
                .onSuccess { text ->
                    _voiceState.value = WarehouseVoiceUiState(
                        transcript = text,
                        message = "语音已转成文字",
                        transcriptId = UUID.randomUUID().toString()
                    )
                }
                .onFailure { failure ->
                    _voiceState.value = WarehouseVoiceUiState(
                        message = failure.message ?: "语音识别失败，请再试一次"
                    )
                }
            runCatching { java.io.File(audioPath).delete() }
        }
    }

    fun recognizePhoto(direction: EntryDirection, photoUris: List<String>) {
        val first = photoUris.firstOrNull() ?: return
        if (_state.value.working && _state.value.sourcePhoto == first) return
        photoJob?.cancel()
        _state.value = WarehouseRecognitionUiState(sourcePhoto = first, working = true)
        photoJob = viewModelScope.launch {
            runCatching {
                gateway.recognizePhotos(WarehousePhotoRecognitionRequest(
                    if (direction == EntryDirection.INBOUND) RecognitionDirection.INBOUND else RecognitionDirection.OUTBOUND,
                    listOf(first)
                ))
            }.onSuccess { fields ->
                _state.value = WarehouseRecognitionUiState(sourcePhoto = first, fields = fields,
                    message = if (fields.isEmpty) "没有识别到可靠字段，请手动填写" else "识别结果已自动填入，请核对表单",
                    resultId = UUID.randomUUID().toString())
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _state.value = WarehouseRecognitionUiState(sourcePhoto = first,
                    message = error.message ?: "识别失败，请手动填写")
            }
        }
    }

    fun interpretTranscript(direction: EntryDirection, transcript: String) {
        val text = transcript.trim()
        if (text.isEmpty()) return
        if (_transcriptState.value.working && _transcriptState.value.sourceTranscript == text) return
        transcriptJob?.cancel()
        _transcriptState.value = WarehouseTranscriptUiState(sourceTranscript = text, working = true)
        transcriptJob = viewModelScope.launch {
            runCatching {
                gateway.interpretTranscript(WarehouseTranscriptRecognitionRequest(
                    if (direction == EntryDirection.INBOUND) RecognitionDirection.INBOUND else RecognitionDirection.OUTBOUND,
                    text
                ))
            }.onSuccess { fields ->
                _transcriptState.value = WarehouseTranscriptUiState(sourceTranscript = text, fields = fields,
                    message = if (fields.isEmpty) "没有识别到可靠字段，请补充商品信息" else "识别结果已自动填入，请核对表单",
                    resultId = UUID.randomUUID().toString())
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                val fallback = WarehouseVoiceDraftParser.parse(text)
                val fields = WarehouseRecognitionFields(
                    productName = fallback.productName, quantity = fallback.quantity, unit = fallback.unit,
                    batchCode = fallback.batchCode, expiry = fallback.expiry,
                    location = fallback.location, destination = fallback.destination
                )
                _transcriptState.value = WarehouseTranscriptUiState(sourceTranscript = text,
                    fields = fields.takeUnless { it.isEmpty },
                    message = if (fields.isEmpty) error.message ?: "字段提取失败，请手动填写"
                        else "智能提取暂不可用，已按明确标签填入，请核对",
                    resultId = if (fields.isEmpty) null else UUID.randomUUID().toString())
            }
        }
    }
}
