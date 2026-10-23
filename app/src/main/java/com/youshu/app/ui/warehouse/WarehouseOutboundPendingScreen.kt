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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** All outbound drafts form one posting queue; checkboxes record review, not a selected subset. */
@Composable
internal fun WarehouseOutboundPendingScreen(
    state: WarehouseUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onReview: (StockDraftUi) -> Unit,
    onDelete: (String) -> Unit,
    onConfirm: (EntryDirection, (String?) -> Unit) -> Unit,
    onContinue: () -> Unit,
    onReload: () -> Unit,
    onReviewAll: (List<StockDraftUi>) -> Unit,
    onSaveDraft: (StockDraftUi, (String?) -> Unit) -> Unit
) {
    val drafts = state.drafts.filter { it.direction == EntryDirection.OUTBOUND }
    val quantities = remember { mutableStateMapOf<String, String>() }
    var savingId by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<StockDraftUi?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableStateOf(false) }
    val busy = submitting || savingId != null || state.pendingDraftWrite
    val reviewed = drafts.count { it.reviewed }
    val allReviewed = drafts.isNotEmpty() && reviewed == drafts.size
    val dirty = drafts.any { quantities[it.id]?.let { value -> value != it.quantity.toString() } == true }
    val queueError = outboundQueueError(state)
    val canMutate = !busy && state.persistenceError == null

    // Re-evaluate inside the dialog click, including state changes after the dialog opened.
    fun readyToConfirm(): Boolean {
        val current = state.drafts.filter { it.direction == EntryDirection.OUTBOUND }
        return !submitting && savingId == null && !state.pendingDraftWrite && state.persistenceError == null &&
            current.isNotEmpty() && current.all { it.reviewed } && outboundQueueError(state) == null &&
            current.none { quantities[it.id]?.let { value -> value != it.quantity.toString() } == true }
    }
    val canConfirm = readyToConfirm()
    BackHandler(enabled = busy) { }
    LaunchedEffect(drafts.map { it.id }) {
        val ids = drafts.map { it.id }.toSet()
        quantities.keys.toList().filter { it !in ids }.forEach { quantities.remove(it) }
    }

    Scaffold(containerColor = WarehousePalette.Background, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = Color.White, shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onContinue, enabled = !busy && !dirty,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF2F9F8), contentColor = WarehousePalette.TealDark)) {
                            Icon(Icons.Default.Add, null, Modifier.size(19.dp))
                            Text("继续录入", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { if (readyToConfirm()) confirming = true }, enabled = canConfirm,
                            modifier = Modifier.weight(1.55f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarehousePalette.Teal,
                                contentColor = Color.White, disabledContainerColor = Color(0xFFB7C0D1), disabledContentColor = Color.White)) {
                            Text(when {
                                submitting -> "正在确认…"
                                busy -> "正在保存…"
                                canConfirm -> "${if (state.previewMode) "模拟" else ""}确认出库（${drafts.size}项）"
                                else -> "核对全部后确认出库"
                            }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                    Text(when {
                        state.persistenceError != null -> "草稿同步失败，请先重新加载"
                        dirty -> "数量修改尚未保存，保存后需重新核对"
                        queueError != null -> "请先处理库存或必填信息异常"
                        drafts.isEmpty() -> "当前没有待确认的出库草稿"
                        !allReviewed -> "还有 ${drafts.size - reviewed} 项待核对 · 确认会处理全部草稿"
                        state.previewMode -> "示例预览 · 仅扣减本次预览库存并记录模拟流水"
                        else -> "全部已核对 · 确认后扣减库存并记录流水"
                    }, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 11.sp,
                        color = if (dirty || queueError != null || state.persistenceError != null) WarehousePalette.Orange else WarehousePalette.Muted)
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFAFDFC), WarehousePalette.Background)))
            .padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { OutboundHeader("待确认出库", { if (!busy) onBack() }) }
            item {
                OutboundPanel {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("待确认 ${drafts.size} 项 · 已核对 $reviewed 项", color = WarehousePalette.Navy,
                                fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(if (state.previewMode) "示例预览 · 不影响正式库存" else "确认后才扣减库存", color = WarehousePalette.Muted,
                                fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                        val canToggleAll = canMutate && !dirty && drafts.isNotEmpty() && (allReviewed || queueError == null)
                        Row(Modifier.toggleable(allReviewed, enabled = canToggleAll, role = Role.Checkbox,
                            onValueChange = { value ->
                                if (canToggleAll) {
                                    completed = false; message = null
                                    onReviewAll(state.drafts.filter { it.direction == EntryDirection.OUTBOUND }.map { it.copy(reviewed = value) })
                                }
                            }).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(allReviewed, onCheckedChange = null, enabled = canToggleAll,
                                colors = CheckboxDefaults.colors(checkedColor = WarehousePalette.Teal))
                            Text("全部核对", color = WarehousePalette.TealDark, fontSize = 12.sp,
                                modifier = Modifier.padding(start = 5.dp))
                        }
                    }
                    LinearProgressIndicator(progress = { if (drafts.isEmpty()) 0f else reviewed.toFloat() / drafts.size },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(5.dp),
                        color = WarehousePalette.Teal, trackColor = WarehousePalette.Mint)
                }
            }
            if (drafts.isNotEmpty()) item {
                if (dirty) OutboundNotice("有数量修改尚未保存，请保存并重新核对后再确认。", warning = true)
                else if (queueError != null) OutboundNotice(queueError, warning = true)
                else Surface(color = WarehousePalette.Mint, shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = WarehousePalette.Teal, modifier = Modifier.size(25.dp))
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("库存校验通过", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.TealDark)
                            Text("已计入全部待确认记录，确认前不会扣库存", fontSize = 11.sp, lineHeight = 17.sp, color = WarehousePalette.Muted)
                        }
                    }
                }
            }
            state.persistenceError?.let { error -> item {
                OutboundPanel {
                    Text(error, color = WarehousePalette.Red, fontSize = 13.sp, lineHeight = 21.sp)
                    TextButton(onClick = {
                        if (!busy) { quantities.clear(); completed = false; message = null; onReload() }
                    }, enabled = !busy) { Text("重新加载草稿", color = WarehousePalette.TealDark) }
                }
            } }
            message?.let { result -> item {
                OutboundNotice(result, warning = !completed)
            } }
            if (drafts.isEmpty()) item {
                OutboundPanel {
                    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AssignmentTurnedIn, null, tint = WarehousePalette.Teal,
                            modifier = Modifier.background(WarehousePalette.Mint, RoundedCornerShape(20.dp)).padding(16.dp).size(34.dp))
                        Text("暂无待确认出库", color = WarehousePalette.Navy, fontSize = 19.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 15.dp))
                        Text("拍照、语音或手动录入后，草稿会统一显示在这里。", color = WarehousePalette.Muted,
                            fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 7.dp))
                    }
                }
            }
            items(drafts, key = { it.id }) { draft ->
                val value = quantities[draft.id] ?: draft.quantity.toString()
                val changed = value != draft.quantity.toString()
                val stock = outboundStock(state, draft.productId, draft.batchId, value.toIntOrNull(), draft.id)
                val rowError = stock.error ?: when {
                    draft.reason.isBlank() -> "请填写出库原因"
                    draft.destination.isBlank() -> "请填写商品去向"
                    else -> null
                }
                OutboundPendingRow(draft, value, stock, rowError, changed, canMutate,
                    onQuantityChange = { if (canMutate) { quantities[draft.id] = it; completed = false; message = null } },
                    onSaveQuantity = {
                        val current = state.draft(draft.id)
                        val amount = value.toIntOrNull()
                        if (canMutate && current != null && changed && amount != null && amount > 0 &&
                            outboundStock(state, current.productId, current.batchId, amount, current.id).error == null) {
                            savingId = draft.id; completed = false; message = null
                            onSaveDraft(current.copy(quantity = amount, reviewed = false)) { error ->
                                savingId = null
                                if (error == null) {
                                    quantities.remove(draft.id)
                                    message = "数量已保存，请重新核对这条草稿"
                                } else message = error
                            }
                        }
                    }, onDiscardQuantity = { quantities.remove(draft.id) },
                    onReview = { valueReviewed ->
                        if (canMutate && !changed && (!valueReviewed || rowError == null)) {
                            completed = false; message = null; onReview(draft.copy(reviewed = valueReviewed))
                        }
                    }, onEdit = { if (canMutate && !changed) onEdit(draft.id) },
                    onDelete = { if (canMutate) deleting = draft })
            }
        }
    }
    deleting?.let { draft ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除这条出库草稿？") },
            text = { Text("${draft.productName}\n只删除草稿，不会扣减库存。") },
            confirmButton = { TextButton(enabled = canMutate, onClick = {
                if (canMutate) { completed = false; message = null; onDelete(draft.id); deleting = null }
            }) { Text("删除", color = WarehousePalette.Red) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } })
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text(if (state.previewMode) "模拟确认全部出库" else "确认全部出库") },
        text = { Text("将确认全部 ${drafts.size} 项已核对的出库草稿，扣减对应批次库存并记录流水。" +
            if (state.previewMode) "\n本次操作仅影响示例预览，正式库存不受影响。" else "\n确认前会再次校验库存。") },
        confirmButton = { TextButton(enabled = canConfirm, onClick = {
            if (readyToConfirm()) {
                submitting = true; confirming = false; message = null; completed = false
                onConfirm(EntryDirection.OUTBOUND) { error ->
                    submitting = false; completed = error == null
                    message = error ?: "${if (state.previewMode) "模拟" else ""}出库完成，库存和流水已更新"
                }
            }
        }) { Text(if (state.previewMode) "确认模拟" else "确认出库") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("再核对一下") } })
}

@Composable
private fun OutboundPendingRow(
    draft: StockDraftUi, quantity: String, stock: OutboundStock, error: String?, changed: Boolean, enabled: Boolean,
    onQuantityChange: (String) -> Unit, onSaveQuantity: () -> Unit, onDiscardQuantity: () -> Unit,
    onReview: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    OutboundPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            val reviewEnabled = enabled && !changed && (draft.reviewed || error == null)
            Box(Modifier.toggleable(draft.reviewed, enabled = reviewEnabled, role = Role.Checkbox, onValueChange = onReview)
                .size(36.dp), contentAlignment = Alignment.Center) {
                Checkbox(draft.reviewed, onCheckedChange = null, enabled = reviewEnabled,
                    colors = CheckboxDefaults.colors(checkedColor = WarehousePalette.Teal))
            }
            OutboundThumb(draft.imageUris.firstOrNull(), Modifier.size(64.dp))
            Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(draft.productName.ifBlank { "待核对商品" }, color = WarehousePalette.Navy,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                OutboundSourceBadge(draft.source)
                Text(if (draft.reviewed && !changed) "已核对" else "待核对", fontSize = 11.sp, lineHeight = 16.sp,
                    color = if (draft.reviewed && !changed) WarehousePalette.Teal else WarehousePalette.Orange)
            }
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("出库数量", color = WarehousePalette.Muted, fontSize = 12.sp, modifier = Modifier.width(65.dp))
            Box(Modifier.weight(1f)) { OutboundQuantity(quantity, draft.unit, enabled, onQuantityChange) }
        }
        if (changed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDiscardQuantity, enabled = enabled) { Text("取消修改", fontSize = 12.sp, color = WarehousePalette.Muted) }
                FilledTonalButton(onClick = onSaveQuantity, enabled = enabled && stock.error == null,
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = WarehousePalette.Mint, contentColor = WarehousePalette.TealDark)) {
                    Text("保存数量", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            HorizontalDivider(color = WarehousePalette.Border)
            OutboundPendingDetail("批次", draft.batchCode.ifBlank { "未选择" })
            OutboundPendingDetail("效期 / 库位", "${draft.expiry.ifBlank { "未设置" }} · ${draft.location}")
            OutboundPendingDetail("出库原因", draft.reason.ifBlank { "待填写" }, if (draft.reason == "报损") WarehousePalette.Orange else WarehousePalette.Teal)
            OutboundPendingDetail("去向", draft.destination.ifBlank { "待填写" } + "（仅记录去向）")
            HorizontalDivider(color = WarehousePalette.Border)
            OutboundPendingDetail("确认后余量", "${stock.productRemaining} ${draft.unit} · 本批次 ${stock.batchRemaining} ${draft.unit}",
                if (stock.batchRemaining < 0 || stock.productRemaining < 0) WarehousePalette.Red else WarehousePalette.Teal)
        }
        if (error != null) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.ErrorOutline, null, Modifier.padding(top = 2.dp).size(16.dp), tint = WarehousePalette.Red)
            Text(error, color = WarehousePalette.Red, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(start = 5.dp).weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDelete, enabled = enabled) {
                Icon(Icons.Default.DeleteOutline, "删除草稿 ${draft.productName}", Modifier.size(17.dp), tint = WarehousePalette.Muted)
                Text(" 删除", color = WarehousePalette.Muted, fontSize = 12.sp)
            }
            TextButton(onClick = onEdit, enabled = enabled && !changed) {
                Icon(Icons.Default.EditNote, "编辑草稿 ${draft.productName}", Modifier.size(18.dp), tint = WarehousePalette.TealDark)
                Text(" 编辑信息", color = WarehousePalette.TealDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OutboundPendingDetail(label: String, value: String, color: Color = WarehousePalette.Navy) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = WarehousePalette.Muted, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.width(88.dp))
        Text(value, color = color, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}
