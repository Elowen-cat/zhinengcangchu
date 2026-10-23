package com.youshu.app.ui.warehouse

/** Opt-in Debug examples, never written to Room or sent to recognition. */
internal fun pendingOutboundPreviewSamples(packageName: String): List<StockDraftUi> {
    val products = warehousePreviewProducts()
    return listOf("milk", "cracker", "noodle").mapIndexed { index, id ->
        val product = products.first { it.id == id }
        val batch = product.sortedBatches.first { it.available() }
        val source = if (index == 1) EntrySource.VOICE else EntrySource.PHOTO
        StockDraftUi("outbound-demo-$id", EntryDirection.OUTBOUND, source, product.id, product.name,
            listOf(6, 2, 1)[index], product.unit, batch.id, batch.code, batch.expiry?.toString().orEmpty(), batch.location,
            if (index == 1) "报损" else WarehouseReplenishment.EFFECTIVE_REASON,
            if (index == 1) "报损区" else "销售区", "", "",
            imageUris = if (id in listOf("milk", "cracker")) listOf("android.resource://$packageName/drawable/photo_demo_$id") else emptyList(),
            transcript = if (source == EntrySource.VOICE) "苏打饼干2箱，报损，送到报损区" else "", reviewed = false)
    }
}
