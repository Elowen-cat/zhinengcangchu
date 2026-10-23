package com.youshu.app.ui.warehouse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun WarehouseOutboundHomeScreen(
    state: WarehouseUiState,
    onBack: () -> Unit,
    onSource: (EntrySource) -> Unit,
    onPending: () -> Unit,
    onEdit: (String) -> Unit = { onPending() }
) {
    val drafts = state.drafts.filter { it.direction == EntryDirection.OUTBOUND }
    val enabled = !state.pendingDraftWrite
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color(0xFFF8FDFC), Color(0xFFEDF8F7), WarehousePalette.Background))),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { OutboundHeader("出库作业", { if (enabled) onBack() }) }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = Color.Transparent,
                border = BorderStroke(1.dp, Color.White)) {
                Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                    Color(0xFFDCF5EE), Color(0xFFF5FCFA)))).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("记录商品出库去向", color = WarehousePalette.TealDark,
                            fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
                        Text("拍照或说一说，自动识别商品\n核对数量与去向，确认后扣减库存",
                            color = WarehousePalette.Muted, fontSize = 13.sp, lineHeight = 22.sp)
                        Text(if (state.previewMode) "界面预览 · 示例数据，不代表正式库存" else "本地仓库 · 草稿保存后仍需核对",
                            color = WarehousePalette.TealDark, fontSize = 11.sp, lineHeight = 18.sp)
                    }
                    Icon(WarehouseDashboardIcons.Dispatch, null, tint = Color.Unspecified,
                        modifier = Modifier.padding(start = 10.dp).size(64.dp))
                }
            }
        }
        item {
            OutboundHomeEntry("拍照出库", "拍摄商品，识别后选择库存批次", WarehouseReceiveIcons.Camera,
                WarehousePalette.Teal, enabled = enabled, onClick = { onSource(EntrySource.PHOTO) })
        }
        item {
            OutboundHomeEntry("语音出库", "说出商品、数量、出库原因与去向", WarehouseReceiveIcons.Voice,
                WarehousePalette.Blue, enabled = enabled, onClick = { onSource(EntrySource.VOICE) })
        }
        item {
            OutboundHomeEntry("待确认出库", "逐项核对后，一次确认全部草稿", WarehouseReceiveIcons.Review,
                WarehousePalette.Orange, count = drafts.size, enabled = enabled, onClick = onPending)
        }
        item {
            OutlinedButton(onClick = { onSource(EntrySource.MANUAL) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, WarehousePalette.Teal.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarehousePalette.TealDark,
                    containerColor = Color.White.copy(alpha = 0.75f))) {
                Icon(Icons.Default.EditNote, null, Modifier.size(21.dp))
                Text(" 手动填写出库单", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            OutboundPanel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("最近待处理", color = WarehousePalette.Navy, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onPending, enabled = enabled, contentPadding = PaddingValues(start = 8.dp)) {
                        Text("查看全部", color = WarehousePalette.Muted, fontSize = 12.sp)
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = WarehousePalette.Muted)
                    }
                }
                if (drafts.isEmpty()) {
                    Text("暂无待处理的出库草稿", color = WarehousePalette.Muted, fontSize = 14.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    Text("从拍照、语音或手动填写开始，保存后会显示在这里。", color = WarehousePalette.Muted,
                        fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                } else drafts.takeLast(3).asReversed().forEachIndexed { index, draft ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = WarehousePalette.Border.copy(alpha = 0.65f))
                    Surface(onClick = { onEdit(draft.id) }, enabled = enabled, color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutboundThumb(draft.imageUris.firstOrNull(), Modifier.size(58.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(draft.productName.ifBlank { "待核对商品" }, color = WarehousePalette.Navy,
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
                                OutboundSourceBadge(draft.source)
                                Text("${draft.reason.ifBlank { "原因待填写" }} · ${draft.destination.ifBlank { "去向待填写" }}",
                                    color = WarehousePalette.Muted, fontSize = 12.sp, lineHeight = 19.sp)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${draft.quantity} ${draft.unit}", color = WarehousePalette.Blue,
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(if (draft.reviewed) "已核对" else "待核对",
                                    color = if (draft.reviewed) WarehousePalette.Teal else WarehousePalette.Orange, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        item { OutboundNotice("保存草稿不会扣减库存。请在待确认页核对全部商品后，再确认出库。") }
    }
}

@Composable
private fun OutboundHomeEntry(title: String, subtitle: String, icon: ImageVector, accent: Color,
    count: Int? = null, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
        color = Color.White, border = BorderStroke(1.dp, WarehousePalette.Border.copy(alpha = 0.7f))) {
        Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color.White, accent.copy(alpha = 0.035f))))
            .padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(66.dp).background(accent.copy(alpha = 0.08f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.Unspecified, modifier = Modifier.size(43.dp))
            }
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = WarehousePalette.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = WarehousePalette.Muted, fontSize = 12.sp, lineHeight = 20.sp)
            }
            if (count != null) Text("$count", color = if (count > 0) Color.White else WarehousePalette.Muted,
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(if (count > 0) WarehousePalette.Orange else WarehousePalette.Border,
                    RoundedCornerShape(20.dp)).padding(horizontal = 9.dp, vertical = 4.dp))
            Icon(Icons.Default.ChevronRight, null, tint = WarehousePalette.Muted, modifier = Modifier.size(23.dp))
        }
    }
}
