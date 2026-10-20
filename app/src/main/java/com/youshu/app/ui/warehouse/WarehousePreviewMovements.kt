package com.youshu.app.ui.warehouse

import java.time.LocalDate

/** Illustrative records for the isolated UI preview. Never written to the inventory database. */
internal fun warehousePreviewMovements(products: List<WarehouseProductUi>): List<WarehouseMovementUi> {
    val today = LocalDate.now()
    val movements = mutableListOf<WarehouseMovementUi>()
    products.forEachIndexed { index, product ->
        val supplier = when (product.category) {
            "乳品" -> "青禾乳品批发（示例）"
            "饮料" -> "汇源饮品配送（示例）"
            else -> "万家食品批发（示例）"
        }
        val price = when (product.id) {
            "milk" -> "2.60"
            "cracker" -> "42.00"
            "water" -> "18.00"
            "noodle" -> "36.00"
            else -> "2.20"
        }
        product.batches.forEachIndexed { batchIndex, batch ->
            // Receive a little more than the current quantity, then illustrate the later changes.
            val withdrawal = if (batchIndex == 0) 3 + index else 0
            val correction = if (product.id == "milk" && batchIndex == 0) -1 else 0
            val received = batch.quantity + withdrawal - correction
            val receiveDay = today.minusDays(7L - batchIndex.coerceAtMost(2))
            movements += WarehouseMovementUi(product.id, product.name, batch.code, EntryDirection.INBOUND,
                received, product.unit, "收货 · ${if (product.id == "milk" && batchIndex == 2) "冷藏区 / A-01" else batch.location}",
                receiveDay.atTime(9, 20 + index), supplier = supplier, purchasePrice = price)
            if (withdrawal > 0) {
                movements += WarehouseMovementUi(product.id, product.name, batch.code, EntryDirection.OUTBOUND,
                    withdrawal, product.unit, "补到货架 · 门店销售区", today.atTime(9, 10 + index * 5), reason = "补到货架")
            }
            if (correction != 0) {
                movements += WarehouseMovementUi(product.id, product.name, batch.code, EntryDirection.COUNT,
                    correction, product.unit, "盘点 9 → 8 · 复核货架实物", today.atTime(10, 5))
            }
            if (product.id == "milk" && batchIndex == 2) {
                movements += WarehouseMovementUi(product.id, product.name, batch.code, EntryDirection.TRANSFER,
                    batch.quantity, product.unit, "移库 冷藏区 / A-01 → ${batch.location}", today.minusDays(1).atTime(15, 30))
            }
        }
    }
    return movements.sortedByDescending { it.time }
}
