package com.youshu.app.ui.warehouse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun WarehouseReceiveHomeScreen(
    state: WarehouseUiState,
    onBack: () -> Unit,
    onSource: (EntrySource) -> Unit,
    onPending: () -> Unit
) {
    val pending = state.drafts.count { it.direction == EntryDirection.INBOUND }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color(0xFFF8FDFD), Color(0xFFEDF9F8), WarehousePalette.Background))),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = WarehousePalette.Navy, modifier = Modifier.size(27.dp))
                }
                Text("收货入库", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    color = WarehousePalette.Navy, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }
        }
        item {
            Surface(shape = RoundedCornerShape(26.dp), color = Color.Transparent, border = BorderStroke(1.dp, Color.White)) {
                Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFE2F8F1), Color(0xFFF3FCFA))))
                    .padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(WarehouseReceiveIcons.Store, null, tint = Color.Unspecified, modifier = Modifier.size(42.dp))
                    Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (state.previewMode) "青禾便利店 · 后仓" else "本地仓库", color = WarehousePalette.TealDark,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("识别结果需核对后才会入库", color = WarehousePalette.Muted, fontSize = 13.sp)
                        if (state.previewMode) Text("界面预览 · 示例数据，不代表正式库存",
                            color = WarehousePalette.TealDark, fontSize = 10.sp)
                    }
                }
            }
        }
        item { ReceiveWorkflow() }
        item {
            ReceiveEntryCard("拍照录入", "拍摄商品与包装，自动识别填表", WarehouseReceiveIcons.Camera,
                WarehousePalette.Teal, onClick = { onSource(EntrySource.PHOTO) })
        }
        item {
            ReceiveEntryCard("语音录入", "口述商品、数量、批次和效期", WarehouseReceiveIcons.Voice,
                WarehousePalette.Blue, onClick = { onSource(EntrySource.VOICE) })
        }
        item {
            ReceiveEntryCard("待确认", "核对识别结果后批量入库", WarehouseReceiveIcons.Pending,
                WarehousePalette.Orange, count = pending, onClick = onPending)
        }
        item {
            OutlinedButton(onClick = { onSource(EntrySource.MANUAL) }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, WarehousePalette.Teal.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarehousePalette.TealDark, containerColor = Color.White.copy(alpha = 0.6f))) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("手动填写", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReceiveWorkflow() {
    Surface(shape = RoundedCornerShape(26.dp), color = Color.White.copy(alpha = 0.9f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp), verticalAlignment = Alignment.Top) {
            ReceiveStep("录入", "拍照或语音\n生成识别结果", WarehouseReceiveIcons.Record, Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = WarehousePalette.Muted.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 17.dp).size(20.dp))
            ReceiveStep("核对", "核对商品、数量\n批次和效期", WarehouseReceiveIcons.Review, Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = WarehousePalette.Muted.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 17.dp).size(20.dp))
            ReceiveStep("入库", "确认无误后\n正式入库", WarehouseReceiveIcons.Stock, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReceiveStep(title: String, description: String, icon: ImageVector, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(50.dp).background(WarehousePalette.Mint.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
        }
        Text(title, color = WarehousePalette.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 7.dp, bottom = 5.dp))
        Text(description, color = WarehousePalette.Muted, fontSize = 12.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ReceiveEntryCard(title: String, subtitle: String, icon: ImageVector, accent: Color,
    count: Int? = null, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
        color = Color.White, border = BorderStroke(1.dp, Color.White), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color.White, accent.copy(alpha = 0.045f))))
            .padding(horizontal = 18.dp, vertical = 19.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(74.dp).background(Brush.linearGradient(listOf(accent.copy(alpha = 0.065f), accent.copy(alpha = 0.11f))),
                RoundedCornerShape(21.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.Unspecified, modifier = Modifier.size(46.dp))
            }
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, color = WarehousePalette.Navy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    count?.let {
                        Text("${it}条", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (it > 0) Color.White else WarehousePalette.Muted,
                            modifier = Modifier.background(
                                if (it > 0) WarehousePalette.Red else WarehousePalette.Border.copy(alpha = 0.65f),
                                RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                Text(subtitle, color = WarehousePalette.Muted, fontSize = 13.sp, lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 7.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = WarehousePalette.Navy.copy(alpha = 0.75f), modifier = Modifier.size(24.dp))
        }
    }
}
