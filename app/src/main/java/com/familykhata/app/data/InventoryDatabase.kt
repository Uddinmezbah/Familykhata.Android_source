package com.familykhata.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [ProductEntity::class, StockBatchEntity::class],
    version = 1,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun dao(): InventoryDao

    companion object {
        @Volatile private var INSTANCE: InventoryDatabase? = null

        fun get(context: Context): InventoryDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                InventoryDatabase::class.java,
                "hisabi-inventory.db"
            ).build().also { INSTANCE = it }
        }
    }
}

data class InventoryBackupData(
    val products: List<ProductEntity>,
    val batches: List<StockBatchEntity>
)

object InventoryBackupBridge {
    suspend fun export(context: Context): InventoryBackupData {
        val dao = InventoryDatabase.get(context).dao()
        return InventoryBackupData(
            products = dao.getAllProducts(),
            batches = dao.getAllBatches()
        )
    }

    suspend fun restore(
        context: Context,
        products: List<ProductEntity>,
        batches: List<StockBatchEntity>
    ) {
        val db = InventoryDatabase.get(context)
        val dao = db.dao()
        db.withTransaction {
            dao.clearBatches()
            dao.clearProducts()
            products.forEach { dao.insertProduct(it) }
            batches.forEach { dao.insertBatch(it) }
        }
    }
}
