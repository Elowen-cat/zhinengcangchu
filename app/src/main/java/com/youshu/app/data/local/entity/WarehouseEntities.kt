package com.youshu.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "warehouse_products", indices = [Index(value = ["name"], unique = true)])
data class WarehouseProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val unit: String,
    val safetyStock: Int,
    val createdAt: Long,
    val targetCoverageDays: Int = 7
)

@Serializable
@Entity(
    tableName = "warehouse_batches",
    foreignKeys = [ForeignKey(
        entity = WarehouseProductEntity::class,
        parentColumns = ["id"], childColumns = ["productId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("productId"), Index(value = ["productId", "code"], unique = true)]
)
data class WarehouseBatchEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val code: String,
    val quantity: Int,
    val expiry: String?,
    val location: String,
    val priority: Boolean = false
)

// Each pending document contains one editable line. A confirmation can post several documents atomically.
@Serializable
@Entity(tableName = "warehouse_documents", indices = [Index("status"), Index("direction")])
data class WarehouseDocumentEntity(
    @PrimaryKey val id: String,
    val status: String,
    val direction: String,
    val source: String,
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
    val imageUris: String,
    val transcript: String,
    val reviewed: Boolean,
    val createdAt: Long,
    val confirmedAt: Long? = null
)

@Serializable
@Entity(
    tableName = "warehouse_movements",
    foreignKeys = [
        ForeignKey(entity = WarehouseProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = WarehouseBatchEntity::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = WarehouseDocumentEntity::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("productId"), Index("batchId"), Index(value = ["documentId"], unique = true), Index("createdAt")]
)
data class WarehouseMovementEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val productId: String,
    val productName: String,
    val batchId: String,
    val batchCode: String,
    val direction: String,
    val quantity: Int,
    val unit: String,
    val note: String,
    val createdAt: Long
)
