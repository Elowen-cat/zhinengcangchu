package com.youshu.app.ui.warehouse

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WorkbenchPreviewData(
    val workspace: String,
    val expiring: String,
    val lowStock: String,
    val pending: String,
    val todayOutbound: String,
    val tasks: List<PreviewTask>
)

data class PreviewTask(val name: String, val note: String, val status: String, val urgent: Boolean)

private val previewData = WorkbenchPreviewData(
    workspace = "青禾便利店 · 后仓",
    expiring = "08",
    lowStock = "03",
    pending = "02",
    todayOutbound = "126件",
    tasks = listOf(
        PreviewTask("鲜牛奶 250ml", "3 天后到期", "临期", true),
        PreviewTask("苏打饼干", "低于安全库存", "低库存", true)
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
    WarehousePage(title = "工作台") {
        item {
            Column {
                WarehouseCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = WarehousePalette.Teal)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(data?.workspace ?: "本地仓库", color = WarehousePalette.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("单仓模式 · 当前设备", color = WarehousePalette.Muted, fontSize = 12.sp)
                        }
                    }
                }
                Text(
                    text = if (previewMode) "界面预览 · 示例数据不会写入正式库存" else "本地仓库 · 数据保存在当前设备",
                    color = WarehousePalette.TealDark,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 10.dp)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("临期", data?.expiring ?: "—", Icons.Default.WarningAmber, WarehousePalette.Orange, Modifier.weight(1f)) { onOpenInventory(StockFilter.NEAR) }
                MetricCard("低库存", data?.lowStock ?: "—", Icons.Default.Inventory2, WarehousePalette.Red, Modifier.weight(1f)) { onOpenInventory(StockFilter.LOW) }
                MetricCard("待收货", data?.pending ?: "—", Icons.Default.LocalShipping, WarehousePalette.Blue, Modifier.weight(1f), onReceive)
                MetricCard("今日出库", data?.todayOutbound ?: "—", Icons.Default.Inventory2, WarehousePalette.Teal, Modifier.weight(1f), onOutbound)
            }
        }
        item {
            WarehouseCard {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("快捷操作", "")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile("收货", Icons.Default.Inventory, WarehousePalette.Teal, Modifier.weight(1f), onReceive)
                        ActionTile("出库", Icons.Default.Outbox, WarehousePalette.Blue, Modifier.weight(1f), onOutbound)
                        ActionTile("盘点", Icons.Default.FactCheck, WarehousePalette.Orange, Modifier.weight(1f), onCount)
                    }
                    Text(if (previewMode) "核对草稿后可模拟确认，数据仅在本次预览中有效" else "核对草稿后确认记账，库存与流水同步更新",
                        color = WarehousePalette.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        item {
            WarehouseCard {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("待处理", data?.tasks?.size?.toString() ?: "待接入")
                    Spacer(Modifier.height(12.dp))
                    if (data?.tasks.isNullOrEmpty()) {
                        Text("暂无临期或低库存事项", color = WarehousePalette.Muted, fontSize = 14.sp)
                    } else {
                        data!!.tasks.forEach { task ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onOpenInventory(if (task.status == "临期") StockFilter.NEAR else StockFilter.LOW) }.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(task.name, color = WarehousePalette.Navy, fontWeight = FontWeight.SemiBold)
                                    Text(task.note, color = if (task.urgent) WarehousePalette.Red else WarehousePalette.Muted, fontSize = 12.sp)
                                }
                                Text(task.status, color = WarehousePalette.Orange, fontSize = 12.sp)
                            }
                        }
                    }
                    TextButton(onClick = { onPending(EntryDirection.INBOUND) }) { Text("待确认入库 · " + inboundDraftCount + " 条草稿") }
                    TextButton(onClick = { onPending(EntryDirection.OUTBOUND) }) { Text("待确认出库 · " + outboundDraftCount + " 条草稿") }
                }
            }
        }
        item {
            WarehouseCard {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    SectionTitle("最近出库", if (previewMode) "预览流水" else "本地流水")
                    val outbound = recentMovements.filter { it.direction == EntryDirection.OUTBOUND }.take(3)
                    if (outbound.isEmpty()) {
                        Text(if (previewMode) "暂无模拟出库，可从上方创建并确认草稿" else "暂无出库记录",
                            color = WarehousePalette.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                    } else {
                        outbound.forEach { movement ->
                            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(movement.productName, color = WarehousePalette.Navy, fontWeight = FontWeight.SemiBold)
                                    Text(movement.note, color = WarehousePalette.Muted, fontSize = 12.sp)
                                }
                                Text("−${movement.quantity} ${movement.unit}", color = WarehousePalette.Blue, fontWeight = FontWeight.Bold)
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
    Box(modifier = modifier.height(118.dp).background(accent.copy(alpha = 0.07f), RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = WarehousePalette.Navy, fontSize = 12.sp, maxLines = 1)
            Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
    Box(modifier = modifier.height(86.dp).background(accent.copy(alpha = 0.1f), RoundedCornerShape(18.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            Text(label, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun WarehouseWorkbenchPreview() {
    WarehouseWorkbenchScreen(onOpenInventory = {}, onReceive = {}, onOutbound = {}, onCount = {}, onPending = {}, data = previewData)
}
