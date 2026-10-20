package com.youshu.app.ui.warehouse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarehouseVoiceDraftParserTest {
    @Test fun labelledTranscriptExtractsEditableFields() {
        val fields = WarehouseVoiceDraftParser.parse("商品 SKU001，数量 5，单位 UNIT，批次 LOT002，库位 B03")
        assertEquals("SKU001", fields.productName)
        assertEquals(5, fields.quantity)
        assertEquals("UNIT", fields.unit)
        assertEquals("LOT002", fields.batchCode)
        assertEquals("B03", fields.location)
    }

    @Test fun unlabelledSpeechDoesNotGuessInventoryData() {
        assertTrue(WarehouseVoiceDraftParser.parse("送几箱牛奶到货架").isEmpty)
    }
}
