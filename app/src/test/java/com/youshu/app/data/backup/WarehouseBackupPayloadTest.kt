package com.youshu.app.data.backup

import com.youshu.app.data.local.entity.WarehouseBatchEntity
import com.youshu.app.data.local.entity.WarehouseDocumentEntity
import com.youshu.app.data.local.entity.WarehouseMovementEntity
import com.youshu.app.data.local.entity.WarehouseProductEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WarehouseBackupPayloadTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun warehouseTablesRoundTripInBackupManifest() {
        val warehouse = WarehouseBackupSnapshot(
            products = listOf(WarehouseProductEntity("p1", "Milk", "Dairy", "box", 2, 1L)),
            batches = listOf(WarehouseBatchEntity("b1", "p1", "LOT1", 8, null, "A02")),
            documents = listOf(WarehouseDocumentEntity("d1", "CONFIRMED", "INBOUND", "MANUAL", "p1", "Milk", 8,
                "box", "b1", "LOT1", "", "A02", "收货", "A02", "", "", "", "", true, 1L, 2L)),
            movements = listOf(WarehouseMovementEntity("m1", "d1", "p1", "Milk", "b1", "LOT1", "INBOUND", 8, "box", "收货", 2L))
        )
        val encoded = json.encodeToString(AppBackupPayload(1L, emptyList(), emptyList(), emptyList(), warehouse))
        val decoded = json.decodeFromString<AppBackupPayload>(encoded)
        assertEquals(warehouse, decoded.warehouse)
    }

    @Test fun legacyManifestWithoutWarehouseIsStillReadable() {
        val old = """{"exportedAt":1,"categories":[],"locations":[],"items":[]}"""
        assertNull(json.decodeFromString<AppBackupPayload>(old).warehouse)
    }

    @Test fun previousWarehouseProductGetsDefaultCoverageOnImport() {
        val oldProduct = """{"id":"p1","name":"Milk","category":"Dairy","unit":"box","safetyStock":2,"createdAt":1}"""
        assertEquals(7, json.decodeFromString<WarehouseProductEntity>(oldProduct).targetCoverageDays)
    }
}
