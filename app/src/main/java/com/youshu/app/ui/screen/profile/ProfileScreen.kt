package com.youshu.app.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.BuildConfig
import com.youshu.app.ui.components.AppDialog
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.ProfileViewModel
import com.youshu.app.ui.warehouse.WarehousePalette

@Composable
fun ProfileScreen(
    onOpenExpiry: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    sessionDraftCount: Int = 0,
    productCount: Int = 0,
    expiringCount: Int = 0,
    previewMode: Boolean = false,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val trashCount by viewModel.trashCount.collectAsState()
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }
    var showModelDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarehousePalette.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    infoDialogTitle = "本地仓库"
                    infoDialogMessage = if (previewMode) "这是只用于界面演示的样例仓库。" else "仓储数据保存在当前设备；可通过设置中的数据备份导出。"
                },
            shape = RoundedCornerShape(26.dp),
            color = WarehousePalette.Mint,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(WarehousePalette.Teal),
                    contentAlignment = Alignment.Center
                ) {
                    Text("东", color = androidx.compose.ui.graphics.Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(if (previewMode) "本地仓库 · 界面预览" else "本地仓库", color = WarehousePalette.Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("单仓模式 · 当前设备", color = WarehousePalette.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "仓库信息", tint = WarehousePalette.Muted)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = androidx.compose.ui.graphics.Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileMetric(productCount.toString(), "在库商品", Modifier.weight(1f))
                MetricDivider()
                ProfileMetric(sessionDraftCount.toString(), "待确认草稿", Modifier.weight(1f))
                MetricDivider()
                ProfileMetric(expiringCount.toString(), "即将到期", Modifier.weight(1f))
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = androidx.compose.ui.graphics.Color.White,
            shadowElevation = 2.dp
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ProfileMenuRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "智能服务状态",
                    subtitle = if (viewModel.warehouseRecognitionConfigured) "仓储识别服务已配置" else "仓储识别 API 待配置 · 可手动录入",
                    onClick = { showModelDialog = true }
                )
                ProfileDivider()
                ProfileMenuRow(
                    icon = Icons.Default.Groups,
                    title = "成员与权限",
                    subtitle = "当前为本机单仓，暂不支持云端成员权限",
                    onClick = {
                        infoDialogTitle = "成员与权限"
                        infoDialogMessage = "当前版本使用本地单仓数据，还没有跨设备成员与权限管理。"
                    }
                )
                ProfileDivider()
                ProfileMenuRow(
                    icon = Icons.Default.Notifications,
                    title = "到期提醒",
                    subtitle = "查看即将到期的库存批次",
                    onClick = onOpenExpiry
                )
                ProfileDivider()
                ProfileMenuRow(
                    icon = Icons.Default.History,
                    title = "旧记录回收站",
                    subtitle = if (trashCount > 0) "原物品记录有 $trashCount 项可恢复" else "管理原物品数据的删除记录",
                    onClick = onOpenTrash
                )
                ProfileDivider()
                ProfileMenuRow(
                    icon = Icons.Default.Settings,
                    title = "设置",
                    subtitle = "仓库、提醒与数据备份",
                    onClick = onOpenSettings
                )
                ProfileDivider()
                ProfileMenuRow(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "帮助与反馈",
                    subtitle = "常见问题与功能建议",
                    onClick = {
                        infoDialogTitle = "帮助与反馈"
                        infoDialogMessage = "后续会补充常见问题、使用说明和反馈渠道。"
                    }
                )
                ProfileDivider()
                ProfileMenuRow(
                    icon = Icons.Default.Info,
                    title = "关于我们",
                    subtitle = "版本 ${BuildConfig.VERSION_NAME}",
                    onClick = {
                        infoDialogTitle = "关于我们"
                        infoDialogMessage = "仓储演示空间基于现有开源应用逐步改造。\n版本 ${BuildConfig.VERSION_NAME}"
                    }
                )
            }
        }

        Text(
            text = "本地数据保存在当前设备",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }

    if (showModelDialog) {
        AppDialog(
            title = "智能服务状态",
            subtitle = if (viewModel.warehouseRecognitionConfigured) "仓储识别服务已配置。" else "仓储识别 API 尚未配置。",
            onDismissRequest = { showModelDialog = false },
            confirmText = "关闭",
            onConfirm = { showModelDialog = false }
        ) {
            Text(
                if (viewModel.warehouseRecognitionConfigured) "拍照和语音建议仅用于填写草稿，确认前仍需人工核对。"
                else "当前可手动入库、出库和核对草稿；照片保留作核对依据，语音使用设备提供的识别服务。旧云函数已停用。",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }

    if (infoDialogTitle != null && infoDialogMessage != null) {
        AppDialog(
            title = infoDialogTitle!!,
            onDismissRequest = { infoDialogTitle = null; infoDialogMessage = null },
            confirmText = "知道了",
            onConfirm = { infoDialogTitle = null; infoDialogMessage = null }
        ) {
            Text(infoDialogMessage!!, color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Teal)
        Text(label, fontSize = 12.sp, color = WarehousePalette.Muted, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun MetricDivider() {
    Spacer(Modifier.width(1.dp).height(38.dp).background(WarehousePalette.Border))
}

@Composable
private fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(WarehousePalette.Mint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = WarehousePalette.Teal, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WarehousePalette.Navy)
            Text(subtitle, fontSize = 12.sp, color = WarehousePalette.Muted, modifier = Modifier.padding(top = 3.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WarehousePalette.Muted)
    }
}

@Composable
private fun ProfileDivider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(WarehousePalette.Border))
}
