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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun WarehouseAnalysisScreen(state: WarehouseUiState, onProduct: (String) -> Unit) {
    val results = state.products.map { WarehouseReplenishment.calculate(it, state.movements, state.drafts) }
        .sortedWith(compareByDescending<ReplenishmentResult> { it.suggestedQuantity > 0 }
            .thenByDescending { it.suggestedQuantity }.thenBy { it.product.name })
    WarehousePage("分析") {
        item { WarehouseNotice(state.previewMode, "按近 14 天已确认的补货出库估算；报损和其他出库不计入需求。") }
        item {
            WarehouseCard {
                Column(Modifier.padding(20.dp)) {
                    Text("补货概览", color = WarehousePalette.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        AnalysisMetric("建议补货", "${results.count { it.suggestedQuantity > 0 }} 种", WarehousePalette.Teal)
                        AnalysisMetric("有出库依据", "${results.count { it.effectiveOutbound > 0 }} 种", WarehousePalette.Blue)
                        AnalysisMetric("仓储商品", "${results.size} 种", WarehousePalette.Navy)
                    }
                    Text("建议量是本地估算，须人工核对后采购。", color = WarehousePalette.Muted,
                        fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
                }
            }
        }
        if (results.isEmpty()) {
            item { WarehouseEmpty("暂无可分析商品", "先从收货入口确认一件商品，分析会自动读取库存与流水。") }
        } else {
            item { Text("单品建议", color = WarehousePalette.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            results.forEach { result ->
                item(key = result.product.id) {
                    WarehouseCard {
                        Row(Modifier.fillMaxWidth().clickable { onProduct(result.product.id) }.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            ProductThumb()
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text(result.product.name, color = WarehousePalette.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("现存 ${result.available} ${result.product.unit} · 近 14 天出库 ${result.effectiveOutbound} ${result.product.unit}",
                                    color = WarehousePalette.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                                Text(if (result.suggestedQuantity > 0) "建议补 ${result.suggestedQuantity} ${result.product.unit}" else "暂不补货",
                                    color = if (result.suggestedQuantity > 0) WarehousePalette.Teal else WarehousePalette.Blue,
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            }
                            androidx.compose.material3.Icon(Icons.Default.ChevronRight, "查看计算依据", tint = WarehousePalette.Muted)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
fun WarehouseAnalysisDetailScreen(
    product: WarehouseProductUi?,
    movements: List<WarehouseMovementUi>,
    drafts: List<StockDraftUi>,
    preview: Boolean,
    onBack: () -> Unit,
    onSavePolicy: (String, Int, Int, (String?) -> Unit) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    WarehousePage("") {
        item { WarehouseHeader("单品分析", onBack) }
        if (product == null) {
            item { WarehouseEmpty("商品暂不可用", "返回分析列表选择其他商品。") }
        } else {
            val result = WarehouseReplenishment.calculate(product, movements, drafts)
            item { WarehouseNotice(preview, "近 14 个自然日的已确认出库；只把“补到货架”计入需求。") }
            item {
                WarehouseCard {
                    Column(Modifier.padding(20.dp)) {
                        Text(product.name, color = WarehousePalette.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(if (result.suggestedQuantity > 0) "建议补货 ${result.suggestedQuantity} ${product.unit}" else "暂不补货",
                            color = WarehousePalette.Teal, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp))
                        Text("本地规则估算 · 采购前请人工核对", color = WarehousePalette.Muted,
                            fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            item {
                WarehouseCard {
                    Column(Modifier.padding(20.dp)) {
                        Text("出库趋势", color = WarehousePalette.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("近 14 天有效出库 ${result.effectiveOutbound} ${product.unit} · 日均 ${formatRate(result.dailyOutbound)} ${product.unit}",
                            color = WarehousePalette.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        OutboundBars(result.dailyQuantities, Modifier.padding(top = 18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("13 天前", color = WarehousePalette.Muted, fontSize = 11.sp)
                            Text("今天", color = WarehousePalette.Muted, fontSize = 11.sp)
                        }
                        if (result.effectiveOutbound == 0L) {
                            Text("暂无有效出库，建议量仅由安全库存决定。", color = WarehousePalette.Muted,
                                fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
            item {
                WarehouseCard {
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("计算依据", color = WarehousePalette.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { editing = true }) { Text("调整参数") }
                        }
                        BasisRow("近 14 天有效出库", "${result.effectiveOutbound} ${product.unit}")
                        BasisRow("报损及其他出库（排除）", "${result.excludedOutbound} ${product.unit}")
                        BasisRow("日均有效出库", "${formatRate(result.dailyOutbound)} ${product.unit}")
                        BasisRow("目标覆盖", "${product.targetCoverageDays} 天")
                        BasisRow("目标需求（向上取整）", "${result.targetDemand} ${product.unit}")
                        BasisRow("安全库存", "${product.safetyStock} ${product.unit}")
                        BasisRow("当前可用库存", "${result.available} ${product.unit}")
                        BasisRow("待确认收货（暂扣）", "${result.pendingInbound} ${product.unit}")
                        BasisRow("库存覆盖", result.coverageDays?.let { "${formatQuantity(it)} 天" } ?: "暂无有效出库")
                        Text("建议量 = max(0，目标需求 + 安全库存 − 可用库存 − 待确认收货)",
                            color = WarehousePalette.TealDark, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp))
                        Text("待确认收货可能变动；出库记录仅代表仓库去向，不等同真实销量。", color = WarehousePalette.Muted,
                            fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (editing && product != null) {
        ReplenishmentPolicyDialog(product, onDismiss = { editing = false }, onSave = { safety, days, done ->
            onSavePolicy(product.id, safety, days, done)
        })
    }
}

@Composable
private fun AnalysisMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, color = WarehousePalette.Muted, fontSize = 12.sp)
        Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun BasisRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = WarehousePalette.Muted, fontSize = 13.sp)
        Text(value, color = WarehousePalette.Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OutboundBars(daily: List<Int>, modifier: Modifier = Modifier) {
    val peak = (daily.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom) {
        daily.forEach { quantity ->
            val height = (6f + 78f * quantity / peak).dp
            Box(Modifier.weight(1f).height(height).background(
                if (quantity > 0) WarehousePalette.Teal else WarehousePalette.Border,
                RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
        }
    }
}

@Composable
private fun ReplenishmentPolicyDialog(
    product: WarehouseProductUi,
    onDismiss: () -> Unit,
    onSave: (Int, Int, (String?) -> Unit) -> Unit
) {
    var safety by remember(product.id) { mutableStateOf(product.safetyStock.toString()) }
    var days by remember(product.id) { mutableStateOf(product.targetCoverageDays.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("补货参数") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(product.name, color = WarehousePalette.Muted)
                OutlinedTextField(safety, { safety = it.filter(Char::isDigit); error = null }, label = { Text("安全库存（${product.unit}）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(days, { days = it.filter(Char::isDigit); error = null }, label = { Text("目标覆盖天数（1–90）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                error?.let { Text(it, color = WarehousePalette.Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                val safe = safety.toIntOrNull()
                val target = days.toIntOrNull()
                if (safe == null || safe < 0 || target == null || target !in 1..90) {
                    error = "请输入有效的安全库存和 1–90 天的目标覆盖"
                } else {
                    saving = true
                    onSave(safe, target) { failure ->
                        saving = false
                        if (failure == null) onDismiss() else error = failure
                    }
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("取消") } }
    )
}

private fun formatQuantity(value: Double) = String.format(Locale.CHINA, "%.1f", value)
private fun formatRate(value: Double) = String.format(Locale.CHINA, "%.2f", value)
