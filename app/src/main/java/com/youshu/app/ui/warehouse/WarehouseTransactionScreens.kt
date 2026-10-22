package com.youshu.app.ui.warehouse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.youshu.app.data.warehouse.WarehouseRecognitionFields
import com.youshu.app.util.WavAudioRecorder
import java.io.File
import java.time.LocalDate
import java.util.UUID

@Composable
fun WarehouseTransactionHomeScreen(
    direction: EntryDirection,
    state: WarehouseUiState,
    onBack: () -> Unit,
    onSource: (EntrySource) -> Unit,
    onPending: () -> Unit
) {
    val inbound = direction == EntryDirection.INBOUND
    if (inbound) {
        WarehouseReceiveHomeScreen(state, onBack, onSource, onPending)
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(WarehousePalette.Background), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { WarehouseHeader(if (inbound) "收货入库" else "出库作业", onBack) }
        item { WarehouseNotice(state.previewMode) }
        item {
            WarehouseCard {
                Column(Modifier.padding(20.dp)) {
                    Text(if (inbound) "录入 → 核对 → 入库" else "记录商品出库去向", color = WarehousePalette.TealDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("录入结果先保存为待确认草稿，核对并确认后才改变库存。", color = WarehousePalette.Muted, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item { EntryCard("拍照录入", "拍摄商品或包装，进入人工核对字段", Icons.Default.CameraAlt) { onSource(EntrySource.PHOTO) } }
        item { EntryCard("语音录入", "录音后转成文字；结果可编辑核对", Icons.Default.Mic) { onSource(EntrySource.VOICE) } }
        item { EntryCard("待确认", state.drafts.count { it.direction == direction }.toString() + if (state.previewMode) " 条预览草稿" else " 条待处理单据", Icons.Default.TaskAlt, onPending) }
        item { WarehouseAction("手动填写", { onSource(EntrySource.MANUAL) }) }
    }
}

@Composable
private fun EntryCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    WarehouseCard {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = WarehousePalette.Teal, modifier = Modifier.size(38.dp))
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, color = WarehousePalette.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = WarehousePalette.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = WarehousePalette.Muted)
        }
    }
}

@Composable
fun WarehouseStockFormScreen(
    state: WarehouseUiState,
    direction: EntryDirection,
    source: EntrySource,
    productId: String?,
    initialBatchId: String?,
    draftId: String?,
    photos: List<String>,
    onBack: () -> Unit,
    onCamera: () -> Unit,
    onSaveDraft: (StockDraftUi, (String?) -> Unit) -> Unit,
    onSaved: () -> Unit,
    recognitionViewModel: WarehouseRecognitionViewModel = hiltViewModel()
) {
    val existing = state.draft(draftId)
    val inbound = direction == EntryDirection.INBOUND
    var chosenProductId by rememberSaveable { mutableStateOf(existing?.productId ?: productId) }
    val product = state.product(chosenProductId)
    var batchId by rememberSaveable { mutableStateOf(existing?.batchId ?: initialBatchId ?: product?.sortedBatches?.firstOrNull { it.available() }?.id) }
    val batch = product?.batches?.firstOrNull { it.id == batchId }
    var name by rememberSaveable { mutableStateOf(existing?.productName ?: product?.name.orEmpty()) }
    var quantity by rememberSaveable { mutableStateOf(existing?.quantity?.toString() ?: if (inbound) "" else "1") }
    var unit by rememberSaveable { mutableStateOf(existing?.unit ?: product?.unit.orEmpty()) }
    var batchCode by rememberSaveable { mutableStateOf(existing?.batchCode.orEmpty()) }
    var expiry by rememberSaveable { mutableStateOf(existing?.expiry.orEmpty()) }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: product?.sortedBatches?.firstOrNull()?.location.orEmpty()) }
    var supplier by rememberSaveable { mutableStateOf(existing?.supplier.orEmpty()) }
    var price by rememberSaveable { mutableStateOf(existing?.purchasePrice.orEmpty()) }
    var reason by rememberSaveable { mutableStateOf(existing?.reason ?: WarehouseReplenishment.EFFECTIVE_REASON) }
    var destination by rememberSaveable { mutableStateOf(existing?.destination ?: "销售区") }
    var transcript by rememberSaveable { mutableStateOf(existing?.transcript.orEmpty()) }
    var chooseProduct by remember { mutableStateOf(false) }
    var chooseBatch by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val voiceRecorder = remember(context) { WavAudioRecorder(context.applicationContext) }
    var recordingVoice by remember { mutableStateOf(false) }
    var voiceStartedAt by remember { mutableStateOf(0L) }
    val voiceState by recognitionViewModel.voiceState.collectAsState()
    val transcriptRecognition by recognitionViewModel.transcriptState.collectAsState()
    val recognition by recognitionViewModel.state.collectAsState()
    var autoRequestedPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedPhotoResult by rememberSaveable { mutableStateOf<String?>(null) }
    var consumedVoiceTranscriptId by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedTranscriptResult by rememberSaveable { mutableStateOf<String?>(null) }
    DisposableEffect(voiceRecorder) {
        onDispose { voiceRecorder.cancel() }
    }

    fun startWarehouseVoice() {
        val started = runCatching { voiceRecorder.start() }.isSuccess
        if (started) {
            voiceStartedAt = System.currentTimeMillis()
            recordingVoice = true
        } else {
            saveMessage = "录音启动失败，请检查麦克风权限"
        }
    }
    val voicePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startWarehouseVoice() else saveMessage = "需要麦克风权限才能录音"
    }
    var galleryPhotos by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val allPhotos = (photos + galleryPhotos).distinct()
    val firstPhoto = allPhotos.firstOrNull()
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        val copied = selected.mapNotNull { copyWarehousePhoto(context, it) }
        galleryPhotos = (galleryPhotos + copied).distinct()
        if (selected.isNotEmpty() && copied.isEmpty()) saveMessage = "照片读取失败，请重新选择或手动录入"
    }
    fun applyRecognizedFields(fields: WarehouseRecognitionFields) {
        if (fields.isEmpty) return
        if (inbound) {
            fields.productName?.let { suggested ->
                name = suggested
                val match = state.products.firstOrNull { it.name.trim() == suggested.trim() }
                chosenProductId = match?.id
                if (match == null && fields.unit == null) unit = ""
                match?.let {
                    if (fields.unit == null) unit = it.unit
                }
            }
            fields.unit?.let { unit = it }
            fields.batchCode?.let { batchCode = it }
            fields.expiry?.let { expiry = it }
            fields.location?.let { location = it }
            fields.supplier?.let { supplier = it }
            fields.purchasePrice?.let { price = it }
        } else {
            fields.productName?.let { suggested ->
                val match = state.products.firstOrNull { it.name.trim() == suggested.trim() }
                chosenProductId = match?.id
                batchId = null
                match?.let {
                    batchId = if (fields.batchCode != null) {
                        it.batches.firstOrNull { batch -> batch.code == fields.batchCode && batch.available() }?.id
                    } else it.sortedBatches.firstOrNull { batch -> batch.available() }?.id
                }
            }
            if (fields.productName == null) fields.batchCode?.let { code ->
                batchId = state.product(chosenProductId)?.batches?.firstOrNull {
                    it.code == code && it.available()
                }?.id
            }
            fields.destination?.let { destination = it }
        }
        fields.quantity?.let { quantity = it.toString() }
        val missing = if (inbound) buildList {
            if (name.isBlank()) add("商品名称")
            if (quantity.isBlank()) add("数量")
            if (unit.isBlank()) add("基础单位")
            if (batchCode.isBlank()) add("批次")
            if (location.isBlank()) add("库位")
        } else buildList {
            if (state.product(chosenProductId) == null) add("库存商品")
            if (batchId == null) add("可用批次")
        }
        saveMessage = if (missing.isEmpty()) "字段已自动填入，请核对后保存草稿"
            else "已自动填入识别结果；还需核对或填写${missing.joinToString("、")}"
    }
    LaunchedEffect(firstPhoto, source, draftId) {
        if (source == EntrySource.PHOTO && draftId == null && firstPhoto != null && autoRequestedPhoto != firstPhoto) {
            autoRequestedPhoto = firstPhoto
            recognitionViewModel.recognizePhoto(direction, allPhotos)
        }
    }
    LaunchedEffect(recognition.resultId, firstPhoto) {
        if (recognition.resultId != null && recognition.resultId != appliedPhotoResult && recognition.sourcePhoto == firstPhoto) {
            appliedPhotoResult = recognition.resultId
            recognition.fields?.let(::applyRecognizedFields)
        }
    }
    LaunchedEffect(voiceState.transcriptId) {
        if (voiceState.transcriptId != null && voiceState.transcriptId != consumedVoiceTranscriptId) voiceState.transcript?.let { text ->
            consumedVoiceTranscriptId = voiceState.transcriptId
            transcript = text
            recognitionViewModel.interpretTranscript(direction, text)
        }
    }
    LaunchedEffect(transcriptRecognition.resultId, transcript) {
        if (transcriptRecognition.resultId != null && transcriptRecognition.resultId != appliedTranscriptResult &&
            transcriptRecognition.sourceTranscript == transcript.trim()) {
            appliedTranscriptResult = transcriptRecognition.resultId
            transcriptRecognition.fields?.let(::applyRecognizedFields)
        }
    }
    val amount = quantity.toIntOrNull()
    val reserved = state.drafts.filter { it.id != draftId && it.direction == EntryDirection.OUTBOUND && it.batchId == batch?.id && it.productId == product?.id }
        .sumOf { it.quantity.toLong() }
    val remaining = ((batch?.quantity ?: 0).toLong() - reserved - (amount ?: 0))
    val error = when {
        amount == null || amount <= 0 -> "请输入大于 0 的整数数量"
        inbound && name.isBlank() -> "请填写商品名称"
        inbound && unit.isBlank() -> "请填写基础单位"
        inbound && batchCode.isBlank() -> "请填写批次"
        inbound && location.isBlank() -> "请填写库位"
        inbound && expiry.isNotBlank() && runCatching { LocalDate.parse(expiry) }.isFailure -> "效期格式应为 YYYY-MM-DD"
        inbound && expiry.isNotBlank() && LocalDate.parse(expiry).isBefore(LocalDate.now()) -> "效期已过，请核对日期；当前不支持过期品入库"
        inbound && price.isNotBlank() && (price.toDoubleOrNull()?.let { !it.isFinite() || it < 0 } != false) -> "进价应为非负数"
        !inbound && (product == null || batch == null || !batch.available()) -> "请选择商品和可用批次"
        !inbound && remaining < 0 -> "包含其他待确认草稿后，数量超过当前批次可用库存"
        !inbound && destination.isBlank() -> "请填写去向"
        else -> null
    }
    Scaffold(
        containerColor = WarehousePalette.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(Modifier.background(WarehousePalette.Background).padding(16.dp)) {
                Text(saveMessage ?: error ?: "保存草稿不会改变库存", fontSize = 12.sp,
                    color = if (error != null || saveMessage != null) WarehousePalette.Orange else WarehousePalette.Teal, modifier = Modifier.padding(bottom = 8.dp))
                WarehouseAction(if (saving) "正在保存…" else "保存待确认草稿", enabled = error == null && !saving, onClick = {
                    saving = true
                    onSaveDraft(StockDraftUi(
                        id = existing?.id ?: UUID.randomUUID().toString(), direction = direction, source = source,
                        productId = if (inbound && name.trim() != product?.name) null else product?.id,
                        productName = if (inbound) name.trim() else product!!.name,
                        quantity = amount!!, unit = if (inbound) unit.trim() else product!!.unit,
                        batchId = if (inbound) null else batch!!.id, batchCode = if (inbound) batchCode.trim() else batch!!.code,
                        expiry = if (inbound) expiry else batch?.expiry?.toString().orEmpty(),
                        location = if (inbound) location.trim() else batch!!.location,
                        reason = if (inbound) "收货" else reason, destination = if (inbound) location.trim() else destination.trim(),
                        supplier = supplier.trim(), purchasePrice = price, imageUris = allPhotos, transcript = transcript, reviewed = false
                    )) { failure ->
                        saving = false
                        if (failure == null) onSaved() else saveMessage = failure
                    }
                })
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { WarehouseHeader((if (source == EntrySource.MANUAL) "手动" else source.label) + direction.label, onBack) }
            item { WarehouseNotice(state.previewMode) }
            if (source == EntrySource.PHOTO) {
                item {
                    WarehouseCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("添加照片后会自动识别第一张，并填入能确认的字段；请核对并补充缺失信息，确认单据后才改变库存。", color = WarehousePalette.Muted, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                allPhotos.take(2).forEach { uri -> AsyncImage(uri, "商品照片", Modifier.size(90.dp).padding(top = 10.dp)) }
                            }
                            Text("已附 ${allPhotos.size} 张照片", color = WarehousePalette.Teal, fontSize = 12.sp)
                            Row {
                                TextButton(onClick = onCamera) { Icon(Icons.Default.CameraAlt, null); Text("拍摄商品") }
                                TextButton(onClick = { galleryLauncher.launch("image/*") }) { Text("从相册选择") }
                            }
                            TextButton(
                                onClick = { recognitionViewModel.recognizePhoto(direction, allPhotos) },
                                enabled = allPhotos.isNotEmpty() && !recognition.working
                            ) { Text(if (recognition.working) "正在识别并填表…" else "重新识别第一张照片") }
                            if (recognition.sourcePhoto == allPhotos.firstOrNull()) {
                                recognition.message?.let {
                                    Text(it, color = WarehousePalette.Muted, fontSize = 12.sp)
                                }
                                recognition.fields?.let { fields ->
                                    val summary = listOfNotNull(
                                        fields.productName?.let { "商品 $it" },
                                        fields.quantity?.let { "数量 $it" },
                                        fields.unit?.let { "单位 $it" },
                                        fields.batchCode?.let { "批次 $it" },
                                        fields.expiry?.let { "效期 $it" },
                                        fields.location?.let { "库位 $it" },
                                        fields.destination?.let { "去向 $it" },
                                        fields.supplier?.let { "供应商 $it" },
                                        fields.purchasePrice?.let { "进价 $it" }
                                    ).joinToString(" · ")
                                    Text(summary.ifBlank { "没有提取到可靠字段，请手动填写" },
                                        color = WarehousePalette.Navy, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (source == EntrySource.VOICE) {
                item {
                    WarehouseCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("语音录入", color = WarehousePalette.Navy, fontWeight = FontWeight.Bold)
                            Text("直接说商品和你知道的信息，如“鲜牛奶 5 瓶，批次 A01”。语音转成文字后会自动提取并填表；不确定的字段留给你核对。录音最长 60 秒。",
                                color = WarehousePalette.Muted, fontSize = 13.sp)
                            TextButton(onClick = {
                                if (recordingVoice) {
                                    val file = voiceRecorder.stop()
                                    recordingVoice = false
                                    val elapsed = System.currentTimeMillis() - voiceStartedAt
                                    when {
                                        file == null || !file.exists() ||
                                            file.length() < WavAudioRecorder.MIN_AUDIO_BYTES -> {
                                            file?.delete()
                                            saveMessage = "录音太短，请再说一次"
                                        }
                                        elapsed > 60_000 || file.length() > 2_000_000 -> {
                                            file.delete()
                                            saveMessage = "录音超过 60 秒，请缩短后重试"
                                        }
                                        else -> recognitionViewModel.transcribeVoice(file.absolutePath)
                                    }
                                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                    PackageManager.PERMISSION_GRANTED) {
                                    startWarehouseVoice()
                                } else {
                                    voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }, enabled = !voiceState.working) {
                                Icon(Icons.Default.Mic, null)
                                Text(when {
                                    voiceState.working -> "正在识别…"
                                    recordingVoice -> "结束录音并识别"
                                    else -> "开始录音"
                                })
                            }
                            voiceState.message?.let { Text(it, color = WarehousePalette.Muted, fontSize = 12.sp) }
                            OutlinedTextField(transcript, { transcript = it }, Modifier.fillMaxWidth().padding(top = 10.dp),
                                label = { Text("识别文本（可编辑）") })
                            if (transcriptRecognition.working && transcriptRecognition.sourceTranscript == transcript.trim()) {
                                Text("正在理解文字并填入字段…", color = WarehousePalette.Teal, fontSize = 12.sp)
                            } else if (transcriptRecognition.sourceTranscript == transcript.trim()) {
                                transcriptRecognition.message?.let { Text(it, color = WarehousePalette.Muted, fontSize = 12.sp) }
                            }
                            TextButton(onClick = {
                                recognitionViewModel.interpretTranscript(direction, transcript)
                            }, enabled = transcript.isNotBlank() && !transcriptRecognition.working) {
                                Text("从文本填入字段")
                            }
                        }
                    }
                }
            }
            item {
                WarehouseCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (inbound) {
                            FormField("商品名称", name, { name = it })
                            FormField("基础单位", unit, { unit = it }, placeholder = "瓶、箱、盒")
                        } else {
                            Text("商品", color = WarehousePalette.Muted)
                            OutlinedButton(onClick = { chooseProduct = true }, modifier = Modifier.fillMaxWidth()) { Text(product?.name ?: "选择库存商品") }
                            product?.let { StockStatusTag(it.stockStatus) }
                            Text("批次（按效期优先推荐，请核对）", color = WarehousePalette.Muted, fontSize = 13.sp)
                            OutlinedButton(onClick = { chooseBatch = true }, enabled = product != null, modifier = Modifier.fillMaxWidth()) {
                                Text(batch?.code ?: "选择可用批次")
                            }
                            Text("效期：" + (batch?.expiry?.toString() ?: "未设置") + " · " + (batch?.location ?: "未选择库位"), color = WarehousePalette.Muted, fontSize = 12.sp)
                            Text("可用 " + (batch?.quantity ?: 0) + " " + product?.unit.orEmpty(),
                                color = product?.stockStatus?.accent ?: WarehousePalette.Muted, fontWeight = FontWeight.Bold)
                        }
                        FormField("数量", quantity, { quantity = it }, keyboardType = KeyboardType.Number)
                        if (inbound) {
                            FormField("批次", batchCode, { batchCode = it })
                            FormField("效期（可选）", expiry, { expiry = it }, placeholder = "YYYY-MM-DD")
                            FormField("库位", location, { location = it }, placeholder = "冷藏区 / A-02")
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(WarehouseReplenishment.EFFECTIVE_REASON, "报损", "其他").forEach { label ->
                                    FilterChip(reason == label, onClick = { reason = label; if (label == "报损") destination = "报损区" }, label = { Text(label) })
                                }
                            }
                            FormField("去向（仅记录去向）", destination, { destination = it })
                            Text("本组草稿出库后批次余量：" + remaining.coerceAtLeast(0) + " " + product?.unit.orEmpty(), color = WarehousePalette.Teal)
                            Text("报损不计入补货需求。", color = WarehousePalette.Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (inbound) {
                item {
                    WarehouseCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FormField("供应商（可选）", supplier, { supplier = it })
                            FormField("单件进价（可选）", price, { price = it }, keyboardType = KeyboardType.Decimal)
                        }
                    }
                }
            }
        }
    }
    if (chooseProduct) {
        AlertDialog(onDismissRequest = { chooseProduct = false }, title = { Text("选择商品") },
            text = {
                Column {
                    if (state.products.isEmpty()) Text("暂无正式库存，数据层接入后才能选择商品。")
                    state.products.forEach { choice ->
                        TextButton(onClick = {
                            chosenProductId = choice.id
                            batchId = choice.sortedBatches.firstOrNull { it.available() }?.id
                            chooseProduct = false
                        }) { Text(choice.name + " · " + choice.availableQuantity + " " + choice.unit + " · " + choice.stockStatus.label,
                            color = choice.stockStatus.accent) }
                    }
                }
            }, confirmButton = { TextButton(onClick = { chooseProduct = false }) { Text("关闭") } })
    }
    if (chooseBatch) {
        AlertDialog(onDismissRequest = { chooseBatch = false }, title = { Text("选择可用批次") },
            text = {
                Column {
                    val available = product?.sortedBatches?.filter { it.available() }.orEmpty()
                    if (available.isEmpty()) Text("没有可出库批次；过期和零库存批次不可选择。")
                    available.forEach { choice ->
                        TextButton(onClick = { batchId = choice.id; chooseBatch = false }) { Text(choice.code + " · " + choice.quantity + " " + product?.unit) }
                    }
                }
            }, confirmButton = { TextButton(onClick = { chooseBatch = false }) { Text("关闭") } })
    }
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit, placeholder: String = "", keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) },
        placeholder = { Text(placeholder) }, singleLine = true, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
}

private fun copyWarehousePhoto(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, "images").apply { mkdirs() }
    val target = File(dir, "warehouse_${UUID.randomUUID()}.jpg")
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return@runCatching null
        Uri.fromFile(target).toString()
    } catch (error: Exception) {
        target.delete()
        throw error
    }
}.getOrNull()

@Composable
fun WarehousePendingScreen(
    state: WarehouseUiState,
    direction: EntryDirection,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onReview: (StockDraftUi) -> Unit,
    onDelete: (String) -> Unit,
    onConfirm: (EntryDirection, (String?) -> Unit) -> Unit,
    onContinue: () -> Unit,
    onReload: () -> Unit = {},
    onReviewAll: (List<StockDraftUi>) -> Unit = {}
) {
    if (direction == EntryDirection.INBOUND) {
        WarehouseInboundPendingScreen(state, onBack, onEdit, onReview, onDelete, onConfirm, onContinue, onReload)
        return
    }
    val drafts = state.drafts.filter { it.direction == direction }
    val reviewed = drafts.count { it.reviewed }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val stockValid = drafts.groupBy { Pair(it.productId, it.batchId) }.all { (key, lines) ->
        val batch = state.product(key.first)?.batches?.firstOrNull { it.id == key.second }
        direction == EntryDirection.INBOUND || (batch != null && batch.available() && lines.sumOf { it.quantity.toLong() } <= batch.quantity)
    }
    Scaffold(containerColor = WarehousePalette.Background, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(Modifier.background(WarehousePalette.Background).padding(16.dp)) {
                Text(if (state.previewMode) "仅模拟本次预览中的库存变化，退出预览后不保留" else "确认后将更新本机库存并写入流水",
                    color = WarehousePalette.Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                WarehouseAction(
                    if (state.previewMode) "模拟确认" + direction.label else "确认" + direction.label,
                    { showConfirmation = true }, enabled = drafts.isNotEmpty() && reviewed == drafts.size && stockValid && !state.pendingDraftWrite && state.persistenceError == null
                )
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { WarehouseHeader("待确认" + direction.label, onBack) }
            item { WarehouseNotice(state.previewMode) }
            item {
                Text("待确认 " + drafts.size + " 项 · 已核对 " + reviewed + " 项", color = WarehousePalette.Teal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (!stockValid) Text("出库数量超过批次库存，请修改草稿", color = WarehousePalette.Red)
                state.persistenceError?.let {
                    Text(it, color = WarehousePalette.Red)
                    TextButton(onClick = onReload, enabled = !state.pendingDraftWrite) { Text("重新加载草稿") }
                }
                resultMessage?.let { Text(it, color = if (it.contains("完成")) WarehousePalette.Teal else WarehousePalette.Red) }
            }
            if (drafts.isEmpty()) item { WarehouseEmpty("暂无待确认草稿", "继续录入后，逐项核对商品、数量、批次和库位。") }
            items(drafts, key = { it.id }) { draft ->
                if (direction == EntryDirection.INBOUND) InboundPendingCard(draft,
                    onReview = { onReview(draft.copy(reviewed = it)) },
                    onEdit = { onEdit(draft.id) }, onDelete = { deletingId = draft.id })
                else WarehouseCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(draft.reviewed, { checked -> onReview(draft.copy(reviewed = checked)) }, enabled = !state.pendingDraftWrite)
                            Column(Modifier.weight(1f)) {
                                Text(draft.productName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WarehousePalette.Navy)
                                Text(draft.source.label + " · " + if (draft.reviewed) "已核对" else "待核对", color = WarehousePalette.Muted, fontSize = 12.sp)
                            }
                            IconButton(onClick = { onEdit(draft.id) }, enabled = !state.pendingDraftWrite) { Icon(Icons.Default.Edit, "编辑草稿") }
                            IconButton(onClick = { deletingId = draft.id }, enabled = !state.pendingDraftWrite) { Icon(Icons.Default.Delete, "删除草稿") }
                        }
                        Text("数量 " + draft.quantity + " " + draft.unit, fontWeight = FontWeight.Bold, color = WarehousePalette.Teal)
                        Text("批次 " + draft.batchCode, color = WarehousePalette.Navy, modifier = Modifier.padding(top = 8.dp))
                        Text("效期 " + draft.expiry.ifEmpty { "未设置" }, color = WarehousePalette.Muted)
                        Text("库位 " + draft.location, color = WarehousePalette.Muted)
                        if (direction == EntryDirection.OUTBOUND) {
                            Text("原因 " + draft.reason + " · 去向 " + draft.destination, color = WarehousePalette.Muted)
                            val batch = state.product(draft.productId)?.batches?.firstOrNull { it.id == draft.batchId }
                            val total = drafts.filter { it.batchId == draft.batchId && it.productId == draft.productId }.sumOf { it.quantity.toLong() }
                            Text("本组草稿后批次余量 " + ((batch?.quantity ?: 0).toLong() - total) + " " + draft.unit, color = WarehousePalette.Teal)
                        }
                    }
                }
            }
            if (drafts.isNotEmpty()) item {
                TextButton(onClick = { onReviewAll(drafts.map { it.copy(reviewed = reviewed != drafts.size) }) }, enabled = !state.pendingDraftWrite) {
                    Text(if (reviewed == drafts.size) "取消全部核对" else "全部标记已核对")
                }
            }
            item { WarehouseAction("继续录入", onContinue) }
        }
    }
    deletingId?.let { id ->
        AlertDialog(onDismissRequest = { deletingId = null }, title = { Text("删除这条界面草稿？") },
            text = { Text("删除草稿不会改变库存。") },
            confirmButton = { TextButton(onClick = { onDelete(id); deletingId = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("取消") } })
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text((if (state.previewMode) "模拟确认" else "确认") + direction.label) },
            text = { Text(if (state.previewMode) "将更新界面预览中的样例库存和流水。退出预览后，本次模拟不会保留。"
                else "将一次性更新批次库存、单据状态和库存流水。请确认商品、数量、批次与去向。") },
            confirmButton = { TextButton(onClick = {
                onConfirm(direction) { error ->
                    resultMessage = error ?: "${if (state.previewMode) "模拟" else ""}${direction.label}完成，库存和流水已更新"
                }
                showConfirmation = false
            }) { Text(if (state.previewMode) "确认模拟" else "确认记账") } },
            dismissButton = { TextButton(onClick = { showConfirmation = false }) { Text("取消") } }
        )
    }
}
