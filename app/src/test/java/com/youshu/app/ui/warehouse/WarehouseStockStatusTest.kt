package com.youshu.app.ui.warehouse

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarehouseStockStatusTest {
    private val today = LocalDate.of(2026, 10, 19)

    private fun product(id: String, quantity: Int, safety: Int = 4, expiry: LocalDate? = null) =
        WarehouseProductUi(id, id, "饮品", "瓶", safety,
            listOf(WarehouseBatchUi("$id-lot", "QA", quantity, expiry, "A01")))

    @Test fun zeroAndPositiveLowStockHaveDifferentStatusesAndFilterTargets() {
        val empty = WarehouseStockStatus.forProduct(product("empty", 0), today)
        val low = WarehouseStockStatus.forProduct(product("low", 2), today)
        assertEquals(WarehouseStockStatus.OUT_OF_STOCK, empty)
        assertEquals(StockFilter.EMPTY, empty.filter)
        assertEquals(WarehouseStockStatus.LOW, low)
        assertEquals(StockFilter.LOW, low.filter)
    }

    @Test fun zeroStockRemainsAnAlertEvenWithoutASafetyStockPolicy() {
        val tasks = warehousePendingTasks(listOf(product("empty", 0, safety = 0)), today)
        assertEquals(1, tasks.size)
        assertEquals(WarehouseStockStatus.OUT_OF_STOCK, tasks.single().status)
    }

    @Test fun expiredUnitsDoNotHideOutOfStockAndAvailableNearExpiryKeepsItsAlert() {
        assertEquals(WarehouseStockStatus.OUT_OF_STOCK,
            WarehouseStockStatus.forProduct(product("expired", 10, expiry = today.minusDays(1)), today))
        assertEquals(WarehouseStockStatus.EXPIRING,
            WarehouseStockStatus.forProduct(product("near", 2, expiry = today.plusDays(1)), today))
    }

    @Test fun pendingListIncludesEveryWarningOnceAndPrioritizesOutOfStock() {
        val tasks = warehousePendingTasks(listOf(
            product("normal", 10), product("low", 2),
            product("near", 1, expiry = today.plusDays(2)), product("empty", 0)), today)
        assertEquals(listOf("empty", "near", "low"), tasks.map { it.name })
        assertEquals(listOf(StockFilter.EMPTY, StockFilter.NEAR, StockFilter.LOW), tasks.map { it.status.filter })
    }

    @Test fun ReplenishingAProductRemovesTheOutOfStockAlertAndEventuallyClearsIt() {
        val empty = product("water", 0)
        val low = empty.copy(batches = listOf(empty.batches.single().copy(quantity = 2)))
        val replenished = empty.copy(batches = listOf(empty.batches.single().copy(quantity = 4)))
        assertEquals(WarehouseStockStatus.OUT_OF_STOCK, warehousePendingTasks(listOf(empty), today).single().status)
        assertEquals(WarehouseStockStatus.LOW, warehousePendingTasks(listOf(low), today).single().status)
        assertTrue(warehousePendingTasks(listOf(replenished), today).isEmpty())
    }
}
