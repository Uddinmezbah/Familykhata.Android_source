package com.familykhata.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.familykhata.app.dealership.DealershipDao
import com.familykhata.app.dealership.DealershipDealerEntity
import com.familykhata.app.dealership.DealershipInvoiceEntity
import com.familykhata.app.dealership.DealershipInvoiceLineEntity
import com.familykhata.app.dealership.DealershipPaymentEntity
import com.familykhata.app.dealership.DealershipProductPolicyEntity
import com.familykhata.app.dealership.DealershipStockAllocationEntity
import com.familykhata.app.dealership.DealershipStockReceiptEntity
import com.familykhata.app.dealership.DealershipSupplierEntity
import com.familykhata.app.dealership.DealershipTerritoryEntity
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
        ProductionCostEntity::class,
        DealershipSupplierEntity::class,
        DealershipTerritoryEntity::class,
        DealershipDealerEntity::class,
        DealershipProductPolicyEntity::class,
        DealershipStockReceiptEntity::class,
        DealershipInvoiceEntity::class,
        DealershipInvoiceLineEntity::class,
        DealershipStockAllocationEntity::class,
        DealershipPaymentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun dao(): InventoryDao
    abstract fun productionDao(): ProductionDao
    abstract fun dealershipDao(): DealershipDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_suppliers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `contactPerson` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `workspace` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_suppliers_workspace_name`
                    ON `dealership_suppliers` (`workspace`, `name`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_territories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `workspace` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_territories_workspace_name`
                    ON `dealership_territories` (`workspace`, `name`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_dealers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `territoryId` INTEGER,
                        `name` TEXT NOT NULL,
                        `dealerCode` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `creditLimit` REAL NOT NULL,
                        `note` TEXT NOT NULL,
                        `workspace` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`territoryId`)
                            REFERENCES `dealership_territories`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_dealers_territoryId`
                    ON `dealership_dealers` (`territoryId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_dealers_workspace_name`
                    ON `dealership_dealers` (`workspace`, `name`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_product_policies` (
                        `productId` INTEGER NOT NULL,
                        `dealerPrice` REAL NOT NULL,
                        `marginPercent` REAL NOT NULL,
                        `note` TEXT NOT NULL,
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
                    `index_dealership_product_policies_workspace_productId`
                    ON `dealership_product_policies`
                    (`workspace`, `productId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_stock_receipts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `supplierId` INTEGER,
                        `productId` INTEGER,
                        `inventoryBatchId` INTEGER,
                        `supplierNameSnapshot` TEXT NOT NULL,
                        `productNameSnapshot` TEXT NOT NULL,
                        `invoiceReference` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitCost` REAL NOT NULL,
                        `receivedAt` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `workspace` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`supplierId`)
                            REFERENCES `dealership_suppliers`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL,
                        FOREIGN KEY(`productId`)
                            REFERENCES `inventory_products`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL,
                        FOREIGN KEY(`inventoryBatchId`)
                            REFERENCES `inventory_batches`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_stock_receipts_supplierId`
                    ON `dealership_stock_receipts` (`supplierId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_stock_receipts_productId`
                    ON `dealership_stock_receipts` (`productId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_stock_receipts_inventoryBatchId`
                    ON `dealership_stock_receipts` (`inventoryBatchId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_stock_receipts_receivedAt`
                    ON `dealership_stock_receipts` (`receivedAt`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_invoices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dealerId` INTEGER,
                        `dealerNameSnapshot` TEXT NOT NULL,
                        `invoiceNo` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `soldAt` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `workspace` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`dealerId`)
                            REFERENCES `dealership_dealers`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_invoices_dealerId`
                    ON `dealership_invoices` (`dealerId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_invoices_workspace_status`
                    ON `dealership_invoices` (`workspace`, `status`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_invoices_soldAt`
                    ON `dealership_invoices` (`soldAt`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_invoice_lines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoiceId` INTEGER NOT NULL,
                        `productId` INTEGER,
                        `productNameSnapshot` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitPrice` REAL NOT NULL,
                        `lineTotal` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`invoiceId`)
                            REFERENCES `dealership_invoices`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
                        FOREIGN KEY(`productId`)
                            REFERENCES `inventory_products`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_invoice_lines_invoiceId`
                    ON `dealership_invoice_lines` (`invoiceId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_invoice_lines_productId`
                    ON `dealership_invoice_lines` (`productId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_stock_allocations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoiceLineId` INTEGER NOT NULL,
                        `sourceStockBatchId` INTEGER,
                        `sourceBatchNoSnapshot` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitCost` REAL NOT NULL,
                        `totalCost` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`invoiceLineId`)
                            REFERENCES `dealership_invoice_lines`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
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
                    `index_dealership_stock_allocations_invoiceLineId`
                    ON `dealership_stock_allocations` (`invoiceLineId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_stock_allocations_sourceStockBatchId`
                    ON `dealership_stock_allocations` (`sourceStockBatchId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dealership_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoiceId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `paidAt` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`invoiceId`)
                            REFERENCES `dealership_invoices`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_payments_invoiceId`
                    ON `dealership_payments` (`invoiceId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_dealership_payments_paidAt`
                    ON `dealership_payments` (`paidAt`)
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
                    MIGRATION_2_3,
                    MIGRATION_3_4
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
