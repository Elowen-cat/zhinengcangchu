package com.youshu.app.ui.warehouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun WarehouseCountScreen(
    state: WarehouseUiState,
    onBack: () -> Unit,
    onConfirm: (String, Int, String, (String?) -> Unit) -> Unit
) {
    var productId by rememberSaveable { mutableStateOf<String?>(null) }
    var batchId by rememberSaveable { mutableStateOf<String?>(null) }
    var observed by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectProduct by remember { mutableStateOf(false) }
    var selectBatch by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val product = state.product(productId)
    val batch = product?.batches?.firstOrNull { it.id == batchId }
    val quantity = observed.toIntOrNull()
    val ready = batch != null && quantity != null && quantity >= 0 && note.isNotBlank()
    WarehousePage("") {
        item { WarehouseHeader("库存盘点", onBack) }
        item { WarehouseNotice(state.previewMode, "输入实盘数量，确认后按差异调整批次库存并记录流水。") }
        item {
            WarehouseCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("选择盘点批次", color = WarehousePalette.Navy, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { selectProduct = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(product?.name ?: "选择商品")
                    }
                    product?.let { StockStatusTag(it.stockStatus) }
                    TextButton(onClick = { selectBatch = true }, enabled = product != null, modifier = Modifier.fillMaxWidth()) {
                        Text(batch?.let { "${it.code} · ${it.location}" } ?: "选择批次")
                    }
                    Text("账面数量：${batch?.quantity ?: "—"} ${product?.unit.orEmpty()}",
                        color = product?.stockStatus?.accent ?: WarehousePalette.Muted)
                    OutlinedTextField(observed, { observed = it.filter(Char::isDigit); result = null },
                        modifier = Modifier.fillMaxWidth(), label = { Text("实盘数量") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(note, { note = it; result = null }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("盘点说明") }, singleLine = true)
                    if (batch != null && quantity != null) {
                        val delta = quantity.toLong() - batch.quantity
                        Text("差异：${if (delta > 0) "+" else ""}$delta ${product.unit}", color = WarehousePalette.Blue)
                    }
                }
            }
        }
        item {
            result?.let { Text(it, color = WarehousePalette.TealDark) }
            WarehouseAction(if (saving) "正在记账…" else "核对并确认盘点", { confirm = true }, enabled = ready && !saving)
        }
    }
    if (selectProduct) {
        AdjustmentPicker("选择商品", state.products.map { it.id to "${it.name} · ${it.stockStatus.label}" }, { chosen ->
            productId = chosen; batchId = null; observed = ""; selectProduct = false
        }, { selectProduct = false })
    }
    if (selectBatch) {
        AdjustmentPicker("选择批次", product?.batches.orEmpty().map { it.id to "${it.code} · ${it.location} · ${it.quantity} ${product?.unit}" }, { chosen ->
            batchId = chosen
            observed = product?.batches?.firstOrNull { it.id == chosen }?.quantity?.toString().orEmpty()
            selectBatch = false
        }, { selectBatch = false })
    }
    if (confirm && ready) {
        AlertDialog(onDismissRequest = { confirm = false }, title = { Text("确认盘点差异") },
            text = { Text("${product?.name} · ${batch?.code}\n账面 ${batch?.quantity} → 实盘 $quantity ${product?.unit}\n说明：${note.trim()}") },
            confirmButton = { TextButton(onClick = {
                confirm = false; saving = true
                onConfirm(batchId!!, quantity!!, note.trim()) { failure ->
                    saving = false
                    result = failure ?: "盘点完成，差异已写入流水"
                }
            }) { Text("确认记账") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("返回修改") } })
    }
}

@Composable
fun WarehouseTransferScreen(
    product: WarehouseProductUi?,
    onBack: () -> Unit,
    preview: Boolean,
    onConfirm: (String, String, (String?) -> Unit) -> Unit
) {
    var batchId by rememberSaveable { mutableStateOf<String?>(null) }
    var target by rememberSaveable { mutableStateOf("") }
    var selectBatch by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val batch = product?.batches?.firstOrNull { it.id == batchId }
    val ready = batch != null && batch.quantity > 0 && target.trim().isNotEmpty() && target.trim() != batch.location
    WarehousePage("") {
        item { WarehouseHeader("整批移库", onBack) }
        item { WarehouseNotice(preview, "移动整个批次到新库位；库存数量不变，操作会记录流水。") }
        if (product == null) {
            item { WarehouseEmpty("商品暂不可用", "返回库存详情选择其他商品。") }
        } else {
            item {
                WarehouseCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(product.name, color = WarehousePalette.Navy, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { selectBatch = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(batch?.let { "${it.code} · ${it.location}" } ?: "选择来源批次")
                        }
                        Text("本次移动：${batch?.quantity ?: "—"} ${product.unit}", color = WarehousePalette.Teal)
                        Text("来源库位：${batch?.location ?: "—"}", color = WarehousePalette.Muted)
                        OutlinedTextField(target, { target = it; result = null }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("目标库位") }, singleLine = true)
                    }
                }
            }
            item {
                result?.let { Text(it, color = WarehousePalette.TealDark) }
                WarehouseAction(if (saving) "正在记账…" else "核对并确认移库", { confirm = true }, enabled = ready && !saving)
            }
        }
    }
    if (selectBatch) {
        AdjustmentPicker("选择来源批次", product?.batches.orEmpty().filter { it.quantity > 0 }
            .map { it.id to "${it.code} · ${it.location} · ${it.quantity} ${product?.unit}" }, { chosen ->
            batchId = chosen; selectBatch = false
        }, { selectBatch = false })
    }
    if (confirm && ready) {
        AlertDialog(onDismissRequest = { confirm = false }, title = { Text("确认整批移库") },
            text = { Text("${product?.name} · ${batch?.code}\n${batch?.quantity} ${product?.unit}\n${batch?.location} → ${target.trim()}") },
            confirmButton = { TextButton(onClick = {
                confirm = false; saving = true
                onConfirm(batchId!!, target.trim()) { failure ->
                    saving = false
                    result = failure ?: "移库完成，数量不变且流水已记录"
                }
            }) { Text("确认记账") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("返回修改") } })
    }
}

@Composable
private fun AdjustmentPicker(title: String, choices: List<Pair<String, String>>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (choices.isEmpty()) Text("暂无可选项", color = WarehousePalette.Muted)
                choices.forEach { (id, label) -> TextButton(onClick = { onSelect(id) }) { Text(label) } }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}
