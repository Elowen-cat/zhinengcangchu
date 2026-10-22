package com.youshu.app.ui.warehouse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WarehousePreviewSimulationTest {
    private fun draft(direction: EntryDirection, quantity: Int, reviewed: Boolean = true) = StockDraftUi(
        id = direction.name + quantity,
        direction = direction,
        source = EntrySource.MANUAL,
        productId = "milk",
        productName = "鲜牛奶 250ml",
        quantity = quantity,
        unit = "瓶",
        batchId = if (direction == EntryDirection.OUTBOUND) "milk-near" else null,
        batchCode = "B20261010",
        expiry = java.time.LocalDate.now().plusDays(3).toString(),
        location = "冷藏区 / A-02",
        reason = if (direction == EntryDirection.OUTBOUND) "补到货架" else "收货",
        destination = if (direction == EntryDirection.OUTBOUND) "销售区" else "冷藏区 / A-02",
        supplier = "示例乳品批发商",
        purchasePrice = "2.60",
        reviewed = reviewed
    )

    @Test fun inboundThenOutboundChangesPreviewStockAndLedger() {
        val state = WarehouseUiState()
        state.enablePreview()
        val initialMovements = state.movements.toList()
        assertEquals(24, state.product("milk")?.availableQuantity)

        state.save(draft(EntryDirection.INBOUND, 5))
        assertEquals(24, state.product("milk")?.availableQuantity)
        assertNull(state.simulateConfirmation(EntryDirection.INBOUND))
        assertEquals(29, state.product("milk")?.availableQuantity)
        assertEquals(13, state.product("milk")?.batches?.first { it.id == "milk-near" }?.quantity)

        state.save(draft(EntryDirection.OUTBOUND, 2))
        assertNull(state.simulateConfirmation(EntryDirection.OUTBOUND))
        assertEquals(27, state.product("milk")?.availableQuantity)
        assertEquals(listOf(EntryDirection.OUTBOUND, EntryDirection.INBOUND), state.movements.take(2).map { it.direction })
        assertEquals(initialMovements, state.movements.drop(2))
        assertEquals("示例乳品批发商", state.movements[1].supplier)
        assertEquals("2.60", state.movements[1].purchasePrice)
        assertTrue(state.drafts.isEmpty())
    }

    @Test fun uncheckedAndExcessOutboundDraftsDoNotChangeStock() {
        val state = WarehouseUiState()
        state.enablePreview()
        val initialMovements = state.movements.toList()
        state.save(draft(EntryDirection.OUTBOUND, 2, reviewed = false))
        assertNotNull(state.simulateConfirmation(EntryDirection.OUTBOUND))
        state.save(draft(EntryDirection.OUTBOUND, 2))
        state.save(draft(EntryDirection.OUTBOUND, 9))
        assertNotNull(state.simulateConfirmation(EntryDirection.OUTBOUND))
        assertEquals(24, state.product("milk")?.availableQuantity)
        assertEquals(initialMovements, state.movements.toList())
    }

    @Test fun sampleLedgerBalancesMatchEachBatchAndShowPurchaseMetadata() {
        val state = WarehouseUiState()
        state.enablePreview()
        assertTrue(state.previewMode)
        assertEquals(EntryDirection.entries.toSet(), state.movements.map { it.direction }.toSet())
        state.products.forEach { product ->
            product.batches.forEach { batch ->
                val balance = state.movements.filter { it.productId == product.id && it.batchCode == batch.code }
                    .sumOf { when (it.direction) {
                        EntryDirection.INBOUND, EntryDirection.COUNT -> it.quantity
                        EntryDirection.OUTBOUND -> -it.quantity
                        EntryDirection.TRANSFER -> 0
                    } }
                assertEquals("${product.name} ${batch.code}", batch.quantity, balance)
            }
        }
        val purchases = state.movements.filter { it.direction == EntryDirection.INBOUND }
        assertTrue(purchases.isNotEmpty())
        assertTrue(purchases.all { it.supplier.isNotBlank() && it.purchasePrice.toBigDecimal() > java.math.BigDecimal.ZERO })
        assertTrue(state.movements.filter { it.direction != EntryDirection.INBOUND }
            .all { it.supplier.isBlank() && it.purchasePrice.isBlank() })
        assertTrue(state.drafts.isEmpty())
    }

    @Test fun previewResetDoesNotDuplicateExampleRecords() {
        val state = WarehouseUiState()
        state.enablePreview()
        val original = state.movements.toList()
        state.enablePreview()
        assertEquals(original, state.movements.toList())
    }

    @Test fun mixedSourcesRemainPendingTogetherUntilEveryInboundRowIsReviewed() {
        val state = WarehouseUiState().apply { enablePreview() }
        val originalMovements = state.movements.toList()
        val photo = draft(EntryDirection.INBOUND, 5, reviewed = false).copy(source = EntrySource.PHOTO)
        val voice = draft(EntryDirection.INBOUND, 3).copy(source = EntrySource.VOICE, transcript = "鲜牛奶三瓶")
        val outbound = draft(EntryDirection.OUTBOUND, 2)
        listOf(photo, voice, outbound).forEach(state::save)

        assertEquals(2, state.drafts.count { it.direction == EntryDirection.INBOUND })
        assertNotNull(state.simulateConfirmation(EntryDirection.INBOUND))
        assertEquals(24, state.product("milk")?.availableQuantity)
        assertEquals(originalMovements, state.movements.toList())

        state.save(photo.copy(reviewed = true))
        assertNull(state.simulateConfirmation(EntryDirection.INBOUND))
        assertEquals(32, state.product("milk")?.availableQuantity)
        assertEquals(listOf(outbound), state.drafts.toList())
        assertEquals(2, state.movements.size - originalMovements.size)
        assertEquals(setOf(3, 5), state.movements.take(2).map { it.quantity }.toSet())
    }

    @Test fun editedVoiceDraftKeepsSourceAndIdentityButMustBeReviewedAgain() {
        val state = WarehouseUiState().apply { enablePreview() }
        val voice = draft(EntryDirection.INBOUND, 3).copy(source = EntrySource.VOICE, transcript = "鲜牛奶三瓶")
        state.save(voice)
        state.save(voice.copy(quantity = 4, reviewed = false))
        assertEquals(1, state.drafts.size)
        assertEquals(EntrySource.VOICE, state.drafts.single().source)
        assertEquals("鲜牛奶三瓶", state.drafts.single().transcript)
        assertNotNull(state.simulateConfirmation(EntryDirection.INBOUND))
        assertEquals(24, state.product("milk")?.availableQuantity)
    }
}
