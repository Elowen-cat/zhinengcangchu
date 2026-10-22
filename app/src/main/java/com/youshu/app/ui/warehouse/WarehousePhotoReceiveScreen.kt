package com.youshu.app.ui.warehouse

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WarehousePhotoReceiveScreen(
    state: WarehouseUiState,
    existing: StockDraftUi?,
    incomingPhotos: List<String>,
    onPhotosConsumed: () -> Unit,
    onBack: () -> Unit,
    onCamera: () -> Unit,
    onSaveDrafts: (List<StockDraftUi>, (String?) -> Unit) -> Unit,
    onSaved: () -> Unit,
    demoLines: List<PhotoReceiveLine> = emptyList(),
    onDemoConsumed: () -> Unit = {},
    holder: WarehousePhotoReceiveViewModel = hiltViewModel()
) {
    val lines by holder.lines.collectAsState()
    val copying by holder.copying.collectAsState()
    val saving by holder.saving.collectAsState()
    val message by holder.message.collectAsState()
    var remove by remember { mutableStateOf<PhotoReceiveLine?>(null) }
    var discard by remember { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        holder.addPhotos(uris.map { it.toString() })
    }
    LaunchedEffect(existing?.id) {
        holder.initialize(existing)
        if (state.previewMode && demoLines.isNotEmpty()) {
            holder.seedDemo(demoLines)
            onDemoConsumed()
        }
    }
    LaunchedEffect(incomingPhotos, saving) {
        if (incomingPhotos.isNotEmpty() && !saving) {
            holder.addPhotos(incomingPhotos)
            onPhotosConsumed()
        }
    }
    val back: () -> Unit = { if (!saving) { if (lines.isEmpty() && !copying) onBack() else discard = true } }
    BackHandler(enabled = saving || copying || lines.isNotEmpty(), onBack = back)
    val busy = lines.count { it.working }
    val incomplete = lines.count { it.validationError != null }
    val enabled = !saving && !copying
    Scaffold(containerColor = WarehousePalette.Background, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), shadowElevation = 2.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    message?.let { Text(it, color = WarehousePalette.Orange, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onCamera, enabled = enabled && lines.flatMap { it.attachments }.distinct().size < 12,
                            modifier = Modifier.weight(1f).heightIn(min = 54.dp), shape = RoundedCornerShape(17.dp)) {
                            Text("继续拍照", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { holder.save(state.products.toList(), onSaveDrafts, onSaved) },
                            enabled = enabled && busy == 0 && lines.isNotEmpty() && incomplete == 0,
                            modifier = Modifier.weight(1.8f).heightIn(min = 54.dp), shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarehousePalette.Teal)) {
                            Text(if (saving) "正在加入…" else "加入待确认（${lines.size}项）", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = if (enabled && busy == 0 && lines.isNotEmpty() && incomplete == 0) Color.White else WarehousePalette.Muted)
                        }
                    }
                    Text(when {
                        saving -> "正在保存本次草稿，请稍候"
                        copying -> "正在添加照片…"
                        busy > 0 -> "还有 $busy 张正在识别，可先查看已完成的结果"
                        lines.isEmpty() -> "先添加照片，识别结果会显示在这里"
                        incomplete > 0 -> "还有 $incomplete 项需补充；填写完整后可加入待确认"
                        else -> "将保存 ${lines.size} 项待确认草稿，不会直接改变库存"
                    }, color = WarehousePalette.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(
            Color(0xFFF8FDFD), Color(0xFFF0FAF9)))), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { WarehouseHeader("拍照录入", back) }
            item {
                Surface(color = WarehousePalette.Mint, shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(WarehouseReceiveIcons.Record, null, tint = Color.Unspecified, modifier = Modifier.size(34.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("识别结果为草稿，请核对后入库", color = WarehousePalette.TealDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("缺少的信息可手动补充；每项结果独立核对，确认单据后才改变库存。", color = WarehousePalette.Muted, fontSize = 12.sp)
                            if (state.previewMode) Text(if (lines.any { it.demo }) "界面预览 · 以下为示例识别结果" else "界面预览 · 不写入正式库存",
                                color = WarehousePalette.TealDark, fontSize = 11.sp)
                        }
                    }
                }
            }
            item { PhotoReceiveStrip(lines, enabled, onCamera, { gallery.launch("image/*") }, { remove = it }) }
            if (lines.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(when {
                                existing != null -> "编辑待确认 ${lines.size} 项"
                                lines.any { it.demo } -> "示例商品 ${lines.size} 项"
                                else -> "识别到 ${lines.count { it.recognized }} 个商品"
                            },
                                color = WarehousePalette.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            if (lines.any { it.error != null }) Text("未完成的照片可重试或手动补充", color = WarehousePalette.Muted, fontSize = 11.sp)
                        }
                        StatusTag("待人工核对 ${lines.size} 项", WarehousePalette.Orange)
                    }
                }
                items(lines, key = { it.id }) { line ->
                    PhotoReceiveResultCard(line, enabled, state.products.flatMap { it.batches }.map { it.location }.distinct(),
                        onEdit = { field, value -> holder.edit(line.id, field, value) }, onRetry = { holder.retry(line.id) })
                }
            }
        }
    }
    remove?.let { line ->
        AlertDialog(onDismissRequest = { remove = null }, title = { Text("移除这张照片？") },
            text = { Text("会同时移除本次录入中对应的商品结果，不会删除已有待确认单据或改变库存。") },
            confirmButton = { TextButton(onClick = { holder.delete(line.id); remove = null }, enabled = enabled) { Text("移除") } },
            dismissButton = { TextButton(onClick = { remove = null }) { Text("取消") } })
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("离开本次照片录入？") },
        text = { Text("尚未加入待确认的结果和修改将被放弃；原有库存与单据不受影响。") },
        confirmButton = { TextButton(onClick = { discard = false; onBack() }, enabled = !saving) { Text("离开") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("继续编辑") } })
}
