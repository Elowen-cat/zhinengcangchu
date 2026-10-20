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
        supplier = "",
        purchasePrice = "",
        reviewed = reviewed
    )

    @Test fun inboundThenOutboundChangesPreviewStockAndLedger() {
        val state = WarehouseUiState()
        state.enablePreview()
        assertEquals(24, state.product("milk")?.availableQuantity)

        state.save(draft(EntryDirection.INBOUND, 5))
        assertEquals(24, state.product("milk")?.availableQuantity)
        assertNull(state.simulateConfirmation(EntryDirection.INBOUND))
        assertEquals(29, state.product("milk")?.availableQuantity)
        assertEquals(13, state.product("milk")?.batches?.first { it.id == "milk-near" }?.quantity)

        state.save(draft(EntryDirection.OUTBOUND, 2))
        assertNull(state.simulateConfirmation(EntryDirection.OUTBOUND))
        assertEquals(27, state.product("milk")?.availableQuantity)
        assertEquals(listOf(EntryDirection.OUTBOUND, EntryDirection.INBOUND), state.movements.map { it.direction })
        assertTrue(state.drafts.isEmpty())
    }

    @Test fun uncheckedAndExcessOutboundDraftsDoNotChangeStock() {
        val state = WarehouseUiState()
        state.enablePreview()
        state.save(draft(EntryDirection.OUTBOUND, 2, reviewed = false))
        assertNotNull(state.simulateConfirmation(EntryDirection.OUTBOUND))
        state.save(draft(EntryDirection.OUTBOUND, 2))
        state.save(draft(EntryDirection.OUTBOUND, 9))
        assertNotNull(state.simulateConfirmation(EntryDirection.OUTBOUND))
        assertEquals(24, state.product("milk")?.availableQuantity)
        assertTrue(state.movements.isEmpty())
    }
}
