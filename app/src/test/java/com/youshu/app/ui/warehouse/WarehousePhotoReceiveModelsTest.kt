package com.youshu.app.ui.warehouse

import com.youshu.app.data.warehouse.WarehouseRecognitionFields
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate

class WarehousePhotoReceiveModelsTest {
    private fun valid(id: String = "line-a", image: String = "file:///photo-a.jpg") = PhotoReceiveLine(
        id = id, photoUri = image, name = "鲜牛奶 250ml", quantity = "24", unit = "瓶",
        batchCode = "MILK-A", expiry = LocalDate.now().plusDays(10).toString(), location = "冷藏区 / A-02",
        supplier = "示例乳品批发", price = "2.60"
    )

    @Test fun explicitEditsAndClearsSurviveRecognitionRetry() {
        val line = valid().edit("quantity", "7").edit("batchCode", "").edit("name", "手动名称")
            .applyRecognition(WarehouseRecognitionFields(productName = "模型名称", quantity = 99,
                batchCode = "MODEL-B", location = "新建议库位", purchasePrice = "3.10"))
        assertEquals("7", line.quantity)
        assertEquals("", line.batchCode)
        assertEquals("手动名称", line.name)
        assertEquals("新建议库位", line.location)
        assertEquals("3.10", line.price)
        assertTrue(line.recognized)
        assertEquals("请补充批次", line.validationError)
    }

    @Test fun missingSuggestionsDoNotCreateDefaults() {
        val line = PhotoReceiveLine(photoUri = "file:///water.jpg")
            .applyRecognition(WarehouseRecognitionFields(productName = "矿泉水"))
        assertEquals("矿泉水", line.name)
        assertEquals("", line.quantity)
        assertEquals("", line.unit)
        assertEquals("", line.batchCode)
        assertEquals("", line.location)
        assertNotNull(line.validationError)
    }

    @Test fun lineToDraftKeepsItsIdentityAttachmentAndMetadataButRequiresReview() {
        val product = WarehouseProductUi("milk-id", "鲜牛奶 250ml", "乳品", "瓶", 8, emptyList())
        val first = valid().toDraft(listOf(product))
        val second = valid("line-b", "file:///photo-b.jpg").copy(name = "苏打饼干", unit = "盒", batchCode = "CRACKER-B")
            .toDraft(listOf(product))
        assertEquals("line-a", first.id)
        assertEquals("milk-id", first.productId)
        assertEquals(listOf("file:///photo-a.jpg"), first.imageUris)
        assertEquals("示例乳品批发", first.supplier)
        assertEquals("2.60", first.purchasePrice)
        assertFalse(first.reviewed)
        assertEquals(EntryDirection.INBOUND, first.direction)
        assertEquals(EntrySource.PHOTO, first.source)
        assertEquals("line-b", second.id)
        assertEquals(listOf("file:///photo-b.jpg"), second.imageUris)
        assertNull(second.productId)
    }

    @Test fun editingOldMultiPhotoDraftKeepsOneLineAllPhotosAndProtectsFields() {
        val old = valid().toDraft(emptyList()).copy(id = "old-draft", reviewed = true,
            imageUris = listOf("file:///front.jpg", "file:///back.jpg"))
        val line = PhotoReceiveLine.fromDraft(old).applyRecognition(WarehouseRecognitionFields(
            productName = "不应覆盖", quantity = 1, batchCode = "BAD", unit = "箱", location = "BAD"))
        val edited = line.toDraft(emptyList())
        assertEquals("old-draft", line.id)
        assertEquals("鲜牛奶 250ml", line.name)
        assertEquals("24", line.quantity)
        assertEquals(old.imageUris, edited.imageUris)
        assertEquals(old.batchCode, edited.batchCode)
        assertFalse(edited.reviewed)
    }

    @Test fun validationRejectsUnsafeAmountsDatesAndPrices() {
        val row = valid()
        assertNull(row.validationError)
        listOf("0", "-1", "1.5", "2147483648", "", "数量").forEach {
            assertNotNull("quantity=$it", row.copy(quantity = it).validationError)
        }
        assertNotNull(row.copy(expiry = "2026-99-99").validationError)
        assertNotNull(row.copy(expiry = LocalDate.now().minusDays(1).toString()).validationError)
        assertNull(row.copy(expiry = "").validationError)
        listOf("-0.01", "NaN", "Infinity", "1e999", "bad").forEach {
            assertNotNull("price=$it", row.copy(price = it).validationError)
        }
        assertNull(row.copy(price = "0").validationError)
    }

    @Test fun savedMetadataRoundTripsWithoutBitmapsAndStillProtectsEditedFields() {
        val line = valid().edit("quantity", "8").copy(error = "请重试", generation = "request-a")
        val bytes = ByteArrayOutputStream().also { output ->
            ObjectOutputStream(output).use { it.writeObject(arrayListOf(line)) }
        }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as ArrayList<*>
        assertEquals(line, restored.single())
        assertEquals("8", (restored.single() as PhotoReceiveLine)
            .applyRecognition(WarehouseRecognitionFields(quantity = 400)).quantity)
    }
}
