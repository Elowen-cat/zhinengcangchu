package com.youshu.app.ui.warehouse

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

/** One severity order and visual language for stock across the app. */
enum class WarehouseStockStatus(
    val label: String,
    val accent: Color,
    val background: Color,
    val backgroundEnd: Color,
    val badgeBackground: Color,
    val filter: StockFilter,
    val alertNote: String
) {
    OUT_OF_STOCK("缺货", Color(0xFFA61D32), Color(0xFFF6BDC7), Color(0xFFFFDBE2),
        Color(0xFFA61D32), StockFilter.EMPTY, "可用库存为 0，请及时补货"),
    EXPIRING("临期", WarehousePalette.Orange, Color(0xFFFFF2E1), Color(0xFFFFFAF2),
        Color(0xFFFFEBD1), StockFilter.NEAR, "批次进入临期窗口"),
    LOW("低库存", Color(0xFFCF3C50), Color(0xFFFFEDF0), Color(0xFFFFF7F9),
        Color(0xFFFCE1E6), StockFilter.LOW, "低于安全库存"),
    NORMAL("正常", WarehousePalette.Teal, Color(0xFFEBF9F4), Color(0xFFF7FCFC),
        Color(0xFFDDF4ED), StockFilter.ALL, "库存正常");

    val badgeText: Color get() = if (this == OUT_OF_STOCK) Color.White else accent
    val border: Color get() = if (this == OUT_OF_STOCK) Color(0xFFD77788) else Color.White
    val isStockWarning: Boolean get() = this == OUT_OF_STOCK || this == LOW

    companion object {
        fun forProduct(product: WarehouseProductUi, today: LocalDate = LocalDate.now()): WarehouseStockStatus {
            val available = product.batches.filter { it.available(today) }.sumOf { it.quantity.toLong() }
            return when {
                available == 0L -> OUT_OF_STOCK
                product.batches.any { it.quantity > 0 && it.status(today) == BatchStatus.NEAR } -> EXPIRING
                available < product.safetyStock -> LOW
                else -> NORMAL
            }
        }
    }
}

val WarehouseProductUi.stockStatus: WarehouseStockStatus
    get() = WarehouseStockStatus.forProduct(this)

fun warehousePendingTasks(products: List<WarehouseProductUi>, today: LocalDate = LocalDate.now()): List<PreviewTask> =
    products.mapNotNull { product ->
        val status = WarehouseStockStatus.forProduct(product, today)
        if (status == WarehouseStockStatus.NORMAL) null
        else PreviewTask(product.name, status.alertNote, status, true)
    }.sortedBy { it.status.ordinal }
