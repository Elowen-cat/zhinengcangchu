package com.youshu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.youshu.app.data.local.entity.WarehouseBatchEntity
import com.youshu.app.data.local.entity.WarehouseDocumentEntity
import com.youshu.app.data.local.entity.WarehouseMovementEntity
import com.youshu.app.data.local.entity.WarehouseProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouse_products ORDER BY createdAt DESC")
    fun observeProducts(): Flow<List<WarehouseProductEntity>>

    @Query("SELECT * FROM warehouse_batches")
    fun observeBatches(): Flow<List<WarehouseBatchEntity>>

    @Query("SELECT * FROM warehouse_documents WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun observePendingDocuments(): Flow<List<WarehouseDocumentEntity>>

    @Query("SELECT * FROM warehouse_documents WHERE status = 'CONFIRMED'")
    fun observeConfirmedDocuments(): Flow<List<WarehouseDocumentEntity>>

    @Query("SELECT * FROM warehouse_movements ORDER BY createdAt DESC")
    fun observeMovements(): Flow<List<WarehouseMovementEntity>>

    @Query("SELECT * FROM warehouse_products")
    suspend fun allProducts(): List<WarehouseProductEntity>

    @Query("SELECT * FROM warehouse_batches")
    suspend fun allBatches(): List<WarehouseBatchEntity>

    @Query("SELECT * FROM warehouse_documents")
    suspend fun allDocuments(): List<WarehouseDocumentEntity>

    @Query("SELECT * FROM warehouse_movements")
    suspend fun allMovements(): List<WarehouseMovementEntity>

    @Query("SELECT * FROM warehouse_documents WHERE status = 'PENDING' AND direction = :direction ORDER BY createdAt ASC")
    suspend fun pendingDocuments(direction: String): List<WarehouseDocumentEntity>

    @Query("SELECT * FROM warehouse_documents WHERE id = :id LIMIT 1")
    suspend fun document(id: String): WarehouseDocumentEntity?

    @Query("SELECT * FROM warehouse_products WHERE id = :id LIMIT 1")
    suspend fun product(id: String): WarehouseProductEntity?

    @Query("SELECT * FROM warehouse_products WHERE name = :name LIMIT 1")
    suspend fun productByName(name: String): WarehouseProductEntity?

    @Query("SELECT * FROM warehouse_batches WHERE id = :id LIMIT 1")
    suspend fun batch(id: String): WarehouseBatchEntity?

    @Query("SELECT * FROM warehouse_batches WHERE productId = :productId AND code = :code LIMIT 1")
    suspend fun batchByCode(productId: String, code: String): WarehouseBatchEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: WarehouseProductEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBatch(batch: WarehouseBatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: WarehouseDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: WarehouseMovementEntity)

    @Query("DELETE FROM warehouse_movements")
    suspend fun deleteAllMovements()

    @Query("DELETE FROM warehouse_documents")
    suspend fun deleteAllDocuments()

    @Query("DELETE FROM warehouse_batches")
    suspend fun deleteAllBatches()

    @Query("DELETE FROM warehouse_products")
    suspend fun deleteAllProducts()

    @Query("UPDATE warehouse_batches SET quantity = :quantity WHERE id = :id")
    suspend fun setBatchQuantity(id: String, quantity: Int)

    @Query("UPDATE warehouse_batches SET location = :location WHERE id = :id")
    suspend fun setBatchLocation(id: String, location: String)

    @Query("UPDATE warehouse_products SET safetyStock = :safetyStock, targetCoverageDays = :targetCoverageDays WHERE id = :id")
    suspend fun updateReplenishmentPolicy(id: String, safetyStock: Int, targetCoverageDays: Int): Int

    @Query("UPDATE warehouse_batches SET quantity = quantity - :amount WHERE id = :id AND quantity >= :amount")
    suspend fun subtractBatchQuantity(id: String, amount: Int): Int

    @Query("DELETE FROM warehouse_documents WHERE id = :id AND status = 'PENDING'")
    suspend fun deletePendingDocument(id: String)
}
