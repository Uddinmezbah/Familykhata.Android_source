package com.familykhata.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.familykhata.app.production.ProductionBatchEntity
import com.familykhata.app.production.ProductionConsumptionEntity
import com.familykhata.app.production.ProductionCostEntity
import com.familykhata.app.production.ProductionDao
import com.familykhata.app.production.ProductionItemRoleEntity

@Database(
    entities = [
        ProductEntity::class,
        StockBatchEntity::class,
        ProductionItemRoleEntity::class,
        ProductionBatchEntity::class,
        ProductionConsumptionEntity::class,
        ProductionCostEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun dao(): InventoryDao
    abstract fun productionDao(): ProductionDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `production_item_roles` (
                        `productId` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `workspace` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`productId`),
                        FOREIGN KEY(`productId`)
                            REFERENCES `inventory_products`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_item_roles_workspace_role`
                    ON `production_item_roles` (`workspace`, `role`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `production_batches` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `batchNo` TEXT NOT NULL,
                        `finishedProductId` INTEGER,
                        `finishedProductNameSnapshot` TEXT NOT NULL,
                        `outputQuantity` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `outputStockBatchId` INTEGER,
                        `note` TEXT NOT NULL,
                        `workspace` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`finishedProductId`)
                            REFERENCES `inventory_products`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL,
                        FOREIGN KEY(`outputStockBatchId`)
                            REFERENCES `inventory_batches`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_batches_finishedProductId`
                    ON `production_batches` (`finishedProductId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_batches_outputStockBatchId`
                    ON `production_batches` (`outputStockBatchId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_batches_workspace_status`
                    ON `production_batches` (`workspace`, `status`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_batches_startedAt`
                    ON `production_batches` (`startedAt`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `production_consumptions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productionBatchId` INTEGER NOT NULL,
                        `materialProductId` INTEGER,
                        `materialNameSnapshot` TEXT NOT NULL,
                        `unitSnapshot` TEXT NOT NULL,
                        `sourceStockBatchId` INTEGER,
                        `sourceBatchNoSnapshot` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitCost` REAL NOT NULL,
                        `totalCost` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`productionBatchId`)
                            REFERENCES `production_batches`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
                        FOREIGN KEY(`materialProductId`)
                            REFERENCES `inventory_products`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL,
                        FOREIGN KEY(`sourceStockBatchId`)
                            REFERENCES `inventory_batches`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_consumptions_productionBatchId`
                    ON `production_consumptions` (`productionBatchId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_consumptions_materialProductId`
                    ON `production_consumptions` (`materialProductId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_consumptions_sourceStockBatchId`
                    ON `production_consumptions` (`sourceStockBatchId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `production_costs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productionBatchId` INTEGER NOT NULL,
                        `costType` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`productionBatchId`)
                            REFERENCES `production_batches`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_costs_productionBatchId`
                    ON `production_costs` (`productionBatchId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_production_costs_costType`
                    ON `production_costs` (`costType`)
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): InventoryDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                InventoryDatabase::class.java,
                "hisabi-inventory.db"
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3
                )
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
