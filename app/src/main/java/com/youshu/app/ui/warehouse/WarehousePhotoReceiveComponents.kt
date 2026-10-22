package com.youshu.app.ui.warehouse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun PhotoReceiveStrip(lines: List<PhotoReceiveLine>, enabled: Boolean, onCamera: () -> Unit,
    onGallery: () -> Unit, onRemove: (PhotoReceiveLine) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("已添加 ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy)
                Text("${lines.flatMap { it.attachments }.filter { it.isNotBlank() }.distinct().size}", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Teal)
                Text(" 张", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onGallery, enabled = enabled && lines.flatMap { it.attachments }.distinct().size < 12) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(17.dp))
                    Text(" 相册选择", fontSize = 12.sp)
                }
            }
            if (lines.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(WarehouseReceiveIcons.Camera, null, tint = Color.Unspecified, modifier = Modifier.size(50.dp))
                    Text("拍下商品，自动生成待核对结果", color = WarehousePalette.Navy, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp))
                    Text("每张照片以一个商品为主，文字清晰更容易识别", color = WarehousePalette.Muted, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 7.dp))
                    TextButton(onClick = onCamera, enabled = enabled) { Text("开始拍照") }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(lines, key = { it.id }) { line ->
                        Box(Modifier.size(104.dp).clip(RoundedCornerShape(18.dp)).background(WarehousePalette.Background)) {
                            AsyncImage(line.photoUri, "商品照片 ${line.name.ifBlank { "待识别" }}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            IconButton(onClick = { onRemove(line) }, enabled = enabled,
                                modifier = Modifier.align(Alignment.TopEnd).size(48.dp)) {
                                Icon(Icons.Default.Close, "删除照片及对应结果 ${line.name}", tint = Color.White,
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape).padding(4.dp).size(22.dp))
                            }
                            if (line.working) CircularProgressIndicator(Modifier.align(Alignment.BottomStart).padding(8.dp).size(18.dp),
                                color = WarehousePalette.Teal, strokeWidth = 2.dp)
                        }
                    }
                    item {
                        Surface(onClick = onCamera, enabled = enabled && lines.flatMap { it.attachments }.distinct().size < 12, modifier = Modifier.size(104.dp),
                            shape = RoundedCornerShape(18.dp), color = Color(0xFFEEF8F8)) {
                            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, null, tint = WarehousePalette.Teal,
                                    modifier = Modifier.background(WarehousePalette.Mint, CircleShape).padding(9.dp).size(25.dp))
                                Text("继续拍照", color = WarehousePalette.TealDark, fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
                            }
                        }
                    }
                }
                Text("一张照片对应一项商品；重复照片可移除。每次最多 12 张。", color = WarehousePalette.Muted,
                    fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoReceiveResultCard(line: PhotoReceiveLine, enabled: Boolean, locations: List<String>,
    onEdit: (String, String) -> Unit, onRetry: () -> Unit) {
    var expanded by rememberSaveable(line.id) { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showLocations by remember { mutableStateOf(false) }
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFFEDF5F5))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(line.photoUri, "${line.name}照片", Modifier.size(74.dp).clip(RoundedCornerShape(16.dp))
                    .background(WarehousePalette.Background), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    StatusTag(when {
                        line.demo -> "示例结果"
                        line.working -> "识别中"
                        line.recognized -> "已识别"
                        line.validationError == null -> "已填写"
                        else -> "待补充"
                    }, if (line.error != null) WarehousePalette.Orange else WarehousePalette.Teal)
                    Text(line.name.ifBlank { if (line.working) "正在识别商品…" else "请填写商品名称" },
                        color = WarehousePalette.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp))
                }
                IconButton(onClick = { expanded = !expanded }, enabled = enabled) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                        if (expanded) "收起商品编辑" else "编辑商品名称和进价", tint = WarehousePalette.Muted)
                }
            }
            if (line.working) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = WarehousePalette.Teal)
                Text("正在提取这张照片中的商品信息…", color = WarehousePalette.Muted, fontSize = 12.sp)
            }
            if (expanded || (!line.working && line.name.isBlank())) {
                PhotoInput("商品名称", line.name, enabled, onValueChange = { onEdit("name", it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhotoFieldLabel("数量")
                OutlinedTextField(line.quantity, { onEdit("quantity", it) }, Modifier.weight(1f), enabled = enabled,
                    singleLine = true, placeholder = { Text("请填写") }, shape = RoundedCornerShape(13.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(line.unit, { onEdit("unit", it) }, Modifier.width(76.dp), enabled = enabled,
                    singleLine = true, placeholder = { Text("单位") }, shape = RoundedCornerShape(13.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhotoFieldLabel("批次")
                PhotoInput("", line.batchCode, enabled, warning = "batchCode" !in line.touched,
                    modifier = Modifier.weight(1f), placeholder = "请填写批次", onValueChange = { onEdit("batchCode", it) },
                    trailing = if (line.batchCode.isNotBlank() && "batchCode" !in line.touched) ({
                        Icon(Icons.Default.ErrorOutline, "批次请核对", tint = WarehousePalette.Orange, modifier = Modifier.size(20.dp))
                    }) else null)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhotoFieldLabel("效期")
                PhotoInput("",
                    line.expiry, enabled, warning = line.expiry.isNotBlank() && "expiry" !in line.touched,
                    modifier = Modifier.weight(1f), placeholder = "YYYY-MM-DD", onValueChange = { onEdit("expiry", it) },
                    trailing = { IconButton(onClick = { showDate = true }, enabled = enabled) {
                        Icon(Icons.Default.CalendarMonth, "选择效期", tint = WarehousePalette.Muted) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhotoFieldLabel("库位")
                PhotoInput("", line.location, enabled, modifier = Modifier.weight(1f), placeholder = "填写或选择库位", onValueChange = { onEdit("location", it) },
                    trailing = { IconButton(onClick = { showLocations = true }, enabled = enabled) {
                        Icon(Icons.Default.ChevronRight, "选择已有库位", tint = WarehousePalette.Muted) } })
            }
            if (expanded) {
                PhotoInput("供应商（可选）", line.supplier, enabled, onValueChange = { onEdit("supplier", it) })
                PhotoInput("单件进价（可选）", line.price, enabled, keyboardType = KeyboardType.Decimal,
                    onValueChange = { onEdit("price", it) })
            }
            if (!line.working) line.validationError?.let { Text(it, color = WarehousePalette.Orange, fontSize = 12.sp) }
            line.error?.let { Text(it, color = WarehousePalette.Orange, fontSize = 12.sp) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("加入后仍需人工核对", color = WarehousePalette.Muted, fontSize = 11.sp)
                TextButton(onClick = onRetry, enabled = enabled && !line.working && !line.demo) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Text("重新识别", fontSize = 12.sp)
                }
            }
        }
    }
    if (showDate) {
        val initial = runCatching { LocalDate.parse(line.expiry).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { onEdit("expiry", LocalDate.ofEpochDay(it / 86_400_000).toString()) }
                showDate = false
            }, enabled = picker.selectedDateMillis != null && enabled) { Text("确定") }
        }, dismissButton = { TextButton(onClick = { onEdit("expiry", ""); showDate = false }, enabled = enabled) { Text("不设置效期") } }) {
            DatePicker(picker)
        }
    }
    if (showLocations) AlertDialog(onDismissRequest = { showLocations = false }, title = { Text("选择库位") }, text = {
        Column {
            Text("可选择已有库位，也可关闭后直接输入。", color = WarehousePalette.Muted, fontSize = 13.sp)
            // Keep the dialog compact; arbitrary locations remain editable in the field.
            locations.take(8).forEach { location ->
                TextButton(onClick = { onEdit("location", location); showLocations = false }, enabled = enabled) { Text(location) }
            }
        }
    }, confirmButton = { TextButton(onClick = { showLocations = false }) { Text("关闭") } })
}

@Composable
private fun PhotoFieldLabel(text: String) {
    Text(text, color = WarehousePalette.Muted, fontSize = 13.sp, modifier = Modifier.width(43.dp))
}

@Composable
private fun PhotoInput(label: String, value: String, enabled: Boolean, modifier: Modifier = Modifier.fillMaxWidth(),
    warning: Boolean = false, placeholder: String = "请填写", keyboardType: KeyboardType = KeyboardType.Text,
    trailing: (@Composable () -> Unit)? = null, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, modifier, enabled = enabled, singleLine = true,
        label = if (label.isNotBlank()) ({ Text(label, fontSize = 12.sp) }) else null,
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        trailingIcon = trailing, shape = RoundedCornerShape(13.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = if (warning) Color(0xFFFFF8ED) else Color(0xFFFBFDFE),
            unfocusedBorderColor = if (warning) Color(0xFFFFD28A) else WarehousePalette.Border,
            unfocusedLabelColor = if (warning) WarehousePalette.Orange else WarehousePalette.Muted))
}
