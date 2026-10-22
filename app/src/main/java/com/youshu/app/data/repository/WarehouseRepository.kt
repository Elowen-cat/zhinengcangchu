package com.youshu.app.data.repository

import androidx.room.withTransaction
import com.youshu.app.data.local.dao.WarehouseDao
import com.youshu.app.data.local.database.AppDatabase
import com.youshu.app.data.local.entity.WarehouseBatchEntity
import com.youshu.app.data.local.entity.WarehouseDocumentEntity
import com.youshu.app.data.local.entity.WarehouseMovementEntity
import com.youshu.app.data.local.entity.WarehouseProductEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class WarehouseValidationException(message: String) : IllegalArgumentException(message)

data class WarehouseSnapshot(
    val products: List<WarehouseProductEntity>,
    val batches: List<WarehouseBatchEntity>,
    val documents: List<WarehouseDocumentEntity>,
    val movements: List<WarehouseMovementEntity>
)

@Singleton
class WarehouseRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: WarehouseDao
) {
    fun observeProducts(): Flow<List<WarehouseProductEntity>> = dao.observeProducts()
    fun observeBatches(): Flow<List<WarehouseBatchEntity>> = dao.observeBatches()
    fun observePendingDocuments(): Flow<List<WarehouseDocumentEntity>> = dao.observePendingDocuments()
    fun observeConfirmedDocuments(): Flow<List<WarehouseDocumentEntity>> = dao.observeConfirmedDocuments()
    fun observeMovements(): Flow<List<WarehouseMovementEntity>> = dao.observeMovements()

    suspend fun updateReplenishmentPolicy(id: String, safetyStock: Int, targetCoverageDays: Int) {
        if (safetyStock < 0) throw WarehouseValidationException("安全库存不能小于 0")
        if (targetCoverageDays !in 1..90) throw WarehouseValidationException("目标覆盖天数应为 1–90 天")
        if (dao.updateReplenishmentPolicy(id, safetyStock, targetCoverageDays) != 1) {
            throw WarehouseValidationException("商品不存在")
        }
    }

    suspend fun exportSnapshot(): WarehouseSnapshot = database.withTransaction {
        WarehouseSnapshot(dao.allProducts(), dao.allBatches(), dao.allDocuments(), dao.allMovements())
    }

    suspend fun replaceFromBackup(snapshot: WarehouseSnapshot) {
        database.withTransaction {
            dao.deleteAllMovements()
            dao.deleteAllDocuments()
            dao.deleteAllBatches()
            dao.deleteAllProducts()
            snapshot.products.forEach { dao.insertProduct(it) }
            snapshot.batches.forEach { dao.insertBatch(it) }
            snapshot.documents.forEach { dao.upsertDocument(it) }
            snapshot.movements.forEach { dao.insertMovement(it) }
        }
    }

    suspend fun savePending(document: WarehouseDocumentEntity) = savePendingBatch(listOf(document))

    /** A photo session is saved all-or-nothing; this does not post any stock movement. */
    suspend fun savePendingBatch(documents: List<WarehouseDocumentEntity>) {
        require(documents.isNotEmpty()) { "没有待保存草稿" }
        require(documents.all { it.status == "PENDING" })
        require(documents.map { it.id }.distinct().size == documents.size) { "草稿编号重复" }
        database.withTransaction {
            documents.forEach { document ->
                val old = dao.document(document.id)
                if (old != null && old.status != "PENDING") throw WarehouseValidationException("已确认单据不可修改")
                dao.upsertDocument(document.copy(createdAt = old?.createdAt ?: document.createdAt))
            }
        }
    }

    suspend fun deletePending(id: String) = dao.deletePendingDocument(id)

    /** Counts a batch against the observed physical quantity and records the signed difference. */
    suspend fun countBatch(batchId: String, observed: Int, note: String) {
        if (observed < 0) throw WarehouseValidationException("实盘数量不能小于 0")
        if (note.isBlank()) throw WarehouseValidationException("请填写盘点说明")
        database.withTransaction {
            val batch = dao.batch(batchId) ?: throw WarehouseValidationException("批次不存在")
            val product = dao.product(batch.productId) ?: throw WarehouseValidationException("商品不存在")
            val before = batch.quantity
            val difference = observed.toLong() - before.toLong()
            if (difference < Int.MIN_VALUE || difference > Int.MAX_VALUE) throw WarehouseValidationException("盘点差异超出范围")
            val now = System.currentTimeMillis()
            val documentId = UUID.randomUUID().toString()
            dao.setBatchQuantity(batch.id, observed)
            dao.upsertDocument(WarehouseDocumentEntity(
                id = documentId, status = "CONFIRMED", direction = "COUNT", source = "MANUAL",
                productId = product.id, productName = product.name, quantity = observed, unit = product.unit,
                batchId = batch.id, batchCode = batch.code, expiry = batch.expiry.orEmpty(), location = batch.location,
                reason = note.trim(), destination = batch.location, supplier = "", purchasePrice = "",
                imageUris = "", transcript = "", reviewed = true, createdAt = now, confirmedAt = now
            ))
            dao.insertMovement(WarehouseMovementEntity(
                UUID.randomUUID().toString(), documentId, product.id, product.name, batch.id, batch.code,
                "COUNT", difference.toInt(), product.unit, "盘点 $before → $observed · ${note.trim()}", now
            ))
        }
    }

    /** Moves an entire batch to a new location without changing stock quantity. */
    suspend fun transferBatch(batchId: String, destination: String) {
        val target = destination.trim()
        if (target.isBlank()) throw WarehouseValidationException("请填写目标库位")
        database.withTransaction {
            val batch = dao.batch(batchId) ?: throw WarehouseValidationException("批次不存在")
            val product = dao.product(batch.productId) ?: throw WarehouseValidationException("商品不存在")
            if (batch.quantity <= 0) throw WarehouseValidationException("该批次暂无库存")
            if (target == batch.location) throw WarehouseValidationException("目标库位与当前库位相同")
            val now = System.currentTimeMillis()
            val documentId = UUID.randomUUID().toString()
            dao.setBatchLocation(batch.id, target)
            dao.upsertDocument(WarehouseDocumentEntity(
                id = documentId, status = "CONFIRMED", direction = "TRANSFER", source = "MANUAL",
                productId = product.id, productName = product.name, quantity = batch.quantity, unit = product.unit,
                batchId = batch.id, batchCode = batch.code, expiry = batch.expiry.orEmpty(), location = batch.location,
                reason = "整批移库", destination = target, supplier = "", purchasePrice = "",
                imageUris = "", transcript = "", reviewed = true, createdAt = now, confirmedAt = now
            ))
            dao.insertMovement(WarehouseMovementEntity(
                UUID.randomUUID().toString(), documentId, product.id, product.name, batch.id, batch.code,
                "TRANSFER", batch.quantity, product.unit, "移库 ${batch.location} → $target", now
            ))
        }
    }

    /** Posts all reviewed pending documents of one direction in a single Room transaction. */
    suspend fun confirm(direction: String) {
        database.withTransaction {
            val lines = dao.pendingDocuments(direction)
            if (lines.isEmpty()) throw WarehouseValidationException("没有待确认单据")
            if (lines.any { !it.reviewed }) throw WarehouseValidationException("请先核对全部单据")
            if (lines.any { it.quantity <= 0 }) throw WarehouseValidationException("数量必须大于 0")
            when (direction) {
                "INBOUND" -> postInbound(lines)
                "OUTBOUND" -> postOutbound(lines)
                else -> throw WarehouseValidationException("未知作业方向")
            }
        }
    }

    private suspend fun postInbound(lines: List<WarehouseDocumentEntity>) {
        val today = LocalDate.now()
        for ((name, group) in lines.groupBy { it.productName.trim() }) {
            if (name.isBlank() || group.map { it.unit.trim() }.distinct().size != 1 || group.first().unit.isBlank()) {
                throw WarehouseValidationException("请核对商品名称和基础单位")
            }
            val existing = dao.productByName(name)
            if (existing != null && existing.unit != group.first().unit.trim()) {
                throw WarehouseValidationException("同名商品的基础单位不一致")
            }
        }
        for ((key, group) in lines.groupBy { it.productName.trim() to it.batchCode.trim() }) {
            if (key.second.isBlank() || group.any { it.location.isBlank() }) throw WarehouseValidationException("请填写批次和库位")
            if (group.map { it.expiry.trim() to it.location.trim() }.distinct().size != 1) {
                throw WarehouseValidationException("同批次的效期或库位不一致")
            }
            val expiry = group.first().expiry.trim().takeIf { it.isNotEmpty() }
            val parsed = expiry?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (expiry != null && (parsed == null || parsed.isBefore(today))) throw WarehouseValidationException("请核对效期")
            val product = dao.productByName(key.first)
            val batch = product?.let { dao.batchByCode(it.id, key.second) }
            if (batch != null && (batch.expiry != expiry || batch.location != group.first().location.trim())) {
                throw WarehouseValidationException("同批次的效期或库位不一致")
            }
            if ((batch?.quantity ?: 0).toLong() + group.sumOf { it.quantity.toLong() } > Int.MAX_VALUE) {
                throw WarehouseValidationException("批次数量超出范围")
            }
            if (group.any { it.purchasePrice.isNotBlank() && (it.purchasePrice.toDoubleOrNull()?.let { value -> !value.isFinite() || value < 0 } != false) }) {
                throw WarehouseValidationException("进价应为非负数")
            }
        }
        lines.forEach { line ->
            val now = System.currentTimeMillis()
            val name = line.productName.trim()
            val product = dao.productByName(name) ?: WarehouseProductEntity(
                id = UUID.randomUUID().toString(), name = name, category = "未分类",
                unit = line.unit.trim(), safetyStock = 0, createdAt = now
            ).also { dao.insertProduct(it) }
            val code = line.batchCode.trim()
            val existing = dao.batchByCode(product.id, code)
            val batch = if (existing == null) {
                WarehouseBatchEntity(UUID.randomUUID().toString(), product.id, code, line.quantity,
                    line.expiry.trim().ifEmpty { null }, line.location.trim()).also { dao.insertBatch(it) }
            } else {
                dao.setBatchQuantity(existing.id, existing.quantity + line.quantity)
                existing
            }
            dao.upsertDocument(line.copy(status = "CONFIRMED", productId = product.id, batchId = batch.id, confirmedAt = now))
            dao.insertMovement(WarehouseMovementEntity(UUID.randomUUID().toString(), line.id, product.id, product.name,
                batch.id, code, "INBOUND", line.quantity, product.unit, "收货 · ${line.location.trim()}", now))
        }
    }

    private suspend fun postOutbound(lines: List<WarehouseDocumentEntity>) {
        val today = LocalDate.now()
        for ((batchId, group) in lines.groupBy { it.batchId }) {
            val batch = batchId?.let { dao.batch(it) } ?: throw WarehouseValidationException("请选择可用批次")
            val product = dao.product(batch.productId) ?: throw WarehouseValidationException("商品不存在")
            if (group.any { it.productId != product.id || it.destination.isBlank() || it.reason.isBlank() }) {
                throw WarehouseValidationException("请核对商品、原因和去向")
            }
            val expiry = batch.expiry?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (batch.expiry != null && expiry == null) throw WarehouseValidationException("批次效期无效，请核对")
            if (batch.quantity <= 0 || (expiry != null && expiry.isBefore(today)) || group.sumOf { it.quantity.toLong() } > batch.quantity) {
                throw WarehouseValidationException("批次库存不足或已过期")
            }
        }
        lines.forEach { line ->
            val batch = dao.batch(requireNotNull(line.batchId)) ?: throw WarehouseValidationException("批次不存在")
            val product = dao.product(batch.productId) ?: throw WarehouseValidationException("商品不存在")
            if (dao.subtractBatchQuantity(batch.id, line.quantity) != 1) throw WarehouseValidationException("批次库存不足")
            val now = System.currentTimeMillis()
            dao.upsertDocument(line.copy(status = "CONFIRMED", confirmedAt = now))
            dao.insertMovement(WarehouseMovementEntity(UUID.randomUUID().toString(), line.id, product.id, product.name,
                batch.id, batch.code, "OUTBOUND", line.quantity, product.unit,
                "${line.reason.trim()} · ${line.destination.trim()}", now))
        }
    }
}
