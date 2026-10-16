package com.youshu.app.ui.warehouse

/** Extracts only explicitly labelled fields. The operator still reviews the whole draft. */
data class WarehouseVoiceFields(
    val productName: String? = null,
    val quantity: Int? = null,
    val unit: String? = null,
    val batchCode: String? = null,
    val expiry: String? = null,
    val location: String? = null,
    val destination: String? = null
) {
    val isEmpty: Boolean get() = productName == null && quantity == null && unit == null &&
        batchCode == null && expiry == null && location == null && destination == null
}

object WarehouseVoiceDraftParser {
    private val field = Regex(
        "(商品名称|商品|数量|单位|批次|效期|库位|去向)\\s*[:：]?\\s*(.*?)" +
            "(?=\\s*(?:商品名称|商品|数量|单位|批次|效期|库位|去向)\\s*[:：]?|[，,。\\n]|$)"
    )

    fun parse(text: String): WarehouseVoiceFields {
        val values = field.findAll(text).mapNotNull { match ->
            val value = match.groupValues[2].trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            match.groupValues[1] to value
        }.toMap()
        val quantity = values["数量"]?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }
        return WarehouseVoiceFields(
            productName = values["商品名称"] ?: values["商品"],
            quantity = quantity,
            unit = values["单位"], batchCode = values["批次"], expiry = values["效期"],
            location = values["库位"], destination = values["去向"]
        )
    }
}
