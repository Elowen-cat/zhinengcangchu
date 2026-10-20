package com.youshu.app.ui.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WarehouseInventoryScreen(
    state: WarehouseUiState,
    initialFilter: StockFilter,
    onProduct: (String) -> Unit,
    onReceive: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(initialFilter.name) }
    var lastIncomingFilter by rememberSaveable { mutableStateOf(initialFilter.name) }
    var category by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    LaunchedEffect(initialFilter) {
        if (lastIncomingFilter != initialFilter.name) {
            filterName = initialFilter.name
            lastIncomingFilter = initialFilter.name
        }
    }
    val filter = StockFilter.valueOf(filterName)
    val products = state.products.filter { product ->
        product.matches(filter) && (category.isEmpty() || category == product.category) &&
            (location.isEmpty() || product.batches.any { it.location == location }) &&
            (query.isBlank() || listOf(product.name, product.category).plus(product.batches.flatMap { listOf(it.code, it.location) })
                .any { it.contains(query.trim(), ignoreCase = true) })
    }
    WarehousePage("库存") {
        item { WarehouseNotice(state.previewMode, "库存来自当前设备；新增商品请从收货入口录入并确认。") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it }, Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(28.dp),
                    placeholder = { Text("搜索商品、批次、库位") },
                    leadingIcon = { Icon(Icons.Default.Search, null) })
                IconButton(onClick = { showFilters = true }) { Icon(Icons.Default.FilterAlt, "分类和库位筛选", tint = WarehousePalette.Navy) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StockFilter.entries.forEach { choice ->
                    val selected = filter == choice
                    FilterChip(selected = selected, onClick = { filterName = choice.name },
                        label = {
                            Text(choice.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                                maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else WarehousePalette.Navy)
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, if (selected) Color.Transparent else WarehousePalette.Border),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.45f),
                            selectedContainerColor = WarehousePalette.Teal, selectedLabelColor = Color.White))
                }
            }
            if (category.isNotEmpty() || location.isNotEmpty()) {
                Text(listOf(category, location).filter { it.isNotEmpty() }.joinToString(" · "),
                    color = WarehousePalette.Muted, fontSize = 12.sp)
            }
        }
        if (products.isEmpty()) {
            item { WarehouseEmpty(if (state.products.isEmpty()) "尚无仓储商品" else "没有符合条件的商品",
                if (state.products.isEmpty()) "从收货入口创建第一条草稿，核对后确认入库。" else "试试修改关键词或筛选条件。") }
        }
        items(products, key = { it.id }) { product ->
            val tone = product.stockStatus
            Surface(
                onClick = { onProduct(product.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = tone.background,
                border = BorderStroke(1.dp, tone.border),
                shadowElevation = 1.dp
            ) {
                Row(Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(tone.background, tone.backgroundEnd)))
                    .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb(accent = tone.accent, background = Color.White.copy(alpha = 0.7f))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(product.name, color = WarehousePalette.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(product.availableQuantity.toString() + " " + product.unit,
                            color = if (tone.isStockWarning) tone.accent else WarehousePalette.Blue,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
                        Text((product.sortedBatches.firstOrNull()?.location ?: "暂无可用批次") + " · " + product.batches.size + " 批",
                            color = WarehousePalette.Muted, fontSize = 12.sp, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StockStatusTag(tone)
                        Icon(Icons.Default.ChevronRight, "查看库存详情", tint = WarehousePalette.Muted)
                    }
                }
            }
        }
        item { WarehouseAction("收货入库", onReceive) }
    }
    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("筛选商品") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("分类", fontWeight = FontWeight.Bold)
                    (listOf("") + state.products.map { it.category }.distinct()).forEach { value ->
                        TextButton(onClick = { category = value }) { Text((if (category == value) "✓ " else "") + value.ifEmpty { "全部分类" }) }
                    }
                    Text("库位", fontWeight = FontWeight.Bold)
                    (listOf("") + state.products.flatMap { it.batches.map { batch -> batch.location } }.distinct()).forEach { value ->
                        TextButton(onClick = { location = value }) { Text((if (location == value) "✓ " else "") + value.ifEmpty { "全部库位" }) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilters = false }) { Text("完成") } },
            dismissButton = { TextButton(onClick = { category = ""; location = ""; showFilters = false }) { Text("清除") } }
        )
    }
}

@Composable
fun WarehouseProductDetailScreen(
    product: WarehouseProductUi?,
    preview: Boolean,
    movements: List<WarehouseMovementUi>,
    onBack: () -> Unit,
    onReceive: () -> Unit,
    onOutbound: (String?) -> Unit,
    onTransfer: () -> Unit
) {
    var expanded by rememberSaveable(product?.id) { mutableStateOf(false) }
    var selectedBatch by remember(product?.id) { mutableStateOf<WarehouseBatchUi?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().background(WarehousePalette.Background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { WarehouseHeader("库存详情", onBack) }
        item { WarehouseNotice(preview) }
        if (product == null) {
            item { WarehouseEmpty("商品暂不可用", "返回库存列表选择其他商品。") }
        } else {
            item {
                val status = product.stockStatus
                WarehouseCard(color = status.background) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProductThumb(Modifier.size(92.dp), status.accent, Color.White.copy(alpha = 0.7f))
                        Column(Modifier.weight(1f).padding(start = 18.dp)) {
                            Text(product.name, color = WarehousePalette.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            StockStatusTag(status)
                            Text("可用 " + product.availableQuantity + " " + product.unit, color = status.accent,
                                fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            Text(product.sortedBatches.firstOrNull()?.location ?: "暂无批次", color = WarehousePalette.Muted)
                        }
                    }
                }
            }
            item {
                InventoryActionPanel(product.availableQuantity > 0, onReceive, { onOutbound(null) }, onTransfer)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("库存批次", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy)
                    TextButton(onClick = { expanded = !expanded }, enabled = product.sortedBatches.isNotEmpty()) {
                        Text("共 ${product.batches.size} 批 · ${if (expanded) "收起" else "展开"}", color = WarehousePalette.Muted, fontSize = 12.sp)
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (expanded) "收起全部批次" else "展开全部批次", tint = WarehousePalette.Muted, modifier = Modifier.size(22.dp))
                    }
                }
            }
            item {
                WarehouseCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("批次明细", color = WarehousePalette.Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        if (product.sortedBatches.isEmpty()) {
                            Text("暂无有库存的批次，确认收货后会显示在这里。", color = WarehousePalette.Muted, fontSize = 13.sp)
                        } else {
                            (if (expanded) product.sortedBatches else product.sortedBatches.take(1)).forEach { batch ->
                                InventoryBatchRow(batch, product.unit, onClick = { selectedBatch = batch })
                            }
                        }
                        HorizontalDivider(color = WarehousePalette.Border.copy(alpha = 0.6f))
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("合计可用  ", color = WarehousePalette.Muted, fontSize = 16.sp)
                            Text("${product.availableQuantity} ${product.unit}", color = WarehousePalette.Teal, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { InventoryMovementPanel(product.id, movements, preview) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    selectedBatch?.let { batch ->
        AlertDialog(
            onDismissRequest = { selectedBatch = null },
            title = { Text("批次 " + batch.code) },
            text = { Text("效期：" + (batch.expiry?.toString() ?: "未设置") + "\n库位：" + batch.location + "\n可用数量：" + (if (batch.available()) batch.quantity else 0) + " " + product?.unit) },
            confirmButton = {
                TextButton(onClick = { selectedBatch = null; onOutbound(batch.id) }, enabled = batch.available()) { Text("从此批次出库") }
            },
            dismissButton = { TextButton(onClick = { selectedBatch = null }) { Text("关闭") } }
        )
    }
}
