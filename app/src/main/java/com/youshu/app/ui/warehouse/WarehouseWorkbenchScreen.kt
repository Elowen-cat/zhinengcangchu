package com.youshu.app.ui.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WorkbenchPreviewData(
    val workspace: String,
    val expiring: String,
    val lowStock: String,
    val pending: String,
    val todayOutbound: String,
    val tasks: List<PreviewTask>
)

data class PreviewTask(val name: String, val note: String, val status: WarehouseStockStatus, val urgent: Boolean)

private val previewData = WorkbenchPreviewData(
    workspace = "青禾便利店 · 后仓",
    expiring = "08",
    lowStock = "03",
    pending = "02",
    todayOutbound = "126件",
    tasks = listOf(
        PreviewTask("鲜牛奶 250ml", "3 天后到期", WarehouseStockStatus.EXPIRING, true),
        PreviewTask("苏打饼干", "低于安全库存", WarehouseStockStatus.LOW, true)
    )
)

@Composable
fun WarehouseWorkbenchScreen(
    onOpenInventory: (StockFilter) -> Unit,
    onReceive: () -> Unit,
    onOutbound: () -> Unit,
    onCount: () -> Unit,
    onPending: (EntryDirection) -> Unit,
    inboundDraftCount: Int = 0,
    outboundDraftCount: Int = 0,
    recentMovements: List<WarehouseMovementUi> = emptyList(),
    previewMode: Boolean = false,
    onUiPreview: (() -> Unit)? = null,
    onExitPreview: (() -> Unit)? = null,
    data: WorkbenchPreviewData? = null
) {
    val tasks = data?.tasks.orEmpty()
    val pendingCount = tasks.size + inboundDraftCount + outboundDraftCount
    WarehousePage(title = "工作台") {
        item {
            Column {
                WarehouseCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warehouse, contentDescription = null, tint = WarehousePalette.Teal,
                            modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(data?.workspace ?: "本地仓库", color = WarehousePalette.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("单仓模式 · 当前设备", color = WarehousePalette.Muted, fontSize = 12.sp)
                        }
                    }
                }
                Row(Modifier.padding(start = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = WarehousePalette.Teal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (previewMode) "界面预览 · 示例数据不会写入正式库存" else "本地仓库 · 数据保存在当前设备",
                        color = WarehousePalette.TealDark, fontSize = 12.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("临期", data?.expiring ?: "—", WarehouseDashboardIcons.Expiring, WarehousePalette.Orange, Modifier.weight(1f)) { onOpenInventory(StockFilter.NEAR) }
                MetricCard("低库存", data?.lowStock ?: "—", WarehouseDashboardIcons.LowStock, WarehouseStockStatus.LOW.accent, Modifier.weight(1f)) { onOpenInventory(StockFilter.LOW) }
                MetricCard("待收货", data?.pending ?: "—", WarehouseDashboardIcons.Delivery, WarehousePalette.Blue, Modifier.weight(1f), onReceive)
                MetricCard("今日出库", data?.todayOutbound ?: "—", WarehouseDashboardIcons.OutboundTrend, WarehousePalette.Teal, Modifier.weight(1f), onOutbound)
            }
        }
        item {
            WarehouseCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).background(WarehousePalette.Orange, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Assignment, null, tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("待处理（$pendingCount）", color = WarehousePalette.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (tasks.isEmpty()) {
                        Text(if (pendingCount == 0) "暂无待处理事项" else "$pendingCount 条草稿等待核对",
                            color = WarehousePalette.Muted, fontSize = 13.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tasks.forEach { task ->
                                PendingTaskCard(task) {
                                    onOpenInventory(task.status.filter)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DraftShortcut("待确认入库", inboundDraftCount, WarehouseDashboardIcons.Receive, WarehousePalette.Teal,
                            Modifier.weight(1f)) { onPending(EntryDirection.INBOUND) }
                        DraftShortcut("待确认出库", outboundDraftCount, WarehouseDashboardIcons.Dispatch, WarehousePalette.Blue,
                            Modifier.weight(1f)) { onPending(EntryDirection.OUTBOUND) }
                    }
                }
            }
        }
        item {
            WarehouseCard {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("快捷操作", "")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile("收货", WarehouseDashboardIcons.Receive, WarehousePalette.Teal, Modifier.weight(1f), onReceive)
                        ActionTile("出库", WarehouseDashboardIcons.Dispatch, WarehousePalette.Blue, Modifier.weight(1f), onOutbound)
                        ActionTile("盘点", WarehouseDashboardIcons.Count, Color(0xFF7E4B9D), Modifier.weight(1f), onCount)
                    }
                    Text(if (previewMode) "核对草稿后可模拟确认，数据仅在本次预览中有效" else "核对草稿后确认记账，库存与流水同步更新",
                        color = WarehousePalette.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        item {
            WarehouseCard {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    SectionTitle("最近出库", if (previewMode) "预览流水" else "本地流水")
                    val outbound = recentMovements.filter { it.direction == EntryDirection.OUTBOUND }.sortedByDescending { it.time }.take(3)
                    if (outbound.isEmpty()) {
                        Text(if (previewMode) "暂无模拟出库，可从上方创建并确认草稿" else "暂无出库记录",
                            color = WarehousePalette.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                    } else {
                        outbound.forEach { movement ->
                            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(movement.productName, color = WarehousePalette.Navy, fontWeight = FontWeight.SemiBold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(movement.note, color = WarehousePalette.Muted, fontSize = 12.sp,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("−${movement.quantity} ${movement.unit}", color = WarehousePalette.Blue, fontWeight = FontWeight.Bold)
                                    Text(formatMovementTime(movement.time), color = WarehousePalette.Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            WarehouseCard {
                Row(Modifier.fillMaxWidth().clickable { onOpenInventory(StockFilter.ALL) }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("查看库存页面", color = WarehousePalette.Navy, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = WarehousePalette.Muted, modifier = Modifier.size(16.dp))
                }
            }
        }
        onUiPreview?.let { openPreview ->
            item { WarehouseAction("界面预览（示例，不入账）", openPreview) }
        }
        onExitPreview?.let { closePreview ->
            item { WarehouseAction("退出界面预览", closePreview) }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(128.dp), shape = RoundedCornerShape(22.dp),
        color = Color.Transparent, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f))) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color.White, accent.copy(alpha = 0.08f)))).padding(6.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(8.dp))
                Text(label, color = WarehousePalette.Navy, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(value, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PendingTaskCard(task: PreviewTask, onClick: () -> Unit) {
    val status = task.status
    val colors = listOf(status.background, status.backgroundEnd)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = Color.Transparent, border = BorderStroke(1.dp, status.border)) {
        Row(Modifier.background(Brush.linearGradient(colors)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center) {
                Icon(when (status) {
                    WarehouseStockStatus.OUT_OF_STOCK -> WarehouseDashboardIcons.OutOfStock
                    WarehouseStockStatus.EXPIRING -> WarehouseDashboardIcons.Expiring
                    else -> WarehouseDashboardIcons.LowStock
                },
                    null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(task.name, color = WarehousePalette.Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(task.note, color = when {
                    status == WarehouseStockStatus.OUT_OF_STOCK -> status.accent
                    status == WarehouseStockStatus.EXPIRING && task.urgent -> WarehousePalette.Red
                    else -> WarehousePalette.Muted
                },
                    fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            }
            StockStatusTag(status)
            Icon(Icons.Default.ChevronRight, null,
                tint = if (status == WarehouseStockStatus.OUT_OF_STOCK) status.accent else WarehousePalette.Muted,
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DraftShortcut(label: String, count: Int, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = 0.05f)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(label, color = WarehousePalette.Navy, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("$count 条草稿", color = accent, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = WarehousePalette.Muted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = WarehousePalette.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(trailing, color = WarehousePalette.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(100.dp), shape = RoundedCornerShape(20.dp), color = Color.Transparent) {
        Box(Modifier.background(Brush.linearGradient(listOf(accent.copy(alpha = 0.08f), accent.copy(alpha = 0.14f)))),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(40.dp))
                Text(label, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

private fun formatMovementTime(time: LocalDateTime): String {
    val today = LocalDate.now()
    val clock = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (time.toLocalDate()) {
        today -> "今天 $clock"
        today.minusDays(1) -> "昨天 $clock"
        else -> time.format(DateTimeFormatter.ofPattern(if (time.year == today.year) "MM-dd HH:mm" else "yyyy-MM-dd HH:mm"))
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun WarehouseWorkbenchPreview() {
    WarehouseWorkbenchScreen(onOpenInventory = {}, onReceive = {}, onOutbound = {}, onCount = {}, onPending = {}, data = previewData)
}
