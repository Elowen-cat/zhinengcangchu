package com.youshu.app.ui.warehouse

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Acceptance examples for the analysis page, independent of cloud recognition. */
class WarehouseAnalysisAcceptanceTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val product = WarehouseProductUi("water", "矿泉水", "饮品", "瓶", 4,
        listOf(WarehouseBatchUi("lot", "QA", 1, null, "A01")))

    private fun movement(quantity: Int, daysAgo: Long = 0, reason: String = "补到货架",
        direction: EntryDirection = EntryDirection.OUTBOUND, productId: String = "water") =
        WarehouseMovementUi(productId, "矿泉水", "QA", direction, quantity, "瓶", reason,
            today.minusDays(daysAgo).atTime(12, 0), reason)

    private fun pending(quantity: Int, productId: String? = "water", name: String = "矿泉水",
        direction: EntryDirection = EntryDirection.INBOUND) =
        StockDraftUi("pending-$quantity-$productId", direction, EntrySource.MANUAL, productId,
            name, quantity, "瓶", null, "NEW", "", "A01", "收货", "A01", "", "")

    @Test fun windowIncludesTodayAndDayThirteenButExcludesOlderAndFutureMovements() {
        val result = WarehouseReplenishment.calculate(product,
            listOf(movement(2), movement(3, 13), movement(100, 14), movement(100, -1)), emptyList(), today)
        assertEquals(5L, result.effectiveOutbound)
        assertEquals(14, result.dailyQuantities.size)
        assertEquals(3, result.dailyQuantities.first())
        assertEquals(2, result.dailyQuantities.last())
        assertEquals(5, result.dailyQuantities.sum())
    }

    @Test fun onlyThisProductsReplenishmentOutboundCountsAsDemand() {
        val result = WarehouseReplenishment.calculate(product, listOf(
            movement(1), movement(2, reason = "报损"), movement(3, reason = "其他"),
            movement(50, direction = EntryDirection.INBOUND),
            movement(50, direction = EntryDirection.COUNT),
            movement(50, direction = EntryDirection.TRANSFER),
            movement(50, productId = "another-product")), emptyList(), today)
        assertEquals(1L, result.effectiveOutbound)
        assertEquals(5L, result.excludedOutbound)
        assertEquals(1, result.dailyQuantities.sum())
    }

    @Test fun expiredAndZeroStockDoNotReduceThePurchaseSuggestion() {
        val stock = product.copy(batches = listOf(
            WarehouseBatchUi("expired", "OLD", 100, today.minusDays(1), "A01"),
            WarehouseBatchUi("today", "TODAY", 2, today, "A01"),
            WarehouseBatchUi("unknown", "UNKNOWN", 3, null, "A01"),
            WarehouseBatchUi("zero", "ZERO", 0, today.plusDays(1), "A01")))
        val result = WarehouseReplenishment.calculate(stock, listOf(movement(14)), emptyList(), today)
        assertEquals(5L, result.available)
        assertEquals(7L, result.targetDemand)
        assertEquals(6L, result.suggestedQuantity)
    }

    @Test fun pendingInboundIsDeductedButOtherProductsAndOutboundDraftsAreNot() {
        val result = WarehouseReplenishment.calculate(product, emptyList(), listOf(
            pending(1), pending(1, productId = null),
            pending(100, productId = "other", name = "牛奶"),
            pending(100, direction = EntryDirection.OUTBOUND)), today)
        assertEquals(2L, result.pendingInbound)
        assertEquals(1L, result.available)
        assertEquals(1L, result.suggestedQuantity)
    }

    @Test fun demandRoundsUpAndCoveragePolicyRecalculatesTheSuggestion() {
        val sevenDays = WarehouseReplenishment.calculate(product, listOf(movement(1)), emptyList(), today)
        val twentyOneDays = WarehouseReplenishment.calculate(product.copy(targetCoverageDays = 21),
            listOf(movement(1)), emptyList(), today)
        assertEquals(1L, sevenDays.targetDemand)
        assertEquals(4L, sevenDays.suggestedQuantity)
        assertEquals(2L, twentyOneDays.targetDemand)
        assertEquals(5L, twentyOneDays.suggestedQuantity)
    }

    @Test fun excessStockNeverSuggestsNegativePurchasesAndNoDemandHasNoCoverage() {
        val result = WarehouseReplenishment.calculate(product.copy(batches = listOf(
            WarehouseBatchUi("stock", "FULL", 100, null, "A01"))), emptyList(), emptyList(), today)
        assertEquals(0L, result.suggestedQuantity)
        assertEquals(0L, result.targetDemand)
        assertNull(result.coverageDays)
    }
}
