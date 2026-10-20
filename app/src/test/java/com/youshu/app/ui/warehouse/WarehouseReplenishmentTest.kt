package com.youshu.app.ui.warehouse

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WarehouseReplenishmentTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val product = WarehouseProductUi(
        "milk", "鲜牛奶", "乳品", "瓶", 8,
        listOf(WarehouseBatchUi("lot", "LOT1", 24, today.plusDays(5), "A02"))
    )

    private fun outbound(quantity: Int, reason: String, daysAgo: Long = 0) = WarehouseMovementUi(
        "milk", "鲜牛奶", "LOT1", EntryDirection.OUTBOUND, quantity, "瓶", reason,
        today.minusDays(daysAgo).atTime(12, 0), reason
    )

    @Test fun fixedExampleUsesFourPerDayAndSuggestsTwelve() {
        val result = WarehouseReplenishment.calculate(product,
            listOf(outbound(56, "补到货架"), outbound(10, "报损")), emptyList(), today)
        assertEquals(56L, result.effectiveOutbound)
        assertEquals(10L, result.excludedOutbound)
        assertEquals(4.0, result.dailyOutbound, 0.001)
        assertEquals(28L, result.targetDemand)
        assertEquals(12L, result.suggestedQuantity)
        assertEquals(6.0, result.coverageDays!!, 0.001)
        assertEquals(56, result.dailyQuantities.last())
    }

    @Test fun pendingInboundReducesSuggestionButOldAndUnknownOutboundDoNotIncreaseIt() {
        val pending = StockDraftUi("d1", EntryDirection.INBOUND, EntrySource.MANUAL,
            "milk", "鲜牛奶", 5, "瓶", null, "LOT2", "", "A02", "收货", "A02", "", "")
        val result = WarehouseReplenishment.calculate(product,
            listOf(outbound(56, "补到货架"), outbound(14, "其他"), outbound(28, "补到货架", 14)),
            listOf(pending), today)
        assertEquals(56L, result.effectiveOutbound)
        assertEquals(14L, result.excludedOutbound)
        assertEquals(5L, result.pendingInbound)
        assertEquals(7L, result.suggestedQuantity)
    }

    @Test fun noDemandUsesSafetyStockAndHasNoCoverageEstimate() {
        val result = WarehouseReplenishment.calculate(product.copy(safetyStock = 30), emptyList(), emptyList(), today)
        assertEquals(6L, result.suggestedQuantity)
        assertNull(result.coverageDays)
    }
}
