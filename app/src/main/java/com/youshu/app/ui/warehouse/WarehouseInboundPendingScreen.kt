package com.youshu.app.ui.warehouse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate

/** Photo, voice and manual intake share the same pending documents and posting transaction. */
@Composable
internal fun WarehouseInboundPendingScreen(
    state: WarehouseUiState, onBack: () -> Unit, onEdit: (String) -> Unit,
    onReview: (StockDraftUi) -> Unit, onDelete: (String) -> Unit,
    onConfirm: (EntryDirection, (String?) -> Unit) -> Unit, onContinue: () -> Unit,
    onReload: () -> Unit
) {
    val drafts = state.drafts.filter { it.direction == EntryDirection.INBOUND }
    val reviewed = drafts.count { it.reviewed }
    val remaining = drafts.size - reviewed
    var deleting by remember { mutableStateOf<StockDraftUi?>(null) }
    var confirming by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val busy = submitting || state.pendingDraftWrite
    val canConfirm = drafts.isNotEmpty() && remaining == 0 && !busy && state.persistenceError == null
    BackHandler(enabled = submitting) { }
    Scaffold(containerColor = WarehousePalette.Background, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onContinue, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEDF8F8), contentColor = WarehousePalette.TealDark)) {
                            Icon(Icons.Default.NoteAdd, null, Modifier.size(20.dp))
                            Text(" 继续录入", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { if (canConfirm) confirming = true }, enabled = canConfirm,
                            modifier = Modifier.weight(1.55f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarehousePalette.Teal, contentColor = Color.White,
                                disabledContainerColor = Color(0xFFB7C0D1), disabledContentColor = Color.White)) {
                            Text(when {
                                submitting -> "正在确认…"
                                state.pendingDraftWrite -> "正在保存…"
                                canConfirm -> "${if (state.previewMode) "模拟" else ""}确认入库（${drafts.size}项）"
                                else -> "全部核对后确认入库"
                            }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                    Text(when {
                        state.persistenceError != null -> "草稿尚未同步成功，请重新加载后再确认"
                        remaining > 0 -> "还有 $remaining 项信息待核对"
                        drafts.isEmpty() -> "拍照或语音录入后，在这里逐项核对"
                        state.previewMode -> "示例预览 · 确认仅改变本次预览库存"
                        else -> "已全部核对，确认后更新库存与流水"
                    }, color = if (remaining > 0) WarehousePalette.Orange else WarehousePalette.Muted,
                        fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFAFDFE), WarehousePalette.Background)))
            .padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { WarehouseHeader("待确认入库", { if (!busy) onBack() }) }
            item {
                Surface(color = Color(0xFFFFF8ED), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = WarehousePalette.Orange, modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("识别结果不会自动入库，请逐项核对", color = WarehousePalette.TealDark,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(if (state.previewMode) "示例数据 · 不代表正式库存" else "图片、语音和手动录入的草稿统一在此确认",
                                color = WarehousePalette.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                }
            }
            item { PendingReviewProgress(drafts.size, reviewed) }
            state.persistenceError?.let { error -> item {
                Surface(color = Color(0xFFFFF2F1), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(error, color = WarehousePalette.Red, fontSize = 13.sp)
                        TextButton(onClick = onReload, enabled = !busy) { Text("重新加载草稿") }
                    }
                }
            } }
            message?.let { result -> item {
                Text(result, color = if (result.contains("完成")) WarehousePalette.Teal else WarehousePalette.Red,
                    fontSize = 14.sp, modifier = Modifier.padding(4.dp))
            } }
            if (drafts.isEmpty()) item {
                Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AssignmentTurnedIn, null, tint = WarehousePalette.Teal,
                            modifier = Modifier.background(WarehousePalette.Mint, RoundedCornerShape(22.dp)).padding(18.dp).size(38.dp))
                        Text("暂无待确认的收货草稿", color = WarehousePalette.Navy, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                        Text("用照片或语音记录商品，识别后会集中显示在这里。", color = WarehousePalette.Muted,
                            fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            items(drafts, key = { it.id }) { draft ->
                InboundPendingCard(draft, enabled = !busy,
                    onReview = { if (!busy) { message = null; onReview(draft.copy(reviewed = it)) } },
                    onEdit = { if (!busy) onEdit(draft.id) }, onDelete = { if (!busy) deleting = draft })
            }
        }
    }
    deleting?.let { draft ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除这条待确认草稿？") },
            text = { Text("${draft.productName}\n删除草稿不会改变库存。") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                if (!busy) { message = null; onDelete(draft.id); deleting = null }
            }) { Text("删除", color = WarehousePalette.Red) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } })
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text(if (state.previewMode) "模拟确认入库" else "确认入库") },
        text = { Text(if (state.previewMode) "将把 ${drafts.size} 项已核对示例草稿加入预览库存并记录模拟流水，正式库存不受影响。"
            else "已核对 ${drafts.size} 项收货草稿。确认后将更新本机库存并记录流水。") },
        confirmButton = { TextButton(enabled = canConfirm, onClick = {
            if (canConfirm) {
                submitting = true; confirming = false
                onConfirm(EntryDirection.INBOUND) { error ->
                    submitting = false
                    message = error ?: "${if (state.previewMode) "模拟" else ""}入库完成，库存和流水已更新"
                }
            }
        }) { Text(if (state.previewMode) "确认模拟" else "确认入库") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("再核对一下") } })
}

@Composable
private fun PendingReviewProgress(total: Int, reviewed: Int) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReviewCount("待确认", total, WarehousePalette.Navy)
                ReviewCount("已核对", reviewed, WarehousePalette.Teal)
                ReviewCount("还需核对", total - reviewed, WarehousePalette.Orange)
            }
            LinearProgressIndicator(progress = if (total == 0) 0f else reviewed.toFloat() / total,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp).clip(RoundedCornerShape(8.dp)),
                color = WarehousePalette.Teal, trackColor = Color(0xFFE7EDF5))
        }
    }
}

@Composable
private fun ReviewCount(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 12.sp, color = WarehousePalette.Muted)
        Text("$count", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = color)
        Text("项", fontSize = 12.sp, color = WarehousePalette.Muted)
    }
}

@Composable
internal fun InboundPendingCard(draft: StockDraftUi, enabled: Boolean = true,
    onReview: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var showPurchase by remember { mutableStateOf(false) }
    val sourceIcon = when (draft.source) { EntrySource.PHOTO -> Icons.Default.CameraAlt; EntrySource.VOICE -> Icons.Default.Mic; EntrySource.MANUAL -> Icons.Default.EditNote }
    val sourceLabel = when (draft.source) { EntrySource.PHOTO -> "图片录入"; EntrySource.VOICE -> "语音录入"; EntrySource.MANUAL -> "手动录入" }
    val date = draft.expiry.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val invalidDate = draft.expiry.isNotBlank() && (date == null || date.isBefore(LocalDate.now()))
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.background(Color(0xFFEDF3FF), RoundedCornerShape(11.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(sourceIcon, null, tint = WarehousePalette.Blue, modifier = Modifier.size(19.dp))
                    Text(" $sourceLabel", color = WarehousePalette.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.toggleable(draft.reviewed, enabled = enabled, role = Role.Checkbox, onValueChange = onReview)
                    .heightIn(min = 48.dp).padding(start = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(draft.reviewed, onCheckedChange = null, enabled = enabled,
                        colors = CheckboxDefaults.colors(checkedColor = WarehousePalette.Teal, uncheckedColor = Color(0xFFA3AEC4)))
                    Text(if (draft.reviewed) "已核对" else "待核对", fontSize = 13.sp,
                        color = if (draft.reviewed) WarehousePalette.TealDark else WarehousePalette.Muted,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
                }
                Box {
                    IconButton(onClick = { menu = true }, enabled = enabled, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, "更多操作 ${draft.productName}", tint = WarehousePalette.Muted, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("删除草稿", color = WarehousePalette.Red) }, enabled = enabled,
                            onClick = { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = WarehousePalette.Red) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                if (draft.imageUris.isNotEmpty()) AsyncImage(draft.imageUris.first(), "${draft.productName}照片",
                    Modifier.size(72.dp).clip(RoundedCornerShape(15.dp)).background(WarehousePalette.Background), contentScale = ContentScale.Crop)
                else Box(Modifier.size(72.dp).background(Color(0xFFEDF7F7), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                    Icon(if (draft.source == EntrySource.VOICE) WarehouseReceiveIcons.Voice else WarehouseReceiveIcons.Stock,
                        null, tint = Color.Unspecified, modifier = Modifier.size(43.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(draft.productName, modifier = Modifier.weight(1f), lineHeight = 24.sp,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy)
                        TextButton(onClick = onEdit, enabled = enabled, shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFFEDF8F7), contentColor = WarehousePalette.TealDark)) {
                            Icon(Icons.Default.Edit, "编辑草稿 ${draft.productName}", Modifier.size(16.dp))
                            Text(" 编辑", color = WarehousePalette.TealDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    PendingDetail("数量", "${draft.quantity} ${draft.unit}")
                    PendingDetail("批次", draft.batchCode.ifBlank { "未填写" })
                    if (draft.expiry.isNotBlank()) {
                        PendingDetail("效期", draft.expiry, color = if (!draft.reviewed || invalidDate) WarehousePalette.Orange else WarehousePalette.Navy,
                            needsReview = !draft.reviewed && !invalidDate)
                        if (invalidDate) Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, Modifier.size(14.dp), tint = WarehousePalette.Orange)
                            Text(" 效期异常，请编辑核对", color = WarehousePalette.Orange, fontSize = 11.sp, lineHeight = 17.sp)
                        }
                    }
                    PendingDetail("库位", draft.location.ifBlank { "未填写" })
                    if (draft.supplier.isNotBlank() || draft.purchasePrice.isNotBlank()) {
                        TextButton(onClick = { showPurchase = !showPurchase }, contentPadding = PaddingValues(0.dp)) {
                            Text(if (showPurchase) "收起供应商与进价" else "查看供应商与进价", color = WarehousePalette.Muted, fontSize = 11.sp)
                            Icon(if (showPurchase) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp), tint = WarehousePalette.Muted)
                        }
                        if (showPurchase) {
                            if (draft.supplier.isNotBlank()) PendingDetail("供应商", draft.supplier)
                            if (draft.purchasePrice.isNotBlank()) PendingDetail("进价", "¥${draft.purchasePrice} / ${draft.unit}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingDetail(label: String, value: String, color: Color = WarehousePalette.Navy, needsReview: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = WarehousePalette.Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.width(if (label.length > 2) 46.dp else 38.dp))
        Text(value, color = color, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
        if (needsReview) Icon(Icons.Default.ErrorOutline, "效期请核对", tint = WarehousePalette.Orange,
            modifier = Modifier.padding(start = 3.dp).size(17.dp))
    }
}
