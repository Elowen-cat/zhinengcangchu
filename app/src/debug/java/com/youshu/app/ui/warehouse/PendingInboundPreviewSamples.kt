package com.youshu.app.ui.warehouse

/** Opt-in mixed draft examples, isolated from formal Room inventory and recognition requests. */
internal fun pendingInboundPreviewSamples(packageName: String): List<StockDraftUi> {
    val products = warehousePreviewProducts()
    val photos = photoReceivePreviewSamples(packageName).map { it.toDraft(products) }
    val water = products.first { it.id == "water" }
    val batch = water.sortedBatches.first()
    val voice = StockDraftUi(
        id = "pending-demo-voice-water", direction = EntryDirection.INBOUND, source = EntrySource.VOICE,
        productId = water.id, productName = water.name, quantity = 48, unit = water.unit,
        batchId = batch.id, batchCode = batch.code, expiry = batch.expiry.toString(), location = batch.location,
        reason = "", destination = "", supplier = "", purchasePrice = "",
        transcript = "矿泉水550毫升，48箱，批次${batch.code}，库位${batch.location}", reviewed = true
    )
    return listOf(photos[0], photos[1].copy(reviewed = true), voice)
}
