package com.youshu.app.ui.warehouse

/** Opt-in visual examples, never sent to recognition or written to the formal database. */
internal fun photoReceivePreviewSamples(packageName: String): List<PhotoReceiveLine> {
    val products = warehousePreviewProducts()
    return listOf("milk", "cracker").map { id ->
        val product = products.first { it.id == id }
        val batch = product.sortedBatches.first()
        PhotoReceiveLine(id = "photo-demo-$id", photoUri = "android.resource://$packageName/drawable/photo_demo_$id",
            name = product.name, quantity = if (id == "milk") "24" else "36", unit = product.unit,
            batchCode = batch.code, expiry = batch.expiry?.toString().orEmpty(), location = batch.location,
            supplier = if (id == "milk") "青禾乳品批发（示例）" else "万家食品批发（示例）",
            price = if (id == "milk") "2.60" else "42.00", demo = true)
    }
}
