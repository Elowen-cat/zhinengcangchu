package com.youshu.app.ui.warehouse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal val WarehouseTransferAccent = Color(0xFF813CB0)

@Composable
internal fun InventoryActionPanel(available: Boolean, onReceive: () -> Unit, onOutbound: () -> Unit, onTransfer: () -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = Color.White.copy(alpha = 0.94f)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InventoryActionTile("入库", WarehouseDashboardIcons.Receive, WarehousePalette.Teal,
                Modifier.weight(1f), true, onReceive)
            InventoryActionTile("出库", WarehouseDashboardIcons.Dispatch, WarehousePalette.Blue,
                Modifier.weight(1f), available, onOutbound)
            InventoryActionTile("移库", WarehouseDashboardIcons.Transfer, WarehouseTransferAccent,
                Modifier.weight(1f), available, onTransfer)
        }
    }
}

@Composable
private fun InventoryActionTile(label: String, icon: ImageVector, accent: Color, modifier: Modifier,
    enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 108.dp),
        shape = RoundedCornerShape(18.dp), color = Color.Transparent) {
        Column(Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.07f), accent.copy(alpha = 0.14f))))
            .alpha(if (enabled) 1f else 0.38f).padding(vertical = 15.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.Unspecified, modifier = Modifier.size(38.dp))
            Text(label, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
internal fun InventoryBatchRow(batch: WarehouseBatchUi, unit: String, onClick: () -> Unit) {
    val status = batch.status()
    val accent = status.color()
    val days = batch.expiry?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.08f))) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.045f), Color.White.copy(alpha = 0.4f))))
            .padding(vertical = 13.dp, horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent, RoundedCornerShape(4.dp)))
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(batch.code, color = WarehousePalette.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("效期 " + (batch.expiry?.toString() ?: "未设置"), color = WarehousePalette.Muted, fontSize = 12.sp)
                Text("可用 ${if (batch.available()) batch.quantity else 0} $unit", color = WarehousePalette.Blue,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(batch.location, color = WarehousePalette.Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 112.dp)) {
                StatusTag(status.label, accent)
                Text(when {
                    days == null -> "效期待核对"
                    days < 0 -> "已过期 ${-days} 天"
                    days == 0L -> "今天到期"
                    status == BatchStatus.PRIORITY -> "↑ 优先出库"
                    status == BatchStatus.NEAR -> "$days 天后到期"
                    else -> "距效期 $days 天"
                }, color = accent, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                Icon(Icons.Default.ChevronRight, "查看批次 ${batch.code}", tint = WarehousePalette.Muted,
                    modifier = Modifier.padding(top = 5.dp).size(22.dp))
            }
        }
    }
}

@Composable
internal fun InventoryMovementPanel(productId: String, movements: List<WarehouseMovementUi>, preview: Boolean) {
    var expanded by rememberSaveable(productId) { mutableStateOf(false) }
    val sorted = movements.sortedByDescending { it.time }
    WarehouseCard {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("库存流水", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy)
                Text("${sorted.size} 条${if (preview) "示例" else "记录"}", color = WarehousePalette.Muted, fontSize = 12.sp)
            }
            if (preview && sorted.isNotEmpty()) {
                Text("示例流水与进价，仅用于查看界面效果", color = WarehousePalette.Muted, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
            if (sorted.isEmpty()) {
                Text("暂无库存流水；确认库存操作后将在这里显示", color = WarehousePalette.Muted,
                    modifier = Modifier.padding(top = 14.dp))
            }
            (if (expanded) sorted else sorted.take(5)).forEachIndexed { index, movement ->
                if (index > 0) HorizontalDivider(Modifier.padding(top = 14.dp), color = WarehousePalette.Border.copy(alpha = 0.65f))
                InventoryMovementRow(movement)
            }
            if (sorted.size > 5) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (expanded) "收起流水" else "查看全部 ${sorted.size} 条流水")
                }
            }
        }
    }
}

@Composable
private fun InventoryMovementRow(movement: WarehouseMovementUi) {
    val accent = when (movement.direction) {
        EntryDirection.INBOUND -> WarehousePalette.Teal
        EntryDirection.OUTBOUND -> WarehousePalette.Blue
        EntryDirection.TRANSFER -> WarehouseTransferAccent
        EntryDirection.COUNT -> Color(0xFF8C5CA8)
    }
    val icon = when (movement.direction) {
        EntryDirection.INBOUND -> WarehouseDashboardIcons.Receive
        EntryDirection.OUTBOUND -> WarehouseDashboardIcons.Dispatch
        EntryDirection.TRANSFER -> WarehouseDashboardIcons.Transfer
        EntryDirection.COUNT -> WarehouseDashboardIcons.Count
    }
    val amount = when (movement.direction) {
        EntryDirection.INBOUND -> "+${movement.quantity}"
        EntryDirection.OUTBOUND -> "−${movement.quantity}"
        EntryDirection.TRANSFER -> "移 ${movement.quantity}"
        EntryDirection.COUNT -> if (movement.quantity > 0) "+${movement.quantity}" else movement.quantity.toString()
    }
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(accent.copy(alpha = 0.08f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.Unspecified, modifier = Modifier.size(25.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(movement.direction.label, color = WarehousePalette.Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(movement.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), color = WarehousePalette.Muted, fontSize = 11.sp)
            }
            Text("$amount ${movement.unit}", color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text("批次 ${movement.batchCode}", color = WarehousePalette.Navy, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Text(movement.note, color = WarehousePalette.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        if (movement.direction == EntryDirection.INBOUND && (movement.supplier.isNotBlank() || movement.purchasePrice.isNotBlank())) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)
                .background(WarehousePalette.Mint.copy(alpha = 0.65f), RoundedCornerShape(10.dp)).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (movement.supplier.isNotBlank()) Text("供应商  ${movement.supplier}", color = WarehousePalette.TealDark, fontSize = 12.sp)
                val price = movement.purchasePrice.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
                if (price != null) Text("单件进价  ¥${price.setScale(2, RoundingMode.HALF_UP).toPlainString()} / ${movement.unit}",
                    color = WarehousePalette.TealDark, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}
