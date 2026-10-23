package com.youshu.app.ui.warehouse

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WarehouseOutboundModelsTest {
    private fun state() = WarehouseUiState().apply {
        products.add(WarehouseProductUi("milk", "鲜牛奶", "乳品", "瓶", 3, listOf(
            WarehouseBatchUi("first", "A", 8, LocalDate.now().plusDays(3), "冷藏区"),
            WarehouseBatchUi("second", "B", 16, LocalDate.now().plusDays(30), "冷藏区"),
            WarehouseBatchUi("expired", "C", 100, LocalDate.now().minusDays(1), "冷藏区"),
            WarehouseBatchUi("empty", "D", 0, null, "冷藏区")
        )))
    }

    private fun draft(id: String, quantity: Int, batchId: String = "first") = StockDraftUi(
        id, EntryDirection.OUTBOUND, EntrySource.PHOTO, "milk", "鲜牛奶", quantity, "瓶",
        batchId, "A", "", "冷藏区", "补到货架", "销售区", "", ""
    )

    @Test fun sameBatchReservationsReduceBatchAndProductRemainders() {
        val state = state().apply { save(draft("other", 2)) }
        val result = outboundStock(state, "milk", "first", 6)
        assertEquals(6L, result.batchFree)
        assertEquals(0L, result.batchRemaining)
        assertEquals(16L, result.productRemaining)
        assertNull(result.error)
    }

    @Test fun differentBatchesOnlyReduceProductRemainder() {
        val state = state().apply { save(draft("other", 10, "second")) }
        val result = outboundStock(state, "milk", "first", 6)
        assertEquals(8L, result.batchFree)
        assertEquals(2L, result.batchRemaining)
        assertEquals(8L, result.productRemaining)
        assertNull(result.error)
    }

    @Test fun editingExcludesOnlyTheCurrentDraft() {
        val state = state().apply {
            save(draft("editing", 7))
            save(draft("other", 2))
        }
        assertNotNull(outboundStock(state, "milk", "first", 6).error)
        val result = outboundStock(state, "milk", "first", 6, "editing")
        assertNull(result.error)
        assertEquals(6L, result.batchFree)
        assertEquals(16L, result.productRemaining)
    }

    @Test fun queueChecksAggregateReservationsRatherThanEachRowInIsolation() {
        val state = state().apply {
            save(draft("a", 5))
            save(draft("b", 4))
        }
        assertNotNull(outboundQueueError(state))
        state.save(draft("b", 3))
        assertNull(outboundQueueError(state))
    }

    @Test fun invalidAndMissingSelectionsAreRejected() {
        val state = state()
        assertNotNull(outboundStock(state, null, "first", 1).error)
        assertNotNull(outboundStock(state, "missing", "first", 1).error)
        assertNotNull(outboundStock(state, "milk", null, 1).error)
        assertNotNull(outboundStock(state, "milk", "missing", 1).error)
        assertNotNull(outboundStock(state, "milk", "expired", 1).error)
        assertNotNull(outboundStock(state, "milk", "empty", 1).error)
        assertNotNull(outboundStock(state, "milk", "first", null).error)
        assertNotNull(outboundStock(state, "milk", "first", 0).error)
        assertNotNull(outboundStock(state, "milk", "first", -1).error)
        assertNotNull(outboundStock(state, "milk", "first", 9).error)
    }

    @Test fun nonOutboundAndOtherProductsDoNotReserveStock() {
        val state = state().apply {
            EntryDirection.entries.filter { it != EntryDirection.OUTBOUND }.forEach {
                save(draft(it.name, 100).copy(direction = it))
            }
            save(draft("other-product", 100).copy(productId = "other"))
        }
        val result = outboundStock(state, "milk", "first", 8)
        assertNull(result.error)
        assertEquals(8L, result.batchFree)
        assertEquals(16L, result.productRemaining)
    }

    @Test fun zeroAndNegativeQueueQuantitiesBlockConfirmation() {
        val state = state().apply { save(draft("invalid", 0)) }
        assertNotNull(outboundQueueError(state))
        state.save(draft("invalid", -5))
        assertNotNull(outboundQueueError(state))
        // Invalid negative entries cannot create availability on another row.
        assertEquals(8L, outboundStock(state, "milk", "first", 1).batchFree)
    }

    @Test fun everySourceAndReviewStateReservesStock() {
        val state = state().apply {
            save(draft("photo", 2).copy(reviewed = true))
            save(draft("voice", 2).copy(source = EntrySource.VOICE))
            save(draft("manual", 2).copy(source = EntrySource.MANUAL))
        }
        assertEquals(2L, outboundStock(state, "milk", "first", 2).batchFree)
        assertNull(outboundQueueError(state))
    }

    @Test fun queueRequiresReasonAndDestinationButAnEmptyQueueIsValid() {
        val state = state()
        assertNull(outboundQueueError(state))
        state.save(draft("a", 1).copy(reason = " "))
        assertNotNull(outboundQueueError(state))
        state.save(draft("a", 1).copy(destination = " "))
        assertNotNull(outboundQueueError(state))
        state.save(draft("a", 1))
        assertNull(outboundQueueError(state))
    }

    @Test fun longSumsHandleLargeInventoryAndReservationsWithoutWrapping() {
        val state = WarehouseUiState().apply {
            products.add(WarehouseProductUi("milk", "鲜牛奶", "乳品", "瓶", 0, listOf(
                WarehouseBatchUi("first", "A", Int.MAX_VALUE, null, "A"),
                WarehouseBatchUi("second", "B", Int.MAX_VALUE, null, "B")
            )))
        }
        assertEquals(4_294_967_293L, outboundStock(state, "milk", "first", 1).productRemaining)
        state.save(draft("a", Int.MAX_VALUE))
        state.save(draft("b", Int.MAX_VALUE))
        val result = outboundStock(state, "milk", "first", 1)
        assertEquals(-2_147_483_647L, result.batchFree)
        assertEquals(-2_147_483_648L, result.batchRemaining)
        assertEquals(-1L, result.productRemaining)
        assertNotNull(result.error)
        assertNotNull(outboundQueueError(state))
    }
}
