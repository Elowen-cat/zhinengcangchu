package com.youshu.app.ui.warehouse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.WarehouseDocumentEntity
import com.youshu.app.data.repository.WarehouseRepository
import com.youshu.app.data.repository.WarehouseValidationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

enum class StockFilter(val label: String) { ALL("全部"), LOW("低库存"), NEAR("临期"), EMPTY("缺货") }
enum class EntryDirection(val label: String) { INBOUND("入库"), OUTBOUND("出库"), COUNT("盘点"), TRANSFER("移库") }
enum class EntrySource(val label: String) { MANUAL("手动"), PHOTO("拍照"), VOICE("语音") }
enum class BatchStatus(val label: String) { EXPIRED("已过期"), NEAR("临期"), PRIORITY("优先出库"), NORMAL("正常") }

data class WarehouseBatchUi(
    val id: String,
    val code: String,
    val quantity: Int,
    val expiry: LocalDate?,
    val location: String,
    val priority: Boolean = false
) {
    fun status(today: LocalDate = LocalDate.now()): BatchStatus = when {
        expiry != null && expiry.isBefore(today) -> BatchStatus.EXPIRED
        expiry != null && !expiry.isAfter(today.plusDays(7)) -> BatchStatus.NEAR
        priority -> BatchStatus.PRIORITY
        else -> BatchStatus.NORMAL
    }
    fun available(today: LocalDate = LocalDate.now()) = quantity > 0 && status(today) != BatchStatus.EXPIRED
}

data class WarehouseProductUi(
    val id: String,
    val name: String,
    val category: String,
    val unit: String,
    val safetyStock: Int,
    val batches: List<WarehouseBatchUi>,
    val targetCoverageDays: Int = 7
) {
    val availableQuantity: Int get() = batches.filter { it.available() }.sumOf { it.quantity }
    val isLow: Boolean get() = availableQuantity > 0 && availableQuantity < safetyStock
    val isNear: Boolean get() = batches.any { it.quantity > 0 && it.status() == BatchStatus.NEAR }
    val sortedBatches: List<WarehouseBatchUi> get() = batches.filter { it.quantity > 0 }
        .sortedWith(compareBy<WarehouseBatchUi> { it.status().ordinal }.thenBy { it.expiry ?: LocalDate.MAX })
    fun matches(filter: StockFilter): Boolean = when (filter) {
        StockFilter.ALL -> true
        StockFilter.LOW -> isLow
        StockFilter.NEAR -> isNear
        StockFilter.EMPTY -> availableQuantity == 0
    }
}

data class StockDraftUi(
    val id: String,
    val direction: EntryDirection,
    val source: EntrySource,
    val productId: String?,
    val productName: String,
    val quantity: Int,
    val unit: String,
    val batchId: String?,
    val batchCode: String,
    val expiry: String,
    val location: String,
    val reason: String,
    val destination: String,
    val supplier: String,
    val purchasePrice: String,
    val imageUris: List<String> = emptyList(),
    val transcript: String = "",
    val reviewed: Boolean = false
)

data class WarehouseMovementUi(
    val productId: String,
    val productName: String,
    val batchCode: String,
    val direction: EntryDirection,
    val quantity: Int,
    val unit: String,
    val note: String,
    val time: LocalDateTime = LocalDateTime.now(),
    val reason: String = "",
    val supplier: String = "",
    val purchasePrice: String = ""
)

class WarehouseUiState {
    var previewMode by mutableStateOf(false)
    var persistenceError by mutableStateOf<String?>(null)
    val products = mutableStateListOf<WarehouseProductUi>()
    val drafts = mutableStateListOf<StockDraftUi>()
    val movements = mutableStateListOf<WarehouseMovementUi>()
    fun product(id: String?) = products.firstOrNull { it.id == id }
    fun draft(id: String?) = drafts.firstOrNull { it.id == id }
    fun save(draft: StockDraftUi) {
        val index = drafts.indexOfFirst { it.id == draft.id }
        if (index < 0) drafts.add(draft) else drafts[index] = draft
    }
    fun enablePreview() {
        previewMode = true
        products.clear()
        products.addAll(warehousePreviewProducts())
        drafts.clear()
        movements.clear()
        movements.addAll(warehousePreviewMovements(products.toList()))
    }

    /** Applies reviewed drafts to the isolated Debug preview only. Returns an error or null on success. */
    fun simulateConfirmation(direction: EntryDirection): String? {
        if (!previewMode) return "仅界面预览可模拟确认"
        val lines = drafts.filter { it.direction == direction }
        if (lines.isEmpty()) return "没有待确认草稿"
        if (lines.any { !it.reviewed }) return "请先核对全部草稿"
        if (lines.any { it.quantity <= 0 }) return "数量必须大于 0"
        if (direction == EntryDirection.OUTBOUND) {
            val groups = lines.groupBy { it.productId to it.batchId }
            for ((key, entries) in groups) {
                val batch = product(key.first)?.batches?.firstOrNull { it.id == key.second }
                if (batch == null || !batch.available() || entries.sumOf { it.quantity.toLong() } > batch.quantity) {
                    return "批次库存不足或已过期，请修改出库草稿"
                }
            }
        } else {
            if (lines.groupBy { it.productName }.any { (_, entries) -> entries.map { it.unit }.distinct().size > 1 }) {
                return "同名商品的基础单位不一致"
            }
            for ((key, entries) in lines.groupBy { it.productName to it.batchCode }) {
                if (entries.map { it.location to it.expiry }.distinct().size > 1) return "同批次的效期或库位不一致"
                val existingQuantity = products.firstOrNull { it.name == key.first }
                    ?.batches?.firstOrNull { it.code == key.second }?.quantity ?: 0
                if (entries.sumOf { it.quantity.toLong() } + existingQuantity > Int.MAX_VALUE) return "批次数量超出范围"
            }
            for (line in lines) {
                if (line.productName.isBlank() || line.unit.isBlank() || line.batchCode.isBlank() || line.location.isBlank()) {
                    return "请补全商品、单位、批次和库位"
                }
                val date = line.expiry.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (line.expiry.isNotBlank() && (date == null || date.isBefore(LocalDate.now()))) return "请核对效期"
                val match = products.firstOrNull { it.name == line.productName }
                if (match != null && match.unit != line.unit) return "同名商品的基础单位不一致"
                val sameCode = match?.batches?.firstOrNull { it.code == line.batchCode }
                if (sameCode != null && (sameCode.location != line.location || sameCode.expiry != date)) {
                    return "同批次的效期或库位不一致"
                }
            }
        }
        lines.forEach { line ->
            if (direction == EntryDirection.INBOUND) {
                val index = products.indexOfFirst { it.name == line.productName }
                val current = products.getOrNull(index)
                val date = line.expiry.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
                val batchIndex = current?.batches?.indexOfFirst { it.code == line.batchCode } ?: -1
                val batch = if (batchIndex >= 0) {
                    current!!.batches[batchIndex].let { it.copy(quantity = it.quantity + line.quantity) }
                } else {
                    WarehouseBatchUi(UUID.randomUUID().toString(), line.batchCode, line.quantity, date, line.location)
                }
                val product = if (current == null) {
                    WarehouseProductUi(UUID.randomUUID().toString(), line.productName, "未分类", line.unit, 0, listOf(batch))
                } else {
                    current.copy(batches = current.batches.toMutableList().apply {
                        if (batchIndex >= 0) set(batchIndex, batch) else add(batch)
                    })
                }
                if (index >= 0) products[index] = product else products.add(product)
                movements.add(0, WarehouseMovementUi(product.id, product.name, line.batchCode, direction, line.quantity, line.unit,
                    "收货 · ${line.location}", supplier = line.supplier, purchasePrice = line.purchasePrice))
            } else {
                val index = products.indexOfFirst { it.id == line.productId }
                val current = products[index]
                products[index] = current.copy(batches = current.batches.map { batch ->
                    if (batch.id == line.batchId) batch.copy(quantity = batch.quantity - line.quantity) else batch
                })
                movements.add(0, WarehouseMovementUi(current.id, current.name, line.batchCode, direction, line.quantity, line.unit, "${line.reason} · ${line.destination}", reason = line.reason))
            }
        }
        drafts.removeAll { it.direction == direction }
        return null
    }
}

@HiltViewModel
class WarehouseUiViewModel @Inject constructor(private val repository: WarehouseRepository) : ViewModel() {
    val state = WarehouseUiState()

    init {
        viewModelScope.launch {
            combine(repository.observeProducts(), repository.observeBatches()) { products, batches ->
                products.map { product ->
                    WarehouseProductUi(product.id, product.name, product.category, product.unit, product.safetyStock,
                        batches.filter { it.productId == product.id }.map { batch ->
                            WarehouseBatchUi(batch.id, batch.code, batch.quantity,
                                batch.expiry?.let { runCatching { LocalDate.parse(it) }.getOrNull() }, batch.location, batch.priority)
                        }, product.targetCoverageDays)
                }
            }.collect { products ->
                if (!state.previewMode) {
                    state.products.clear()
                    state.products.addAll(products)
                }
            }
        }
        viewModelScope.launch {
            repository.observePendingDocuments().collect { documents ->
                if (!state.previewMode) {
                    state.drafts.clear()
                    state.drafts.addAll(documents.map { it.toUi() })
                }
            }
        }
        viewModelScope.launch {
            combine(repository.observeMovements(), repository.observeConfirmedDocuments()) { rows, documents ->
                val documentsById = documents.associateBy { it.id }
                rows.map { it to documentsById[it.documentId] }
            }.collect { rows ->
                if (!state.previewMode) {
                    state.movements.clear()
                    state.movements.addAll(rows.map { (row, document) ->
                        WarehouseMovementUi(row.productId, row.productName, row.batchCode,
                            EntryDirection.valueOf(row.direction), row.quantity, row.unit, row.note,
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(row.createdAt), ZoneId.systemDefault()),
                            reason = document?.reason.orEmpty(),
                            supplier = if (row.direction == EntryDirection.INBOUND.name) document?.supplier.orEmpty() else "",
                            purchasePrice = if (row.direction == EntryDirection.INBOUND.name) document?.purchasePrice.orEmpty() else "")
                    })
                }
            }
        }
    }

    fun saveDraft(draft: StockDraftUi, done: (String?) -> Unit) = saveDrafts(listOf(draft), done)

    fun saveDrafts(drafts: List<StockDraftUi>, done: (String?) -> Unit) {
        if (drafts.isEmpty() || drafts.map { it.id }.distinct().size != drafts.size) {
            done("请核对待保存的草稿")
            return
        }
        if (state.previewMode) {
            drafts.forEach(state::save)
            done(null)
            return
        }
        viewModelScope.launch {
            try {
                repository.savePendingBatch(drafts.map { it.toEntity() })
                done(null)
            } catch (e: Exception) {
                done(e.message ?: "草稿保存失败")
            }
        }
    }

    fun reviewDraft(draft: StockDraftUi) {
        state.save(draft)
        if (!state.previewMode) viewModelScope.launch {
            try {
                repository.savePending(draft.toEntity())
                state.persistenceError = null
            } catch (e: Exception) {
                state.persistenceError = e.message ?: "核对状态保存失败"
            }
        }
    }

    fun deleteDraft(id: String) {
        state.drafts.removeAll { it.id == id }
        if (!state.previewMode) viewModelScope.launch {
            try {
                repository.deletePending(id)
                state.persistenceError = null
            } catch (e: Exception) {
                state.persistenceError = e.message ?: "删除草稿失败"
            }
        }
    }

    fun confirm(direction: EntryDirection, done: (String?) -> Unit) {
        if (state.previewMode) {
            done(state.simulateConfirmation(direction))
            return
        }
        viewModelScope.launch {
            try {
                repository.confirm(direction.name)
                done(null)
            } catch (e: WarehouseValidationException) {
                done(e.message ?: "请核对单据")
            } catch (e: Exception) {
                done("记账失败，请稍后重试")
            }
        }
    }

    fun updateReplenishmentPolicy(id: String, safetyStock: Int, targetCoverageDays: Int, done: (String?) -> Unit) {
        if (state.previewMode) {
            val index = state.products.indexOfFirst { it.id == id }
            if (index < 0) { done("商品不存在"); return }
            if (safetyStock < 0 || targetCoverageDays !in 1..90) { done("请核对安全库存和覆盖天数"); return }
            state.products[index] = state.products[index].copy(safetyStock = safetyStock, targetCoverageDays = targetCoverageDays)
            done(null)
            return
        }
        viewModelScope.launch {
            try {
                repository.updateReplenishmentPolicy(id, safetyStock, targetCoverageDays)
                done(null)
            } catch (e: WarehouseValidationException) {
                done(e.message ?: "配置保存失败")
            } catch (e: Exception) {
                done("配置保存失败，请稍后重试")
            }
        }
    }

    fun countBatch(batchId: String, observed: Int, note: String, done: (String?) -> Unit) {
        if (state.previewMode) {
            val index = state.products.indexOfFirst { product -> product.batches.any { it.id == batchId } }
            val product = state.products.getOrNull(index)
            val batch = product?.batches?.firstOrNull { it.id == batchId }
            if (batch == null || observed < 0 || note.isBlank()) { done("请核对批次、实盘数量和说明"); return }
            state.products[index] = product.copy(batches = product.batches.map {
                if (it.id == batchId) it.copy(quantity = observed) else it
            })
            state.movements.add(0, WarehouseMovementUi(product.id, product.name, batch.code, EntryDirection.COUNT,
                observed - batch.quantity, product.unit, "盘点 ${batch.quantity} → $observed · ${note.trim()}"))
            done(null)
            return
        }
        viewModelScope.launch {
            try { repository.countBatch(batchId, observed, note); done(null) }
            catch (e: WarehouseValidationException) { done(e.message ?: "盘点失败") }
            catch (e: Exception) { done("盘点失败，请稍后重试") }
        }
    }

    fun transferBatch(batchId: String, destination: String, done: (String?) -> Unit) {
        if (state.previewMode) {
            val index = state.products.indexOfFirst { product -> product.batches.any { it.id == batchId } }
            val product = state.products.getOrNull(index)
            val batch = product?.batches?.firstOrNull { it.id == batchId }
            val target = destination.trim()
            if (batch == null || batch.quantity <= 0 || target.isBlank() || target == batch.location) {
                done("请核对批次和目标库位"); return
            }
            state.products[index] = product.copy(batches = product.batches.map {
                if (it.id == batchId) it.copy(location = target) else it
            })
            state.movements.add(0, WarehouseMovementUi(product.id, product.name, batch.code, EntryDirection.TRANSFER,
                batch.quantity, product.unit, "移库 ${batch.location} → $target"))
            done(null)
            return
        }
        viewModelScope.launch {
            try { repository.transferBatch(batchId, destination); done(null) }
            catch (e: WarehouseValidationException) { done(e.message ?: "移库失败") }
            catch (e: Exception) { done("移库失败，请稍后重试") }
        }
    }
}

private fun StockDraftUi.toEntity() = WarehouseDocumentEntity(
    id = id, status = "PENDING", direction = direction.name, source = source.name,
    productId = productId, productName = productName, quantity = quantity, unit = unit,
    batchId = batchId, batchCode = batchCode, expiry = expiry, location = location,
    reason = reason, destination = destination, supplier = supplier, purchasePrice = purchasePrice,
    imageUris = imageUris.joinToString("\n"), transcript = transcript, reviewed = reviewed,
    createdAt = System.currentTimeMillis()
)

private fun WarehouseDocumentEntity.toUi() = StockDraftUi(
    id = id, direction = EntryDirection.valueOf(direction), source = EntrySource.valueOf(source),
    productId = productId, productName = productName, quantity = quantity, unit = unit,
    batchId = batchId, batchCode = batchCode, expiry = expiry, location = location,
    reason = reason, destination = destination, supplier = supplier, purchasePrice = purchasePrice,
    imageUris = imageUris.lineSequence().filter { it.isNotBlank() }.toList(), transcript = transcript, reviewed = reviewed
)

fun warehousePreviewProducts(): List<WarehouseProductUi> {
    val today = LocalDate.now()
    return listOf(
        WarehouseProductUi("milk", "鲜牛奶 250ml", "乳品", "瓶", 8, listOf(
            WarehouseBatchUi("milk-near", "B20261010", 8, today.plusDays(3), "冷藏区 / A-02"),
            WarehouseBatchUi("milk-priority", "B20261022", 6, today.plusDays(18), "冷藏区 / A-02", true),
            WarehouseBatchUi("milk-normal", "B20261028", 10, today.plusDays(58), "冷藏区 / A-02")
        )),
        WarehouseProductUi("cracker", "苏打饼干", "零食", "箱", 12, listOf(
            WarehouseBatchUi("cracker-normal", "B20261008B", 36, today.plusDays(90), "常温区 / B-03")
        )),
        WarehouseProductUi("water", "矿泉水 550ml", "饮料", "箱", 60, listOf(
            WarehouseBatchUi("water-normal", "W20261001", 48, today.plusDays(180), "常温区 / B-01")
        )),
        WarehouseProductUi("noodle", "方便面（桶装）", "方便食品", "箱", 6, listOf(
            WarehouseBatchUi("noodle-normal", "N20261001", 8, today.plusDays(120), "常温区 / B-05")
        )),
        WarehouseProductUi("cola", "可乐 500ml", "饮料", "瓶", 12, listOf(
            WarehouseBatchUi("cola-normal", "C20261001", 60, today.plusDays(180), "常温区 / B-02")
        )),
        WarehouseProductUi("bread", "全麦面包", "烘焙", "袋", 6, emptyList())
    )
}
