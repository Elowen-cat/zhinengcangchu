package com.youshu.app.ui.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WarehouseHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = WarehousePalette.Navy) }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy)
    }
}

@Composable
fun WarehouseNotice(preview: Boolean, text: String = "本地单仓 · 草稿需核对后确认，库存变更会记录流水。") {
    Text(
        if (preview) "界面预览 · 示例数据，不代表正式库存" else text,
        color = WarehousePalette.TealDark,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().background(WarehousePalette.Mint, RoundedCornerShape(14.dp)).padding(12.dp)
    )
}

@Composable
fun WarehouseAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WarehousePalette.Teal, contentColor = Color.White)
    ) { Text(text, color = if (enabled) Color.White else WarehousePalette.Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun ProductThumb(modifier: Modifier = Modifier, accent: Color = WarehousePalette.Teal, background: Color = WarehousePalette.Mint) {
    Box(modifier.size(60.dp).background(background, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Inventory2, null, tint = accent, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun WarehouseEmpty(title: String, description: String) {
    WarehouseCard {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ProductThumb()
            Text(title, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy, modifier = Modifier.padding(top = 14.dp))
            Text(description, color = WarehousePalette.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun StatusTag(label: String, color: Color = WarehousePalette.Teal) {
    Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(color.copy(alpha = 0.09f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
fun StockStatusTag(status: WarehouseStockStatus) {
    Text(status.label, color = status.badgeText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(status.badgeBackground, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp))
}

fun BatchStatus.color(): Color = when (this) {
    BatchStatus.EXPIRED, BatchStatus.NEAR -> WarehousePalette.Red
    BatchStatus.PRIORITY -> WarehousePalette.Orange
    BatchStatus.NORMAL -> WarehousePalette.Teal
}
