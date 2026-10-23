package com.youshu.app.ui.warehouse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
internal fun OutboundPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = Color.White.copy(alpha = .94f), shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE2EBF7))) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
internal fun OutboundHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = WarehousePalette.Navy) }
        Text(title, Modifier.weight(1f), fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = WarehousePalette.Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
internal fun OutboundNotice(text: String, warning: Boolean = false) {
    Surface(color = if (warning) Color(0xFFFFF2E5) else Color(0xFFEBF4FF), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (warning) Icons.Default.ErrorOutline else Icons.Default.Info, null,
                tint = if (warning) WarehousePalette.Orange else WarehousePalette.Blue, modifier = Modifier.size(23.dp))
            Text(text, Modifier.weight(1f).padding(start = 9.dp), fontSize = 12.sp, lineHeight = 19.sp,
                color = if (warning) WarehousePalette.Orange else WarehousePalette.Muted)
        }
    }
}

@Composable
internal fun OutboundThumb(uri: String?, modifier: Modifier = Modifier) {
    Box(modifier.size(76.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F7F9)), contentAlignment = Alignment.Center) {
        if (uri.isNullOrBlank()) Icon(WarehouseReceiveIcons.Stock, null, tint = Color.Unspecified, modifier = Modifier.size(40.dp))
        else AsyncImage(uri, "商品照片", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun OutboundSourceBadge(source: EntrySource) {
    val color = if (source == EntrySource.VOICE) WarehousePalette.Blue else WarehousePalette.Teal
    Row(Modifier.background(color.copy(alpha = .09f), RoundedCornerShape(9.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(when (source) { EntrySource.PHOTO -> Icons.Default.CameraAlt; EntrySource.VOICE -> Icons.Default.Mic; else -> Icons.Default.Edit },
            null, Modifier.size(15.dp), tint = color)
        Text(" ${source.label}", color = color, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun OutboundQuantity(value: String, unit: String, enabled: Boolean, onChange: (String) -> Unit) {
    val amount = value.toIntOrNull()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedIconButton(onClick = { onChange(((amount ?: 1) - 1).coerceAtLeast(1).toString()) },
            enabled = enabled && amount != null && amount > 1, shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, WarehousePalette.Border)) {
            Icon(Icons.Default.Remove, "减少数量", tint = WarehousePalette.Muted)
        }
        OutlinedTextField(value, { text -> if (text.length <= 10 && text.all(Char::isDigit)) onChange(text) },
            Modifier.weight(1f), enabled = enabled, singleLine = true, shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(fontSize = 21.sp, textAlign = TextAlign.Center, color = WarehousePalette.Navy))
        OutlinedIconButton(onClick = { onChange(((amount ?: 0).toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toString()) },
            enabled = enabled && (amount == null || amount < Int.MAX_VALUE), shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, WarehousePalette.Border)) {
            Icon(Icons.Default.Add, "增加数量", tint = WarehousePalette.Muted)
        }
        Text(unit.ifBlank { "单位" }, fontSize = 15.sp, color = WarehousePalette.Muted, modifier = Modifier.widthIn(min = 22.dp))
    }
}
