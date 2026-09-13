package com.familykhata.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProductEntity::class, StockBatchEntity::class],
    version = 2,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun dao(): InventoryDao

    companion object {
        @Volatile private var INSTANCE: InventoryDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_products ADD COLUMN unit TEXT NOT NULL DEFAULT 'pcs'")
                db.execSQL("ALTER TABLE inventory_products ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inventory_products ADD COLUMN genericName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inventory_products ADD COLUMN modelName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inventory_products ADD COLUMN serialOrImei TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inventory_products ADD COLUMN size TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inventory_products ADD COLUMN color TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inventory_products ADD COLUMN warrantyMonths INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inventory_batches ADD COLUMN batchNo TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): InventoryDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                InventoryDatabase::class.java,
                "hisabi-inventory.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
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
