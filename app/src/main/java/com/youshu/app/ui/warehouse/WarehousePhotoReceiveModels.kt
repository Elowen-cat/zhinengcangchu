package com.youshu.app.ui.warehouse

import com.youshu.app.data.warehouse.WarehouseRecognitionFields
import java.io.Serializable
import java.time.LocalDate
import java.util.UUID

/** One independently editable candidate; metadata only, never a bitmap or posted stock record. */
data class PhotoReceiveLine(
    val id: String = UUID.randomUUID().toString(),
    val photoUri: String,
    val attachments: List<String> = listOf(photoUri),
    val sourceUri: String = photoUri,
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
    val batchCode: String = "",
    val expiry: String = "",
    val location: String = "",
    val supplier: String = "",
    val price: String = "",
    val working: Boolean = false,
    val recognized: Boolean = false,
    val error: String? = null,
    val demo: Boolean = false,
    val touched: Set<String> = emptySet(),
    val generation: String = ""
) : Serializable {
    val validationError: String?
        get() = when {
            name.isBlank() -> "请补充商品名称"
            unit.isBlank() -> "请补充基础单位"
            quantity.trim().toIntOrNull()?.let { it > 0 } != true -> "数量应为大于 0 的整数"
            batchCode.isBlank() -> "请补充批次"
            location.isBlank() -> "请补充库位"
            listOf(name, unit, batchCode, location, supplier).any { it.length > 120 } -> "字段内容过长，请控制在 120 字以内"
            expiry.isNotBlank() && runCatching { LocalDate.parse(expiry.trim()) }.isFailure -> "效期格式应为 YYYY-MM-DD"
            expiry.isNotBlank() && LocalDate.parse(expiry.trim()).isBefore(LocalDate.now()) -> "效期已过，请核对日期"
            price.isNotBlank() && (price.length > 40 || price.trim().toDoubleOrNull()?.let { it.isFinite() && it >= 0 } != true) -> "单件进价应为非负数"
            else -> null
        }

    fun edit(field: String, value: String): PhotoReceiveLine {
        if (field !in EDITABLE_FIELDS) return this
        val changed = when (field) {
            "name" -> copy(name = value)
            "quantity" -> copy(quantity = value)
            "unit" -> copy(unit = value)
            "batchCode" -> copy(batchCode = value)
            "expiry" -> copy(expiry = value)
            "location" -> copy(location = value)
            "supplier" -> copy(supplier = value)
            "price" -> copy(price = value)
            else -> this
        }
        return changed.copy(touched = touched + field)
    }

    /** Retries may refresh model suggestions, but never replace an explicitly edited field. */
    fun applyRecognition(fields: WarehouseRecognitionFields): PhotoReceiveLine {
        fun choose(field: String, current: String, suggested: String?) =
            if (field in touched || suggested.isNullOrBlank()) current else suggested.trim()
        return copy(
            name = choose("name", name, fields.productName),
            quantity = choose("quantity", quantity, fields.quantity?.toString()),
            unit = choose("unit", unit, fields.unit),
            batchCode = choose("batchCode", batchCode, fields.batchCode),
            expiry = choose("expiry", expiry, fields.expiry),
            location = choose("location", location, fields.location),
            supplier = choose("supplier", supplier, fields.supplier),
            price = choose("price", price, fields.purchasePrice),
            recognized = !fields.productName.isNullOrBlank(), working = false, error = null
        )
    }

    fun toDraft(products: List<WarehouseProductUi>): StockDraftUi {
        require(validationError == null) { validationError.orEmpty() }
        val match = products.firstOrNull { it.name.trim() == name.trim() && it.unit.trim() == unit.trim() }
        return StockDraftUi(
            id = id, direction = EntryDirection.INBOUND, source = EntrySource.PHOTO,
            productId = match?.id, productName = name.trim(), quantity = quantity.trim().toInt(), unit = unit.trim(),
            batchId = null, batchCode = batchCode.trim(), expiry = expiry.trim(), location = location.trim(),
            reason = "收货", destination = location.trim(), supplier = supplier.trim(), purchasePrice = price.trim(),
            imageUris = attachments.filter { it.isNotBlank() }.distinct(), reviewed = false
        )
    }

    companion object {
        val EDITABLE_FIELDS = setOf("name", "quantity", "unit", "batchCode", "expiry", "location", "supplier", "price")

        /** Older drafts may have several photos; editing still represents exactly one original draft. */
        fun fromDraft(draft: StockDraftUi): PhotoReceiveLine = PhotoReceiveLine(
            id = draft.id, photoUri = draft.imageUris.firstOrNull().orEmpty(), attachments = draft.imageUris.toList(),
            name = draft.productName, quantity = draft.quantity.toString(), unit = draft.unit,
            batchCode = draft.batchCode, expiry = draft.expiry, location = draft.location,
            supplier = draft.supplier, price = draft.purchasePrice, touched = EDITABLE_FIELDS
        )
    }
}
