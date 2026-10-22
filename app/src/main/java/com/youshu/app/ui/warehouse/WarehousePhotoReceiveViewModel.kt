package com.youshu.app.ui.warehouse

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.warehouse.RecognitionDirection
import com.youshu.app.data.warehouse.WarehousePhotoRecognitionRequest
import com.youshu.app.data.warehouse.WarehouseRecognitionGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WarehousePhotoReceiveViewModel @Inject constructor(
    private val gateway: WarehouseRecognitionGateway,
    private val savedState: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val restored = savedState.get<ArrayList<PhotoReceiveLine>>(LINES).orEmpty().map {
        if (it.working) it.copy(working = false, error = "识别已中断，请点击重试") else it
    }
    private val _lines = MutableStateFlow(restored)
    val lines = _lines.asStateFlow()
    private val _copying = MutableStateFlow(false)
    val copying = _copying.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()
    private val recognitionSlots = Semaphore(2)
    private var saveToken: String? = null

    init {
        publish(restored)
        if (savedState.get<Boolean>(IMPORTING) == true) _message.value = "照片导入已中断，请重新选择未加入的照片"
        savedState[IMPORTING] = false
    }

    fun initialize(existing: StockDraftUi?) {
        if (savedState.get<Boolean>(INITIALIZED) == true) return
        savedState[INITIALIZED] = true
        if (_lines.value.isEmpty() && existing != null) publish(listOf(PhotoReceiveLine.fromDraft(existing)))
    }

    fun seedDemo(lines: List<PhotoReceiveLine>) {
        if (_saving.value || _copying.value || _lines.value.isNotEmpty() || savedState.get<Boolean>(DEMO_SEEDED) == true) return
        savedState[DEMO_SEEDED] = true
        publish(lines.take(MAX_PHOTOS).map { it.copy(demo = true, working = false) })
    }

    fun addPhotos(uris: List<String>) {
        if (_saving.value || _copying.value || uris.isEmpty()) return
        val known = _lines.value.flatMap { it.attachments + it.sourceUri }.toSet()
        val distinct = uris.filter { it.isNotBlank() && it !in known }.distinct()
        if (distinct.isEmpty()) return
        // Legacy drafts can retain several attachments in one card. Keep them intact, but
        // count every retained image against the import limit rather than only counting cards.
        val retainedPhotos = _lines.value.flatMap { it.attachments }.filter { it.isNotBlank() }.distinct().size
        val selected = distinct.take((MAX_PHOTOS - retainedPhotos).coerceAtLeast(0))
        if (selected.isEmpty()) { _message.value = "每次最多录入 $MAX_PHOTOS 张照片，请先加入待确认"; return }
        _message.value = null
        _copying.value = true
        savedState[IMPORTING] = true
        viewModelScope.launch {
            var failures = 0
            try {
                for (source in selected) {
                    val copied = try { withContext(Dispatchers.IO) { ownPhoto(source) } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { failures++; continue }
                    val line = PhotoReceiveLine(photoUri = copied, sourceUri = source)
                    publish(_lines.value + line)
                    recognize(line.id)
                }
                val skipped = distinct.size - selected.size
                _message.value = listOfNotNull(
                    if (failures > 0) "$failures 张照片读取失败，请重新选择" else null,
                    if (skipped > 0) "已达到 $MAX_PHOTOS 张上限，另有 $skipped 张未加入" else null
                ).takeIf { it.isNotEmpty() }?.joinToString("；")
            } finally {
                _copying.value = false
                savedState[IMPORTING] = false
            }
        }
    }

    fun delete(id: String) {
        if (_saving.value) return
        jobs.remove(id)?.cancel()
        publish(_lines.value.filterNot { it.id == id })
        _message.value = null
    }

    fun edit(id: String, field: String, value: String) {
        if (_saving.value) return
        publish(_lines.value.map { if (it.id == id) it.edit(field, value) else it })
        _message.value = null
    }

    fun retry(id: String) {
        if (_saving.value) return
        val line = _lines.value.firstOrNull { it.id == id } ?: return
        if (line.demo) { _message.value = "当前是示例结果；添加真实照片后可体验识别"; return }
        recognize(id)
    }

    fun save(products: List<WarehouseProductUi>, onSave: (List<StockDraftUi>, (String?) -> Unit) -> Unit, onSaved: () -> Unit) {
        if (_saving.value || _copying.value || _lines.value.any { it.working }) return
        val snapshot = _lines.value.toList()
        if (snapshot.isEmpty()) { _message.value = "请先添加商品照片"; return }
        val invalid = snapshot.withIndex().firstOrNull { it.value.validationError != null }
        if (invalid != null) { _message.value = "第 ${invalid.index + 1} 项：${invalid.value.validationError}"; return }
        val drafts = snapshot.map { it.toDraft(products) }
        _saving.value = true
        _message.value = null
        val token = UUID.randomUUID().toString()
        saveToken = token
        try {
            onSave(drafts) { error ->
                if (saveToken != token) return@onSave
                saveToken = null
                _saving.value = false
                if (error != null) _message.value = error
                else { jobs.values.forEach { it.cancel() }; jobs.clear(); publish(emptyList()); onSaved() }
            }
        } catch (error: Exception) {
            if (saveToken == token) {
                saveToken = null
                _saving.value = false
                _message.value = error.message ?: "保存失败，请重试；本次录入内容仍保留"
            }
        }
    }

    private fun recognize(id: String) {
        if (_saving.value) return
        val source = _lines.value.firstOrNull { it.id == id } ?: return
        if (source.demo) return
        if (source.photoUri.isBlank()) {
            publish(_lines.value.map { if (it.id == id) it.copy(error = "原始照片不存在，请手动补充字段") else it })
            return
        }
        jobs.remove(id)?.cancel()
        val generation = UUID.randomUUID().toString()
        publish(_lines.value.map { if (it.id == id) it.copy(working = true, generation = generation, error = null) else it })
        jobs[id] = viewModelScope.launch {
            try {
                val fields = recognitionSlots.withPermit {
                    gateway.recognizePhotos(WarehousePhotoRecognitionRequest(RecognitionDirection.INBOUND, listOf(source.photoUri)))
                }
                if (!_saving.value) publish(_lines.value.map {
                    if (it.id == id && it.generation == generation) it.applyRecognition(fields).copy(
                        error = if (fields.isEmpty) "没有识别到可靠字段，可以重试或手动补充" else null
                    ) else it
                })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (!_saving.value) publish(_lines.value.map {
                    if (it.id == id && it.generation == generation) it.copy(working = false, error = error.message ?: "识别失败，请重试或手动填写") else it
                })
            } finally {
                if (_lines.value.firstOrNull { it.id == id }?.generation == generation) jobs.remove(id)
            }
        }
    }

    private fun publish(lines: List<PhotoReceiveLine>) {
        _lines.value = lines
        savedState[LINES] = ArrayList(lines)
    }

    private fun ownPhoto(source: String): String {
        val uri = Uri.parse(source)
        val directory = File(context.filesDir, "images").apply { mkdirs() }.canonicalFile
        if (uri.scheme == "file") {
            val file = File(requireNotNull(uri.path)).canonicalFile
            if (file.parentFile == directory && file.isFile && file.length() in 1..MAX_IMAGE_BYTES) return Uri.fromFile(file).toString()
        }
        val target = File(directory, "warehouse_${UUID.randomUUID()}.jpg")
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: error("照片读取失败")
            stream.use { input -> target.outputStream().use { output ->
                val buffer = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= MAX_IMAGE_BYTES) { "照片过大" }
                    output.write(buffer, 0, count)
                }
                check(total > 0) { "照片为空" }
            } }
            return Uri.fromFile(target).toString()
        } catch (error: Exception) { target.delete(); throw error }
    }

    companion object {
        const val MAX_PHOTOS = 12
        private const val MAX_IMAGE_BYTES = 25_000_000L
        private const val LINES = "receive_photo_lines"
        private const val INITIALIZED = "receive_photo_initialized"
        private const val DEMO_SEEDED = "receive_photo_demo_seeded"
        private const val IMPORTING = "receive_photo_importing"
    }
}
