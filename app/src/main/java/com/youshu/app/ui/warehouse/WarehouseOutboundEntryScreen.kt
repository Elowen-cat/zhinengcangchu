package com.youshu.app.ui.warehouse

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.youshu.app.data.warehouse.WarehouseRecognitionFields
import com.youshu.app.util.WavAudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Capture/recognition only prepares a draft. Posting remains in the reviewed pending queue. */
@Composable
internal fun WarehouseOutboundEntryScreen(
    state: WarehouseUiState, source: EntrySource, productId: String?, initialBatchId: String?, draftId: String?,
    incomingPhotos: List<String>, onPhotosConsumed: () -> Unit, onBack: () -> Unit, onCamera: () -> Unit,
    onSaveDraft: (StockDraftUi, (String?) -> Unit) -> Unit, onSaved: () -> Unit,
    recognitionViewModel: WarehouseRecognitionViewModel = hiltViewModel()
) {
    val existing = state.draft(draftId)
    var itemId by rememberSaveable { mutableStateOf(existing?.id ?: UUID.randomUUID().toString()) }
    var editingId by rememberSaveable { mutableStateOf(existing?.id) }
    var selectedProduct by rememberSaveable { mutableStateOf(existing?.productId ?: productId) }
    var selectedBatch by rememberSaveable { mutableStateOf(existing?.batchId ?: initialBatchId ?:
        state.product(productId)?.sortedBatches?.firstOrNull { it.available() }?.id) }
    var quantity by rememberSaveable { mutableStateOf(existing?.quantity?.toString() ?: if (source == EntrySource.MANUAL) "1" else "") }
    var reason by rememberSaveable { mutableStateOf(existing?.reason ?: WarehouseReplenishment.EFFECTIVE_REASON) }
    var destination by rememberSaveable { mutableStateOf(existing?.destination ?: "销售区") }
    var transcript by rememberSaveable { mutableStateOf(existing?.transcript.orEmpty()) }
    var photo by rememberSaveable { mutableStateOf(existing?.imageUris?.firstOrNull()) }
    var attachments by rememberSaveable { mutableStateOf(existing?.imageUris.orEmpty()) }
    var productTouched by rememberSaveable { mutableStateOf(false) }
    var batchTouched by rememberSaveable { mutableStateOf(false) }
    var quantityTouched by rememberSaveable { mutableStateOf(false) }
    var destinationTouched by rememberSaveable { mutableStateOf(false) }
    var requestedPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var photoResult by rememberSaveable { mutableStateOf<String?>(null) }
    var textResult by rememberSaveable { mutableStateOf<String?>(null) }
    var voiceResult by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestedName by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var choosingProduct by remember { mutableStateOf(false) }
    var choosingBatch by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    var initialized by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val recorder = remember(context) { WavAudioRecorder(context.applicationContext) }
    val photoState by recognitionViewModel.state.collectAsState()
    val voiceState by recognitionViewModel.voiceState.collectAsState()
    val textState by recognitionViewModel.transcriptState.collectAsState()
    val recognizing = photoState.working || voiceState.working || textState.working
    val locked = saving || state.pendingDraftWrite || importing
    val product = state.product(selectedProduct)
    val batch = product?.batches?.firstOrNull { it.id == selectedBatch }
    val stock = outboundStock(state, selectedProduct, selectedBatch, quantity.toIntOrNull(), editingId)
    val error = stock.error ?: if (destination.isBlank()) "请填写出库去向" else null
    val canSave = error == null && !locked && !recognizing && !recording && initialized

    DisposableEffect(recorder) { onDispose { recorder.cancel() } }
    BackHandler(enabled = locked) { }
    LaunchedEffect(itemId) {
        val restored = recognitionViewModel.ensureOutboundSession(itemId)
        if (restored) {
            requestedPhoto = null
            if (source == EntrySource.VOICE && editingId == null && transcript.isNotBlank())
                recognitionViewModel.interpretTranscript(EntryDirection.OUTBOUND, transcript)
        }
        initialized = true
    }

    fun resetItem() {
        itemId = UUID.randomUUID().toString(); editingId = null
        recognitionViewModel.ensureOutboundSession(itemId)
        selectedProduct = null; selectedBatch = null; quantity = if (source == EntrySource.MANUAL) "1" else ""
        reason = WarehouseReplenishment.EFFECTIVE_REASON; destination = "销售区"
        transcript = ""; photo = null; attachments = emptyList(); suggestedName = null
        productTouched = false; batchTouched = false; quantityTouched = false; destinationTouched = false
        requestedPhoto = null; photoResult = null; textResult = null; voiceResult = null
        onPhotosConsumed()
        message = "已加入待确认，可以继续录入下一件"
    }
    fun applyFields(fields: WarehouseRecognitionFields) {
        if (locked) return
        fields.productName?.let { name ->
            suggestedName = name
            if (!productTouched) {
                val normalized = name.filterNot(Char::isWhitespace).lowercase()
                val matches = state.products.filter { it.name.filterNot(Char::isWhitespace).lowercase() == normalized }
                selectedProduct = matches.singleOrNull()?.id
                if (!batchTouched) selectedBatch = null
            }
        }
        if (!batchTouched) {
            val matched = state.product(selectedProduct)
            selectedBatch = if (!fields.batchCode.isNullOrBlank()) {
                matched?.batches?.firstOrNull { it.code == fields.batchCode && it.available() }?.id
            } else matched?.batches?.firstOrNull { it.id == selectedBatch && it.available() }?.id ?:
                matched?.sortedBatches?.firstOrNull { it.available() }?.id
        }
        if (!quantityTouched) fields.quantity?.let { quantity = it.toString() }
        if (!destinationTouched) fields.destination?.takeIf { it.isNotBlank() }?.let { destination = it }
    }
    LaunchedEffect(incomingPhotos, initialized) {
        if (initialized && incomingPhotos.isNotEmpty()) {
            photo = incomingPhotos.first()
            attachments = (incomingPhotos + attachments.drop(1)).distinct()
            onPhotosConsumed()
            requestedPhoto = null
        }
    }
    LaunchedEffect(photo, initialized, itemId) {
        if (initialized && source == EntrySource.PHOTO && photo != null && photo != requestedPhoto &&
            (editingId == null || photo != existing?.imageUris?.firstOrNull())) {
            requestedPhoto = photo
            recognitionViewModel.recognizePhoto(EntryDirection.OUTBOUND, listOf(photo!!))
        }
    }
    LaunchedEffect(photoState.resultId, initialized) {
        if (initialized && photoState.resultId != null && photoState.resultId != photoResult && photoState.sourcePhoto == photo) {
            photoResult = photoState.resultId
            photoState.fields?.let(::applyFields)
        }
    }
    LaunchedEffect(voiceState.transcriptId, initialized) {
        if (initialized && voiceState.transcriptId != null && voiceState.transcriptId != voiceResult) {
            voiceResult = voiceState.transcriptId
            transcript = voiceState.transcript.orEmpty()
            recognitionViewModel.interpretTranscript(EntryDirection.OUTBOUND, transcript)
        }
    }
    LaunchedEffect(textState.resultId, transcript, initialized) {
        if (initialized && textState.resultId != null && textState.resultId != textResult && textState.sourceTranscript == transcript.trim()) {
            textResult = textState.resultId
            textState.fields?.let(::applyFields)
        }
    }
    fun startRecording() {
        if (locked || recognizing) return
        recognitionViewModel.resetSession()
        runCatching { recorder.start() }.onSuccess {
            recordSeconds = 0; recording = true; message = null
        }.onFailure { message = "录音启动失败，请检查麦克风权限" }
    }
    fun finishRecording() {
        val file = runCatching { recorder.stop() }.getOrNull()
        recording = false
        when {
            file == null || file.length() < WavAudioRecorder.MIN_AUDIO_BYTES -> {
                file?.delete(); message = "录音太短，请再说一次"
            }
            file.length() > 2_000_000 -> { file.delete(); message = "录音过长，请控制在60秒内" }
            else -> recognitionViewModel.transcribeVoice(file.absolutePath)
        }
    }
    LaunchedEffect(recording) {
        if (recording) {
            while (recording && recordSeconds < 60) { delay(1000); recordSeconds++ }
            if (recording) finishRecording()
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) startRecording() else message = "需要麦克风权限才能录音，也可以直接输入文字"
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            importing = true
            scope.launch {
                try {
                    val copied = withContext(Dispatchers.IO) { copyOutboundPhoto(context, uri) }
                    photo = copied; attachments = (listOf(copied) + attachments.drop(1)).distinct(); requestedPhoto = null
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { message = "照片读取失败或超过25MB，请重新选择" }
                finally { importing = false }
            }
        }
    }
    fun save(next: Boolean) {
        if (!canSave) return
        val selected = product ?: return
        val chosen = batch ?: return
        val draft = StockDraftUi(itemId, EntryDirection.OUTBOUND, source, selected.id, selected.name,
            quantity.toInt(), selected.unit, chosen.id, chosen.code, chosen.expiry?.toString().orEmpty(), chosen.location,
            reason, destination.trim(), existing?.takeIf { it.id == editingId }?.supplier.orEmpty(),
            existing?.takeIf { it.id == editingId }?.purchasePrice.orEmpty(), attachments, transcript, reviewed = false)
        saving = true
        onSaveDraft(draft) { failure ->
            saving = false
            if (failure != null) message = failure
            else if (next) resetItem() else onSaved()
        }
    }

    Scaffold(containerColor = WarehousePalette.Background, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        Surface(color = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message ?: if (importing) "正在读取照片…" else if (recognizing) "正在识别并分析，请稍候…" else error ?: "核对后加入待确认，不会直接扣库存",
                    fontSize = 12.sp, lineHeight = 18.sp, color = if (error != null || message != null) WarehousePalette.Orange else WarehousePalette.Teal)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { save(true) }, enabled = canSave, modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                        shape = RoundedCornerShape(15.dp)) {
                        Text(if (source == EntrySource.VOICE) "保存并再说一件" else "保存并录下一件", color = if (canSave) WarehousePalette.Teal else WarehousePalette.Muted, fontSize = 13.sp)
                    }
                    Button(onClick = { save(false) }, enabled = canSave, modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                        shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = WarehousePalette.Teal)) {
                        Text(if (saving) "正在保存…" else "加入待确认", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFCFEFF), Color(0xFFF0FAF9))))
            .padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { OutboundHeader(when(source) { EntrySource.PHOTO -> "拍照出库"; EntrySource.VOICE -> "语音出库"; else -> "手动出库" }) { if (!locked) onBack() } }
            if (state.previewMode) item { WarehouseNotice(true) }
            if (source == EntrySource.PHOTO) item {
                OutboundPanel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f).height(125.dp).clip(RoundedCornerShape(18.dp)).background(WarehousePalette.Mint), contentAlignment = Alignment.Center) {
                            if (photo != null) {
                                AsyncImage(photo, "已拍摄照片", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Text("✓ 已拍摄", color = Color.White, fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.TopStart).padding(7.dp).background(WarehousePalette.Navy.copy(alpha=.65f), RoundedCornerShape(8.dp)).padding(6.dp))
                            } else Icon(WarehouseReceiveIcons.Camera, null, Modifier.size(58.dp), tint = Color.Unspecified)
                        }
                        Column(Modifier.weight(.95f), horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedButton(onClick = onCamera, enabled = !locked && !recognizing, contentPadding = PaddingValues(horizontal = 12.dp)) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Text(if (photo == null) " 拍摄商品" else " 重新拍摄", fontSize = 13.sp)
                            }
                            TextButton(onClick = { gallery.launch("image/*") }, enabled = !locked && !recognizing) { Text("从相册选择", fontSize = 13.sp) }
                            if (photo != null) TextButton(onClick = {
                                recognitionViewModel.recognizePhoto(EntryDirection.OUTBOUND, listOf(photo!!))
                            }, enabled = !locked && !recognizing) { Text("重新识别", fontSize = 12.sp) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutboundNotice("照片用于识别商品，出库数量和批次请核对。")
                    if (attachments.size > 1) Text("共 ${attachments.size} 张附件 · 换拍仅替换主照片，其余附件保留", fontSize = 11.sp,
                        color = WarehousePalette.Muted, modifier = Modifier.padding(top = 7.dp))
                    if (photoState.working) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = WarehousePalette.Teal)
                    photoState.message?.let { Text(it, fontSize = 12.sp, lineHeight = 18.sp, color = WarehousePalette.Muted, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
            if (source == EntrySource.VOICE) item {
                OutboundPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(58.dp).background(Color(0xFFE7F3FF), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Icon(WarehouseReceiveIcons.Voice, null, Modifier.size(38.dp), tint = Color.Unspecified)
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(when { recording -> "正在录音 ${recordSeconds}秒"; recognizing -> "正在转写与分析…"; transcript.isNotBlank() -> "语音内容已转写"; else -> "说出商品、数量和去向" },
                                color = WarehousePalette.TealDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("例如：鲜牛奶6瓶，补到销售区", color = WarehousePalette.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(transcript, { transcript = it }, enabled = !locked && !recording && !recognizing,
                        modifier = Modifier.fillMaxWidth(), label = { Text("识别文字 · 可编辑", fontSize = 12.sp) }, minLines = 2,
                        shape = RoundedCornerShape(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            if (recording) finishRecording()
                            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
                            else permission.launch(Manifest.permission.RECORD_AUDIO)
                        }, enabled = !locked && !recognizing) {
                            Icon(if (recording) Icons.Default.Stop else Icons.Default.Mic, null, Modifier.size(20.dp))
                            Text(if (recording) " 停止并识别" else if (transcript.isBlank()) " 开始录音" else " 重新录音")
                        }
                        TextButton(onClick = { recognitionViewModel.interpretTranscript(EntryDirection.OUTBOUND, transcript) },
                            enabled = !locked && !recording && !recognizing && transcript.isNotBlank()) { Text("重新分析文字") }
                    }
                    (voiceState.message ?: textState.message)?.let { Text(it, color = WarehousePalette.Muted, fontSize = 12.sp, lineHeight = 18.sp) }
                    if (recognizing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = WarehousePalette.Teal)
                }
            }
            item {
                OutboundPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (product == null) "选择出库商品" else "请核对识别结果", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WarehousePalette.Navy,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { choosingProduct = true }, enabled = !locked) { Text(if (product == null) "选择商品 ›" else "其他商品 ›", fontSize = 12.sp) }
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(enabled = !locked) { choosingProduct = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        OutboundThumb(photo)
                        Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(product?.name ?: suggestedName ?: "尚未选择库存商品", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                                lineHeight = 27.sp, color = WarehousePalette.Navy)
                            if (product != null) StatusTag(product.category.ifBlank { "库存商品" }, WarehousePalette.Blue)
                            Text(if (product == null) "请选择已有商品后出库" else "已选择库存商品 · 请人工核对", color = WarehousePalette.Teal, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = WarehousePalette.Muted)
                    }
                }
            }
            item {
                OutboundPanel {
                    Row(Modifier.fillMaxWidth().clickable(enabled = !locked && product != null) { choosingBatch = true }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutboundFact("批次 · 可切换", batch?.code ?: "请选择", Modifier.weight(1.25f))
                        OutboundFact("效期", batch?.expiry?.toString() ?: "未设置", Modifier.weight(1f))
                        OutboundFact("批次库存", "${batch?.quantity ?: 0} ${product?.unit.orEmpty()}", Modifier.weight(.9f), WarehousePalette.Teal)
                    }
                    if (batch != null) Text("${batch.location} · 其他待确认占用 ${(batch.quantity.toLong() - stock.batchFree)} ${product?.unit.orEmpty()} · 点击切换批次 ›",
                        color = WarehousePalette.Muted, fontSize = 11.sp,
                        modifier = Modifier.clickable(enabled = !locked && product != null) { choosingBatch = true }.padding(top = 9.dp))
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = WarehousePalette.Border)
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("出库数量", color = WarehousePalette.Muted, fontSize = 13.sp, modifier = Modifier.width(65.dp))
                        Box(Modifier.weight(1f)) { OutboundQuantity(quantity, product?.unit.orEmpty(), !locked) { quantity = it; quantityTouched = true } }
                    }
                    Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("出库原因", color = WarehousePalette.Muted, fontSize = 13.sp, modifier = Modifier.width(65.dp))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(WarehouseReplenishment.EFFECTIVE_REASON, "报损", "其他").forEach { choice ->
                                FilterChip(selected = reason == choice, onClick = { reason = choice }, enabled = !locked,
                                    label = { Text(choice, fontSize = 11.sp, color = if (reason == choice) Color.White else WarehousePalette.Muted) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WarehousePalette.Teal, selectedLabelColor = Color.White))
                            }
                        }
                    }
                    OutlinedTextField(destination, { destination = it; destinationTouched = true }, Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = !locked, singleLine = true, label = { Text("去向（仅记录去向）", fontSize = 12.sp) },
                        trailingIcon = { Icon(Icons.Default.Place, null, tint = WarehousePalette.Muted) }, shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(14.dp))
                    Surface(color = if (stock.error == null) WarehousePalette.Mint else Color(0xFFFFF1ED), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(WarehouseReceiveIcons.Stock, null, Modifier.size(38.dp), tint = Color.Unspecified)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("本次及待确认出库后 · 仓库合计剩余", fontSize = 11.sp, color = WarehousePalette.Muted)
                                Text("${stock.productRemaining} ${product?.unit.orEmpty()}", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                    color = if (stock.productRemaining < 0) WarehousePalette.Red else WarehousePalette.Teal)
                                Text("所选批次剩余 ${stock.batchRemaining} ${product?.unit.orEmpty()}", fontSize = 12.sp,
                                    color = if (stock.batchRemaining < 0) WarehousePalette.Red else WarehousePalette.TealDark)
                            }
                        }
                    }
                }
            }
            item { OutboundNotice("本次为待确认记录，不会直接扣减库存。加入列表后仍可继续编辑。") }
        }
    }
    if (choosingProduct) {
        var search by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { choosingProduct = false }, title = { Text("选择库存商品") }, text = {
            Column {
                OutlinedTextField(search, { search = it }, label = { Text("搜索商品") }, singleLine = true)
                LazyColumn(Modifier.heightIn(max = 330.dp)) {
                    items(state.products.filter { it.name.contains(search, ignoreCase = true) }, key = { it.id }) { choice ->
                        TextButton(onClick = {
                            selectedProduct = choice.id; selectedBatch = choice.sortedBatches.firstOrNull { it.available() }?.id
                            productTouched = true; batchTouched = true; choosingProduct = false
                        }) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(choice.name, color = WarehousePalette.Navy, fontWeight = FontWeight.Bold)
                                Text("库存 ${choice.availableQuantity} ${choice.unit}", color = WarehousePalette.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { choosingProduct = false }) { Text("关闭") } })
    }
    if (choosingBatch) AlertDialog(onDismissRequest = { choosingBatch = false }, title = { Text("选择可用批次") }, text = {
        LazyColumn(Modifier.heightIn(max = 350.dp)) {
            val choices = product?.sortedBatches?.filter { it.available() }.orEmpty()
            if (choices.isEmpty()) item { Text("没有可出库批次；已过期和零库存批次不可选。") }
            items(choices, key = { it.id }) { choice ->
                val free = outboundStock(state, selectedProduct, choice.id, 1, editingId).batchFree
                TextButton(onClick = { selectedBatch = choice.id; batchTouched = true; choosingBatch = false }) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(choice.code, color = WarehousePalette.Navy, fontWeight = FontWeight.Bold)
                        Text("${choice.expiry ?: "无效期"} · ${choice.location}", fontSize = 12.sp, color = WarehousePalette.Muted)
                        Text("扣除其他待确认后可用 $free ${product?.unit.orEmpty()}", fontSize = 12.sp, color = WarehousePalette.Teal)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { choosingBatch = false }) { Text("关闭") } })
}

@Composable
private fun OutboundFact(label: String, value: String, modifier: Modifier, color: Color = WarehousePalette.Navy) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = WarehousePalette.Muted, fontSize = 12.sp)
        Text(value, color = color, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
    }
}

private fun copyOutboundPhoto(context: android.content.Context, uri: Uri): String {
    val target = File(context.filesDir, "images/outbound_${UUID.randomUUID()}.jpg")
    try {
        target.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= 25_000_000L) { "照片过大" }
                    output.write(buffer, 0, count)
                }
                check(total > 0) { "照片为空" }
            }
        } ?: error("无法读取照片")
        return Uri.fromFile(target).toString()
    } catch (error: Throwable) {
        target.delete()
        throw error
    }
}
