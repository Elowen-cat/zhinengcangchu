package com.youshu.app.ui.warehouse

import java.time.LocalDate

/** A local estimate from confirmed warehouse withdrawals, not sales or a purchase order. */
data class ReplenishmentResult(
    val product: WarehouseProductUi,
    val effectiveOutbound: Long,
    val excludedOutbound: Long,
    val dailyOutbound: Double,
    val available: Long,
    val pendingInbound: Long,
    val targetDemand: Long,
    val suggestedQuantity: Long,
    val coverageDays: Double?,
    val dailyQuantities: List<Int>
)

object WarehouseReplenishment {
    const val WINDOW_DAYS = 14
    const val EFFECTIVE_REASON = "补到货架"

    fun calculate(
        product: WarehouseProductUi,
        movements: List<WarehouseMovementUi>,
        drafts: List<StockDraftUi>,
        today: LocalDate = LocalDate.now()
    ): ReplenishmentResult {
        val firstDay = today.minusDays((WINDOW_DAYS - 1).toLong())
        val recentOutbound = movements.filter {
            it.productId == product.id && it.direction == EntryDirection.OUTBOUND &&
                !it.time.toLocalDate().isBefore(firstDay) && !it.time.toLocalDate().isAfter(today)
        }
        val effective = recentOutbound.filter { it.reason == EFFECTIVE_REASON }
        val effectiveQuantity = effective.sumOf { it.quantity.toLong() }
        val excludedQuantity = recentOutbound.sumOf { it.quantity.toLong() } - effectiveQuantity
        val available = product.batches.filter { it.available(today) }.sumOf { it.quantity.toLong() }
        val pending = drafts.filter {
            it.direction == EntryDirection.INBOUND &&
                (it.productId == product.id || it.productName.trim() == product.name)
        }.sumOf { it.quantity.coerceAtLeast(0).toLong() }
        val targetDemand = (effectiveQuantity * product.targetCoverageDays + WINDOW_DAYS - 1) / WINDOW_DAYS
        val suggestion = (targetDemand + product.safetyStock - available - pending).coerceAtLeast(0)
        val byDay = effective.groupBy { it.time.toLocalDate() }
        val daily = (0 until WINDOW_DAYS).map { offset ->
            val date = firstDay.plusDays(offset.toLong())
            byDay[date].orEmpty().sumOf { it.quantity }
        }
        return ReplenishmentResult(
            product, effectiveQuantity, excludedQuantity, effectiveQuantity.toDouble() / WINDOW_DAYS,
            available, pending, targetDemand, suggestion,
            if (effectiveQuantity == 0L) null else available * WINDOW_DAYS.toDouble() / effectiveQuantity,
            daily
        )
    }

}
