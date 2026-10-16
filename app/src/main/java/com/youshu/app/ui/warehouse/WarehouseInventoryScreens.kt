package com.youshu.app.ui.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.temporal.ChronoUnit
import java.time.LocalDate

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
                    FilterChip(selected = filter == choice, onClick = { filterName = choice.name },
                        label = { Text(choice.label, fontSize = 12.sp, color = if (filter == choice) androidx.compose.ui.graphics.Color.White else WarehousePalette.Navy) }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WarehousePalette.Teal, selectedLabelColor = androidx.compose.ui.graphics.Color.White))
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
            WarehouseCard {
                Row(Modifier.fillMaxWidth().clickable { onProduct(product.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb()
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(product.name, color = WarehousePalette.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(product.availableQuantity.toString() + " " + product.unit, color = WarehousePalette.Blue,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
                        Text((product.sortedBatches.firstOrNull()?.location ?: "暂无可用批次") + " · " + product.batches.size + " 批",
                            color = WarehousePalette.Muted, fontSize = 12.sp, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            product.availableQuantity == 0 -> StatusTag("缺货", WarehousePalette.Red)
                            product.isNear -> StatusTag("临期", WarehousePalette.Orange)
                            product.isLow -> StatusTag("低库存", WarehousePalette.Red)
                            else -> StatusTag("正常")
                        }
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
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedBatch by remember { mutableStateOf<WarehouseBatchUi?>(null) }
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
                WarehouseCard {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProductThumb(Modifier.size(92.dp))
                        Column(Modifier.padding(start = 18.dp)) {
                            Text(product.name, color = WarehousePalette.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("可用 " + product.availableQuantity + " " + product.unit, color = WarehousePalette.Teal,
                                fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            Text(product.sortedBatches.firstOrNull()?.location ?: "暂无批次", color = WarehousePalette.Muted)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WarehouseAction("入库", onReceive, Modifier.weight(1f))
                    WarehouseAction("出库", { onOutbound(null) }, Modifier.weight(1f), enabled = product.availableQuantity > 0)
                    WarehouseAction("移库", onTransfer, Modifier.weight(1f), enabled = product.availableQuantity > 0)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("库存批次", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy)
                    Text("共 " + product.batches.size + " 批", color = WarehousePalette.Muted)
                }
            }
            if (product.sortedBatches.isEmpty()) {
                item { WarehouseEmpty("暂无可用批次", "收货确认后，这里会显示数量、效期和库位。") }
            } else if (!expanded) {
                item {
                    BatchCard(product.sortedBatches.first(), product.unit, onClick = { expanded = true }, expand = true)
                }
            } else {
                item {
                    TextButton(onClick = { expanded = false }) { Icon(Icons.Default.ExpandLess, null); Text("收起全部批次") }
                    Text("按临期、优先出库、正常排序；同状态按效期排序。", color = WarehousePalette.Muted, fontSize = 12.sp)
                }
                items(product.sortedBatches, key = { it.id }) { batch ->
                    BatchCard(batch, product.unit, onClick = { selectedBatch = batch }, expand = false)
                }
                item { Text("合计可用 " + product.availableQuantity + " " + product.unit, color = WarehousePalette.Teal, fontWeight = FontWeight.Bold) }
            }
            item {
                WarehouseCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("库存流水", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WarehousePalette.Navy)
                        if (movements.isNotEmpty()) {
                            movements.take(5).forEach { movement ->
                                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text((if (preview) "模拟" else "") + movement.direction.label, color = WarehousePalette.Navy, fontWeight = FontWeight.SemiBold)
                                        Text(movement.batchCode + " · " + movement.note, color = WarehousePalette.Muted, fontSize = 12.sp)
                                    }
                                    val amount = when (movement.direction) {
                                        EntryDirection.INBOUND -> "+${movement.quantity}"
                                        EntryDirection.OUTBOUND -> "−${movement.quantity}"
                                        EntryDirection.COUNT -> when {
                                            movement.quantity > 0 -> "+${movement.quantity}"
                                            movement.quantity < 0 -> "−${-movement.quantity}"
                                            else -> "0"
                                        }
                                        EntryDirection.TRANSFER -> "移 ${movement.quantity}"
                                    }
                                    Text(amount + " " + movement.unit,
                                        color = if (movement.direction == EntryDirection.OUTBOUND) WarehousePalette.Blue else WarehousePalette.Teal,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(if (preview) "暂无模拟流水；可在入库或出库草稿中完成模拟确认" else "暂无库存流水；确认入库或出库后将在这里显示",
                                color = WarehousePalette.Muted, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
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

@Composable
private fun BatchCard(batch: WarehouseBatchUi, unit: String, onClick: () -> Unit, expand: Boolean) {
    val status = batch.status()
    WarehouseCard {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(108.dp).background(status.color()))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(batch.code, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarehousePalette.Navy)
                Text("效期 " + (batch.expiry?.toString() ?: "未设置"), color = WarehousePalette.Muted, modifier = Modifier.padding(top = 8.dp))
                Text("可用 " + (if (batch.available()) batch.quantity else 0) + " " + unit, color = WarehousePalette.Blue, fontWeight = FontWeight.Bold)
                Text(batch.location, color = WarehousePalette.Muted, fontSize = 12.sp)
                Text(
                    batch.expiry?.let {
                        val days = ChronoUnit.DAYS.between(LocalDate.now(), it)
                        if (days < 0) "已过期 " + -days + " 天" else if (days == 0L) "今天到期" else days.toString() + " 天后到期"
                    } ?: "效期需人工核对",
                    color = status.color(), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusTag(status.label, status.color())
                Icon(if (expand) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    if (expand) "展开全部批次" else "查看批次", tint = WarehousePalette.Muted, modifier = Modifier.padding(top = 12.dp).size(32.dp))
            }
        }
    }
}
