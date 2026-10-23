package com.youshu.app.ui.warehouse

/** Stock after other pending outbound drafts, without modifying inventory. */
internal data class OutboundStock(
    val batchFree: Long,
    val batchRemaining: Long,
    val productRemaining: Long,
    val error: String?
)

internal fun outboundStock(
    state: WarehouseUiState,
    productId: String?,
    batchId: String?,
    quantity: Int?,
    excludingDraftId: String? = null
): OutboundStock {
    val product = state.product(productId)
    val batch = product?.batches?.firstOrNull { it.id == batchId }
    val reservations = state.drafts.filter {
        it.direction == EntryDirection.OUTBOUND && it.id != excludingDraftId && it.productId == productId
    }
    // An invalid negative draft must never increase the stock available to another draft.
    val productReserved = reservations.sumOf { it.quantity.coerceAtLeast(0).toLong() }
    val batchReserved = reservations.filter { it.batchId == batchId }
        .sumOf { it.quantity.coerceAtLeast(0).toLong() }
    val batchFree = (batch?.takeIf { it.available() }?.quantity?.toLong() ?: 0L) - batchReserved
    val requested = quantity?.coerceAtLeast(0)?.toLong() ?: 0L
    val productRemaining = (product?.batches?.filter { it.available() }?.sumOf { it.quantity.toLong() } ?: 0L) -
        productReserved - requested
    val batchRemaining = batchFree - requested
    val error = when {
        product == null -> "请选择库存商品"
        batch == null || !batch.available() -> "请选择有可用库存且未过期的批次"
        quantity == null || quantity <= 0 -> "请输入大于 0 的整数数量"
        batchRemaining < 0 -> "出库数量超过该批次可用库存（已计入其他待确认记录）"
        else -> null
    }
    return OutboundStock(batchFree, batchRemaining, productRemaining, error)
}

/** Validates the entire outbound queue, including reservations made by every other row. */
internal fun outboundQueueError(state: WarehouseUiState): String? {
    for (draft in state.drafts.filter { it.direction == EntryDirection.OUTBOUND }) {
        val stock = outboundStock(state, draft.productId, draft.batchId, draft.quantity, draft.id)
        val error = stock.error ?: when {
            draft.reason.isBlank() -> "请选择出库原因"
            draft.destination.isBlank() -> "请填写出库去向"
            else -> null
        }
        if (error != null) return "${draft.productName.ifBlank { "待确认商品" }}：$error"
    }
    return null
}
