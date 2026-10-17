package com.youshu.app.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.ai.AiInferenceRepository
import com.youshu.app.data.warehouse.RecognitionDirection
import com.youshu.app.data.warehouse.WarehousePhotoRecognitionRequest
import com.youshu.app.data.warehouse.WarehouseRecognitionFields
import com.youshu.app.data.warehouse.WarehouseRecognitionGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WarehouseRecognitionUiState(
    val sourcePhoto: String? = null,
    val working: Boolean = false,
    val fields: WarehouseRecognitionFields? = null,
    val message: String? = null
)

data class WarehouseVoiceUiState(
    val working: Boolean = false,
    val transcript: String? = null,
    val message: String? = null
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

    fun transcribeVoice(audioPath: String) {
        if (_voiceState.value.working) return
        _voiceState.value = WarehouseVoiceUiState(working = true)
        viewModelScope.launch {
            aiInferenceRepository.transcribeSpeech(audioPath)
                .onSuccess { text ->
                    _voiceState.value = WarehouseVoiceUiState(
                        transcript = text,
                        message = "语音已转成文字，请核对后填入字段"
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
        if (_state.value.working) return
        val first = photoUris.firstOrNull() ?: return
        _state.value = WarehouseRecognitionUiState(sourcePhoto = first, working = true)
        viewModelScope.launch {
            runCatching {
                gateway.recognizePhotos(WarehousePhotoRecognitionRequest(
                    if (direction == EntryDirection.INBOUND) RecognitionDirection.INBOUND else RecognitionDirection.OUTBOUND,
                    listOf(first)
                ))
            }.onSuccess { fields ->
                _state.value = WarehouseRecognitionUiState(sourcePhoto = first, fields = fields,
                    message = "已生成候选字段，请核对后填入表单")
            }.onFailure { error ->
                _state.value = WarehouseRecognitionUiState(sourcePhoto = first,
                    message = error.message ?: "识别失败，请手动填写")
            }
        }
    }
}
