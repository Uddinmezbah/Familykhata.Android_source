package com.familykhata.app.data

import org.json.JSONArray
import org.json.JSONObject

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.familykhata.app.agro.AgroCostEntity
import com.familykhata.app.agro.AgroCycleEntity
import com.familykhata.app.agro.AgroDao
import com.familykhata.app.agro.AgroHarvestEntity
import com.familykhata.app.agro.AgroLossEntity
import com.familykhata.app.foodservice.FoodMenuItemEntity
import com.familykhata.app.foodservice.FoodOrderEntity
import com.familykhata.app.foodservice.FoodOrderLineEntity
import com.familykhata.app.foodservice.FoodPaymentEntity
import com.familykhata.app.foodservice.FoodRecipeIngredientEntity
import com.familykhata.app.foodservice.FoodServiceDao
import com.familykhata.app.foodservice.FoodStockAllocationEntity
import com.familykhata.app.dealership.DealershipDao
import com.familykhata.app.dealership.DealershipDealerEntity
import com.familykhata.app.dealership.DealershipInvoiceEntity
import com.familykhata.app.dealership.DealershipInvoiceLineEntity
import com.familykhata.app.dealership.DealershipPaymentEntity
import com.familykhata.app.dealership.DealershipProductPolicyEntity
import com.familykhata.app.dealership.DealershipReturnEntity
import com.familykhata.app.dealership.DealershipStockAllocationEntity
import com.familykhata.app.dealership.DealershipStockReceiptEntity
import com.familykhata.app.dealership.DealershipSupplierEntity
import com.familykhata.app.dealership.DealershipTerritoryEntity
import com.familykhata.app.dealerbusiness.DealerAreaEntity
import com.familykhata.app.dealerbusiness.DealerBusinessDao
import com.familykhata.app.dealerbusiness.DealerCollectionEntity
import com.familykhata.app.dealerbusiness.DealerCollectionAllocationEntity
import com.familykhata.app.dealerbusiness.DealerCompanyEntity
import com.familykhata.app.dealerbusiness.DealerCustomerEntity
import com.familykhata.app.dealerbusiness.DealerExpenseEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseLineEntity
import com.familykhata.app.dealerbusiness.DealerPurchaseReturnEntity
import com.familykhata.app.dealerbusiness.DealerSaleEntity
import com.familykhata.app.dealerbusiness.DealerSaleLineEntity
import com.familykhata.app.dealerbusiness.DealerSalesReturnEntity
import com.familykhata.app.dealerbusiness.DealerSalesReturnAllocationEntity
import com.familykhata.app.dealerbusiness.DealerStockAllocationEntity
import com.familykhata.app.dealerbusiness.DealerSupplierPaymentEntity
import com.familykhata.app.dealerbusiness.DealerSupplierPaymentAllocationEntity
import com.familykhata.app.dealerbusiness.DealerDamageEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryChallanAllocationEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryChallanEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryChallanLineEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryChallanSaleEntity
import com.familykhata.app.dealerbusiness.DealerDeliveryPersonEntity
import com.familykhata.app.dealerbusiness.DealerDeliverySaleAllocationEntity
import com.familykhata.app.dealerbusiness.DealerDeliverySettlementEntity
import com.familykhata.app.dealerbusiness.DealerDeliverySettlementLineEntity
import com.familykhata.app.dealerbusiness.DealerProductPackEntity
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
        DealershipPaymentEntity::class,
        DealershipReturnEntity::class,
        DealerCompanyEntity::class,
        DealerAreaEntity::class,
        DealerCustomerEntity::class,
        DealerPurchaseEntity::class,
        DealerPurchaseLineEntity::class,
        DealerSupplierPaymentEntity::class,
        DealerSupplierPaymentAllocationEntity::class,
        DealerSaleEntity::class,
        DealerSaleLineEntity::class,
        DealerStockAllocationEntity::class,
        DealerCollectionEntity::class,
        DealerCollectionAllocationEntity::class,
        DealerSalesReturnEntity::class,
        DealerSalesReturnAllocationEntity::class,
        DealerPurchaseReturnEntity::class,
        DealerExpenseEntity::class,
        DealerProductPackEntity::class,
        DealerDeliveryPersonEntity::class,
        DealerDeliveryChallanEntity::class,
        DealerDeliveryChallanLineEntity::class,
        DealerDeliveryChallanAllocationEntity::class,
        DealerDeliveryChallanSaleEntity::class,
        DealerDeliverySaleAllocationEntity::class,
        DealerDeliverySettlementEntity::class,
        DealerDeliverySettlementLineEntity::class,
        DealerDamageEntity::class,
        AgroCycleEntity::class,
        AgroCostEntity::class,
        AgroLossEntity::class,
        AgroHarvestEntity::class,
        FoodMenuItemEntity::class,
        FoodRecipeIngredientEntity::class,
        FoodOrderEntity::class,
        FoodOrderLineEntity::class,
        FoodStockAllocationEntity::class,
        FoodPaymentEntity::class,
        RetailSaleEntity::class,
        RetailSaleLineEntity::class,
        RetailSaleStockAllocationEntity::class,
        RetailSalePaymentEntity::class,
        PurchaseSupplierEntity::class,
        PurchaseBillEntity::class,
        PurchaseBillLineEntity::class,
        PurchasePaymentEntity::class,
        PurchaseReturnEntity::class,
        ProductUnitConversionEntity::class
    ],
    version = 18,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun dao(): InventoryDao
    abstract fun productionDao(): ProductionDao
    abstract fun dealershipDao(): DealershipDao
    abstract fun dealerBusinessDao(): DealerBusinessDao
    abstract fun agroDao(): AgroDao
    abstract fun foodServiceDao(): FoodServiceDao

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

        private val MIGRATION_4_5 =
            object : Migration(4, 5) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `agro_cycles` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `cycleType` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `breedOrVariety` TEXT NOT NULL,
                            `location` TEXT NOT NULL,
                            `startingAmount` REAL NOT NULL,
                            `startingUnit` TEXT NOT NULL,
                            `startDate` INTEGER NOT NULL,
                            `expectedEndDate` INTEGER,
                            `status` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_cycles_workspace_status`
                        ON `agro_cycles`
                        (`workspace`, `status`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_cycles_startDate`
                        ON `agro_cycles`
                        (`startDate`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `agro_costs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `cycleId` INTEGER NOT NULL,
                            `costType` TEXT NOT NULL,
                            `quantity` REAL NOT NULL,
                            `unit` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `occurredAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`cycleId`)
                                REFERENCES `agro_cycles`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_costs_cycleId`
                        ON `agro_costs`
                        (`cycleId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_costs_costType`
                        ON `agro_costs`
                        (`costType`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_costs_occurredAt`
                        ON `agro_costs`
                        (`occurredAt`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `agro_losses` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `cycleId` INTEGER NOT NULL,
                            `lossType` TEXT NOT NULL,
                            `quantity` REAL NOT NULL,
                            `unit` TEXT NOT NULL,
                            `reason` TEXT NOT NULL,
                            `occurredAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`cycleId`)
                                REFERENCES `agro_cycles`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_losses_cycleId`
                        ON `agro_losses`
                        (`cycleId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_losses_lossType`
                        ON `agro_losses`
                        (`lossType`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_losses_occurredAt`
                        ON `agro_losses`
                        (`occurredAt`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `agro_harvests` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `cycleId` INTEGER NOT NULL,
                            `productId` INTEGER,
                            `inventoryBatchId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitSnapshot` TEXT NOT NULL,
                            `allocatedCost` REAL NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `batchNo` TEXT NOT NULL,
                            `harvestedAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`cycleId`)
                                REFERENCES `agro_cycles`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
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
                        `index_agro_harvests_cycleId`
                        ON `agro_harvests`
                        (`cycleId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_harvests_productId`
                        ON `agro_harvests`
                        (`productId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_harvests_inventoryBatchId`
                        ON `agro_harvests`
                        (`inventoryBatchId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agro_harvests_harvestedAt`
                        ON `agro_harvests`
                        (`harvestedAt`)
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_5_6 =
            object : Migration(5, 6) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `food_menu_items` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `sellingPrice` REAL NOT NULL,
                            `active` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_menu_items_workspace_name`
                        ON `food_menu_items`
                        (`workspace`, `name`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `food_recipe_ingredients` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `menuItemId` INTEGER NOT NULL,
                            `ingredientProductId` INTEGER,
                            `ingredientNameSnapshot` TEXT NOT NULL,
                            `quantityPerItem` INTEGER NOT NULL,
                            `unitSnapshot` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`menuItemId`)
                                REFERENCES `food_menu_items`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`ingredientProductId`)
                                REFERENCES `inventory_products`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_recipe_ingredients_menuItemId`
                        ON `food_recipe_ingredients`
                        (`menuItemId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_recipe_ingredients_ingredientProductId`
                        ON `food_recipe_ingredients`
                        (`ingredientProductId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `food_orders` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `orderNo` TEXT NOT NULL,
                            `orderType` TEXT NOT NULL,
                            `customerName` TEXT NOT NULL,
                            `phone` TEXT NOT NULL,
                            `tableOrReference` TEXT NOT NULL,
                            `eventDate` INTEGER,
                            `guestCount` INTEGER NOT NULL,
                            `status` TEXT NOT NULL,
                            `orderedAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_orders_workspace_status`
                        ON `food_orders`
                        (`workspace`, `status`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_orders_orderedAt`
                        ON `food_orders`
                        (`orderedAt`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_orders_eventDate`
                        ON `food_orders`
                        (`eventDate`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `food_order_lines` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `orderId` INTEGER NOT NULL,
                            `menuItemId` INTEGER,
                            `menuItemNameSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitPrice` REAL NOT NULL,
                            `lineTotal` REAL NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`orderId`)
                                REFERENCES `food_orders`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`menuItemId`)
                                REFERENCES `food_menu_items`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_order_lines_orderId`
                        ON `food_order_lines`
                        (`orderId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_order_lines_menuItemId`
                        ON `food_order_lines`
                        (`menuItemId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `food_stock_allocations` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `orderLineId` INTEGER NOT NULL,
                            `ingredientProductId` INTEGER,
                            `ingredientNameSnapshot` TEXT NOT NULL,
                            `sourceStockBatchId` INTEGER,
                            `sourceBatchNoSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `totalCost` REAL NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`orderLineId`)
                                REFERENCES `food_order_lines`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`ingredientProductId`)
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
                        `index_food_stock_allocations_orderLineId`
                        ON `food_stock_allocations`
                        (`orderLineId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_stock_allocations_ingredientProductId`
                        ON `food_stock_allocations`
                        (`ingredientProductId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_stock_allocations_sourceStockBatchId`
                        ON `food_stock_allocations`
                        (`sourceStockBatchId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `food_payments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `orderId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `paidAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`orderId`)
                                REFERENCES `food_orders`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_payments_orderId`
                        ON `food_payments`
                        (`orderId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_payments_paidAt`
                        ON `food_payments`
                        (`paidAt`)
                        """.trimIndent()
                    )
                }
            }


        private val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        ALTER TABLE inventory_products
                        ADD COLUMN businessKey
                        TEXT NOT NULL
                        DEFAULT 'legacy'
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_inventory_products_workspace_businessKey_name`
                        ON `inventory_products`
                        (`workspace`, `businessKey`, `name`)
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealership_returns` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `invoiceId` INTEGER NOT NULL,
                            `invoiceLineId` INTEGER NOT NULL,
                            `productId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitPrice` REAL NOT NULL,
                            `totalRefund` REAL NOT NULL,
                            `totalCost` REAL NOT NULL,
                            `returnType` TEXT NOT NULL,
                            `returnedAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`invoiceId`)
                                REFERENCES `dealership_invoices`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`invoiceLineId`)
                                REFERENCES `dealership_invoice_lines`(`id`)
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
                        `index_dealership_returns_invoiceId`
                        ON `dealership_returns`
                        (`invoiceId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealership_returns_invoiceLineId`
                        ON `dealership_returns`
                        (`invoiceLineId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealership_returns_productId`
                        ON `dealership_returns`
                        (`productId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealership_returns_returnedAt`
                        ON `dealership_returns`
                        (`returnedAt`)
                        """.trimIndent()
                    )
                }
            }


        private val MIGRATION_8_9 =
            object : androidx.room.migration.Migration(
                8,
                9
            ) {
                override fun migrate(
                    db: androidx.sqlite.db.SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        ALTER TABLE inventory_products
                        ADD COLUMN mrp REAL NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        ALTER TABLE inventory_products
                        ADD COLUMN rackLocation TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )
                }
            }


        private val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_companies` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `code` TEXT NOT NULL,
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
                        `index_dealer_business_companies_workspace_name`
                        ON `dealer_business_companies`
                        (`workspace`, `name`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_areas` (
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
                        `index_dealer_business_areas_workspace_name`
                        ON `dealer_business_areas`
                        (`workspace`, `name`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_customers` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `areaId` INTEGER,
                            `name` TEXT NOT NULL,
                            `customerCode` TEXT NOT NULL,
                            `phone` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `creditLimit` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`areaId`)
                                REFERENCES `dealer_business_areas`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_customers_areaId`
                        ON `dealer_business_customers` (`areaId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_customers_workspace_name`
                        ON `dealer_business_customers`
                        (`workspace`, `name`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_customers_workspace_phone`
                        ON `dealer_business_customers`
                        (`workspace`, `phone`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_customers_workspace_customerCode`
                        ON `dealer_business_customers`
                        (`workspace`, `customerCode`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_purchases` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `companyId` INTEGER,
                            `companyNameSnapshot` TEXT NOT NULL,
                            `invoiceNo` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `purchasedAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`companyId`)
                                REFERENCES `dealer_business_companies`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchases_companyId`
                        ON `dealer_business_purchases` (`companyId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchases_workspace_status`
                        ON `dealer_business_purchases`
                        (`workspace`, `status`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchases_purchasedAt`
                        ON `dealer_business_purchases` (`purchasedAt`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_purchase_lines` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `purchaseId` INTEGER NOT NULL,
                            `productId` INTEGER,
                            `inventoryBatchId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `batchNoSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `lineTotal` REAL NOT NULL,
                            FOREIGN KEY(`purchaseId`)
                                REFERENCES `dealer_business_purchases`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
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

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchase_lines_purchaseId`
                        ON `dealer_business_purchase_lines`
                        (`purchaseId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchase_lines_productId`
                        ON `dealer_business_purchase_lines`
                        (`productId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchase_lines_inventoryBatchId`
                        ON `dealer_business_purchase_lines`
                        (`inventoryBatchId`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_supplier_payments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `companyId` INTEGER,
                            `companyNameSnapshot` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `paidAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`companyId`)
                                REFERENCES `dealer_business_companies`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_supplier_payments_companyId`
                        ON `dealer_business_supplier_payments`
                        (`companyId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_supplier_payments_workspace_paidAt`
                        ON `dealer_business_supplier_payments`
                        (`workspace`, `paidAt`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_supplier_payment_allocations` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `paymentId` INTEGER NOT NULL,
                            `purchaseId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            FOREIGN KEY(`paymentId`)
                                REFERENCES `dealer_business_supplier_payments`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`purchaseId`)
                                REFERENCES `dealer_business_purchases`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_supplier_payment_allocations_paymentId`
                        ON `dealer_business_supplier_payment_allocations`
                        (`paymentId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_supplier_payment_allocations_purchaseId`
                        ON `dealer_business_supplier_payment_allocations`
                        (`purchaseId`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_sales` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `customerId` INTEGER,
                            `customerNameSnapshot` TEXT NOT NULL,
                            `invoiceNo` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `soldAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`customerId`)
                                REFERENCES `dealer_business_customers`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_customerId`
                        ON `dealer_business_sales` (`customerId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_workspace_status`
                        ON `dealer_business_sales`
                        (`workspace`, `status`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_soldAt`
                        ON `dealer_business_sales` (`soldAt`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_sale_lines` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `saleId` INTEGER NOT NULL,
                            `productId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitPrice` REAL NOT NULL,
                            `lineTotal` REAL NOT NULL,
                            FOREIGN KEY(`saleId`)
                                REFERENCES `dealer_business_sales`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`productId`)
                                REFERENCES `inventory_products`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sale_lines_saleId`
                        ON `dealer_business_sale_lines` (`saleId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sale_lines_productId`
                        ON `dealer_business_sale_lines` (`productId`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_stock_allocations` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `saleLineId` INTEGER NOT NULL,
                            `sourceStockBatchId` INTEGER,
                            `batchNoSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `totalCost` REAL NOT NULL,
                            FOREIGN KEY(`saleLineId`)
                                REFERENCES `dealer_business_sale_lines`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`sourceStockBatchId`)
                                REFERENCES `inventory_batches`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_stock_allocations_saleLineId`
                        ON `dealer_business_stock_allocations`
                        (`saleLineId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_stock_allocations_sourceStockBatchId`
                        ON `dealer_business_stock_allocations`
                        (`sourceStockBatchId`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_collections` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `customerId` INTEGER,
                            `customerNameSnapshot` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `collectedAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`customerId`)
                                REFERENCES `dealer_business_customers`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_collections_customerId`
                        ON `dealer_business_collections`
                        (`customerId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_collections_workspace_collectedAt`
                        ON `dealer_business_collections`
                        (`workspace`, `collectedAt`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_collection_allocations` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `collectionId` INTEGER NOT NULL,
                            `saleId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            FOREIGN KEY(`collectionId`)
                                REFERENCES `dealer_business_collections`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`saleId`)
                                REFERENCES `dealer_business_sales`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_collection_allocations_collectionId`
                        ON `dealer_business_collection_allocations`
                        (`collectionId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_collection_allocations_saleId`
                        ON `dealer_business_collection_allocations`
                        (`saleId`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_sales_returns` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `saleId` INTEGER NOT NULL,
                            `saleLineId` INTEGER NOT NULL,
                            `productId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitPrice` REAL NOT NULL,
                            `totalRefund` REAL NOT NULL,
                            `totalCost` REAL NOT NULL,
                            `returnType` TEXT NOT NULL,
                            `returnedAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`saleId`)
                                REFERENCES `dealer_business_sales`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`saleLineId`)
                                REFERENCES `dealer_business_sale_lines`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`productId`)
                                REFERENCES `inventory_products`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_returns_saleId`
                        ON `dealer_business_sales_returns` (`saleId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_returns_saleLineId`
                        ON `dealer_business_sales_returns`
                        (`saleLineId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_returns_productId`
                        ON `dealer_business_sales_returns`
                        (`productId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_returns_returnedAt`
                        ON `dealer_business_sales_returns`
                        (`returnedAt`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_sales_return_allocations` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `returnId` INTEGER NOT NULL,
                            `sourceStockBatchId` INTEGER,
                            `batchNoSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `totalCost` REAL NOT NULL,
                            FOREIGN KEY(`returnId`)
                                REFERENCES `dealer_business_sales_returns`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`sourceStockBatchId`)
                                REFERENCES `inventory_batches`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_return_allocations_returnId`
                        ON `dealer_business_sales_return_allocations`
                        (`returnId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_return_allocations_sourceStockBatchId`
                        ON `dealer_business_sales_return_allocations`
                        (`sourceStockBatchId`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_purchase_returns` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `purchaseId` INTEGER NOT NULL,
                            `purchaseLineId` INTEGER NOT NULL,
                            `productId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `totalValue` REAL NOT NULL,
                            `returnedAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`purchaseId`)
                                REFERENCES `dealer_business_purchases`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`purchaseLineId`)
                                REFERENCES `dealer_business_purchase_lines`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`productId`)
                                REFERENCES `inventory_products`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE SET NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchase_returns_purchaseId`
                        ON `dealer_business_purchase_returns`
                        (`purchaseId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchase_returns_purchaseLineId`
                        ON `dealer_business_purchase_returns`
                        (`purchaseLineId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchase_returns_productId`
                        ON `dealer_business_purchase_returns`
                        (`productId`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_purchase_returns_returnedAt`
                        ON `dealer_business_purchase_returns`
                        (`returnedAt`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `dealer_business_expenses` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `category` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `expenseAt` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    listOf(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_expenses_workspace_expenseAt`
                        ON `dealer_business_expenses`
                        (`workspace`, `expenseAt`)
                        """,
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_expenses_workspace_category`
                        ON `dealer_business_expenses`
                        (`workspace`, `category`)
                        """
                    ).forEach {
                        db.execSQL(it.trimIndent())
                    }
                }
            }


        private val MIGRATION_10_11 =
            object : androidx.room.migration.Migration(
                10,
                11
            ) {
                override fun migrate(
                    db: androidx.sqlite.db.SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        ALTER TABLE
                        `dealer_business_sales_return_allocations`
                        ADD COLUMN `saleAllocationId` INTEGER
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_dealer_business_sales_return_allocations_saleAllocationId`
                        ON `dealer_business_sales_return_allocations`
                        (`saleAllocationId`)
                        """.trimIndent()
                    )
                }
            }


        private val MIGRATION_11_12 =
            object : Migration(
                11,
                12
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    val statements =
                        listOf(
                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_product_packs` (
                                `productId` INTEGER NOT NULL,
                                `piecesPerBox` INTEGER NOT NULL,
                                `piecesPerSheet` INTEGER NOT NULL,
                                `workspace` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`productId`),
                                FOREIGN KEY(`productId`)
                                    REFERENCES `inventory_products`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_product_packs_workspace`
                            ON `dealer_business_product_packs`
                            (`workspace`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_delivery_people` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `name` TEXT NOT NULL,
                                `phone` TEXT NOT NULL,
                                `note` TEXT NOT NULL,
                                `workspace` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_people_workspace_name`
                            ON `dealer_business_delivery_people`
                            (`workspace`, `name`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_delivery_challans` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `deliveryPersonId` INTEGER,
                                `deliveryPersonNameSnapshot` TEXT NOT NULL,
                                `challanNo` TEXT NOT NULL,
                                `issuedAt` INTEGER NOT NULL,
                                `settledAt` INTEGER,
                                `status` TEXT NOT NULL,
                                `note` TEXT NOT NULL,
                                `workspace` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                FOREIGN KEY(`deliveryPersonId`)
                                    REFERENCES `dealer_business_delivery_people`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE SET NULL
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challans_deliveryPersonId`
                            ON `dealer_business_delivery_challans`
                            (`deliveryPersonId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challans_workspace_status`
                            ON `dealer_business_delivery_challans`
                            (`workspace`, `status`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challans_workspace_challanNo`
                            ON `dealer_business_delivery_challans`
                            (`workspace`, `challanNo`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_delivery_challan_lines` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `challanId` INTEGER NOT NULL,
                                `productId` INTEGER,
                                `productNameSnapshot` TEXT NOT NULL,
                                `boxCount` INTEGER NOT NULL,
                                `sheetCount` INTEGER NOT NULL,
                                `loosePieces` INTEGER NOT NULL,
                                `piecesPerBoxSnapshot` INTEGER NOT NULL,
                                `piecesPerSheetSnapshot` INTEGER NOT NULL,
                                `quantityPieces` INTEGER NOT NULL,
                                FOREIGN KEY(`challanId`)
                                    REFERENCES `dealer_business_delivery_challans`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE,
                                FOREIGN KEY(`productId`)
                                    REFERENCES `inventory_products`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE SET NULL
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challan_lines_challanId`
                            ON `dealer_business_delivery_challan_lines`
                            (`challanId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challan_lines_productId`
                            ON `dealer_business_delivery_challan_lines`
                            (`productId`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_delivery_challan_allocations` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `challanLineId` INTEGER NOT NULL,
                                `sourceStockBatchId` INTEGER,
                                `batchNoSnapshot` TEXT NOT NULL,
                                `quantityPieces` INTEGER NOT NULL,
                                `unitCost` REAL NOT NULL,
                                `totalCost` REAL NOT NULL,
                                FOREIGN KEY(`challanLineId`)
                                    REFERENCES `dealer_business_delivery_challan_lines`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE,
                                FOREIGN KEY(`sourceStockBatchId`)
                                    REFERENCES `inventory_batches`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE SET NULL
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challan_allocations_challanLineId`
                            ON `dealer_business_delivery_challan_allocations`
                            (`challanLineId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challan_allocations_sourceStockBatchId`
                            ON `dealer_business_delivery_challan_allocations`
                            (`sourceStockBatchId`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_delivery_challan_sales` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `challanId` INTEGER NOT NULL,
                                `saleId` INTEGER NOT NULL,
                                FOREIGN KEY(`challanId`)
                                    REFERENCES `dealer_business_delivery_challans`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE,
                                FOREIGN KEY(`saleId`)
                                    REFERENCES `dealer_business_sales`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challan_sales_challanId`
                            ON `dealer_business_delivery_challan_sales`
                            (`challanId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challan_sales_saleId`
                            ON `dealer_business_delivery_challan_sales`
                            (`saleId`)
                            """,
                            """
                            CREATE UNIQUE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_challan_sales_challanId_saleId`
                            ON `dealer_business_delivery_challan_sales`
                            (`challanId`, `saleId`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_delivery_sale_allocations` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `challanLineId` INTEGER NOT NULL,
                                `challanAllocationId` INTEGER NOT NULL,
                                `saleLineId` INTEGER NOT NULL,
                                `quantityPieces` INTEGER NOT NULL,
                                `unitCost` REAL NOT NULL,
                                `totalCost` REAL NOT NULL,
                                FOREIGN KEY(`challanLineId`)
                                    REFERENCES `dealer_business_delivery_challan_lines`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE,
                                FOREIGN KEY(`challanAllocationId`)
                                    REFERENCES `dealer_business_delivery_challan_allocations`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE,
                                FOREIGN KEY(`saleLineId`)
                                    REFERENCES `dealer_business_sale_lines`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_sale_allocations_challanLineId`
                            ON `dealer_business_delivery_sale_allocations`
                            (`challanLineId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_sale_allocations_challanAllocationId`
                            ON `dealer_business_delivery_sale_allocations`
                            (`challanAllocationId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_sale_allocations_saleLineId`
                            ON `dealer_business_delivery_sale_allocations`
                            (`saleLineId`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_delivery_settlements` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `challanId` INTEGER NOT NULL,
                                `cashHandedOver` REAL NOT NULL,
                                `receivedAt` INTEGER NOT NULL,
                                `note` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                FOREIGN KEY(`challanId`)
                                    REFERENCES `dealer_business_delivery_challans`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE
                            )
                            """,
                            """
                            CREATE UNIQUE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_settlements_challanId`
                            ON `dealer_business_delivery_settlements`
                            (`challanId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_settlements_receivedAt`
                            ON `dealer_business_delivery_settlements`
                            (`receivedAt`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_delivery_settlement_lines` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `settlementId` INTEGER NOT NULL,
                                `challanLineId` INTEGER NOT NULL,
                                `soldPieces` INTEGER NOT NULL,
                                `returnedPieces` INTEGER NOT NULL,
                                `damagedPieces` INTEGER NOT NULL,
                                `note` TEXT NOT NULL,
                                FOREIGN KEY(`settlementId`)
                                    REFERENCES `dealer_business_delivery_settlements`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE,
                                FOREIGN KEY(`challanLineId`)
                                    REFERENCES `dealer_business_delivery_challan_lines`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_settlement_lines_settlementId`
                            ON `dealer_business_delivery_settlement_lines`
                            (`settlementId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_settlement_lines_challanLineId`
                            ON `dealer_business_delivery_settlement_lines`
                            (`challanLineId`)
                            """,
                            """
                            CREATE UNIQUE INDEX IF NOT EXISTS
                            `index_dealer_business_delivery_settlement_lines_settlementId_challanLineId`
                            ON `dealer_business_delivery_settlement_lines`
                            (`settlementId`, `challanLineId`)
                            """,

                            """
                            CREATE TABLE IF NOT EXISTS
                            `dealer_business_damages` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `productId` INTEGER,
                                `sourceStockBatchId` INTEGER,
                                `deliveryChallanId` INTEGER,
                                `productNameSnapshot` TEXT NOT NULL,
                                `batchNoSnapshot` TEXT NOT NULL,
                                `quantityPieces` INTEGER NOT NULL,
                                `unitCost` REAL NOT NULL,
                                `totalCost` REAL NOT NULL,
                                `sourceType` TEXT NOT NULL,
                                `reason` TEXT NOT NULL,
                                `damagedAt` INTEGER NOT NULL,
                                `note` TEXT NOT NULL,
                                `workspace` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                FOREIGN KEY(`productId`)
                                    REFERENCES `inventory_products`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE SET NULL,
                                FOREIGN KEY(`sourceStockBatchId`)
                                    REFERENCES `inventory_batches`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE SET NULL,
                                FOREIGN KEY(`deliveryChallanId`)
                                    REFERENCES `dealer_business_delivery_challans`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE SET NULL
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_damages_productId`
                            ON `dealer_business_damages`
                            (`productId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_damages_sourceStockBatchId`
                            ON `dealer_business_damages`
                            (`sourceStockBatchId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_damages_deliveryChallanId`
                            ON `dealer_business_damages`
                            (`deliveryChallanId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_dealer_business_damages_workspace_damagedAt`
                            ON `dealer_business_damages`
                            (`workspace`, `damagedAt`)
                            """
                        )

                    statements.forEach {
                        db.execSQL(
                            it.trimIndent()
                        )
                    }
                }
            }


        private val MIGRATION_12_13 =
            object : Migration(
                12,
                13
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    val statements =
                        listOf(
                            """
                            CREATE TABLE IF NOT EXISTS
                            `retail_sales` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `invoiceNo` TEXT NOT NULL,
                                `bakiPersonId` INTEGER,
                                `customerName` TEXT NOT NULL,
                                `customerPhone` TEXT NOT NULL,
                                `subtotal` REAL NOT NULL,
                                `discount` REAL NOT NULL,
                                `total` REAL NOT NULL,
                                `paid` REAL NOT NULL,
                                `paymentMethod` TEXT NOT NULL,
                                `status` TEXT NOT NULL,
                                `note` TEXT NOT NULL,
                                `workspace` TEXT NOT NULL,
                                `businessKey` TEXT NOT NULL,
                                `soldAt` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_retail_sales_workspace_businessKey_soldAt`
                            ON `retail_sales`
                            (`workspace`, `businessKey`, `soldAt`)
                            """,
                            """
                            CREATE UNIQUE INDEX IF NOT EXISTS
                            `index_retail_sales_workspace_businessKey_invoiceNo`
                            ON `retail_sales`
                            (`workspace`, `businessKey`, `invoiceNo`)
                            """,
                            """
                            CREATE TABLE IF NOT EXISTS
                            `retail_sale_lines` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `saleId` INTEGER NOT NULL,
                                `productId` INTEGER NOT NULL,
                                `productNameSnapshot` TEXT NOT NULL,
                                `skuSnapshot` TEXT NOT NULL,
                                `unitSnapshot` TEXT NOT NULL,
                                `quantity` INTEGER NOT NULL,
                                `unitPrice` REAL NOT NULL,
                                `unitCost` REAL NOT NULL,
                                `lineTotal` REAL NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                FOREIGN KEY(`saleId`)
                                    REFERENCES `retail_sales`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_retail_sale_lines_saleId`
                            ON `retail_sale_lines`
                            (`saleId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_retail_sale_lines_productId`
                            ON `retail_sale_lines`
                            (`productId`)
                            """,
                            """
                            CREATE TABLE IF NOT EXISTS
                            `retail_sale_stock_allocations` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `saleLineId` INTEGER NOT NULL,
                                `batchId` INTEGER NOT NULL,
                                `quantity` INTEGER NOT NULL,
                                `unitCost` REAL NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                FOREIGN KEY(`saleLineId`)
                                    REFERENCES `retail_sale_lines`(`id`)
                                    ON UPDATE NO ACTION
                                    ON DELETE CASCADE
                            )
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_retail_sale_stock_allocations_saleLineId`
                            ON `retail_sale_stock_allocations`
                            (`saleLineId`)
                            """,
                            """
                            CREATE INDEX IF NOT EXISTS
                            `index_retail_sale_stock_allocations_batchId`
                            ON `retail_sale_stock_allocations`
                            (`batchId`)
                            """
                        )

                    statements.forEach {
                        db.execSQL(
                            it.trimIndent()
                        )
                    }
                }
            }

        private val MIGRATION_13_14 =
            object : Migration(
                13,
                14
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `inventory_product_units` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `productId` INTEGER NOT NULL,
                            `unitName` TEXT NOT NULL,
                            `unitKey` TEXT NOT NULL,
                            `baseQuantity` INTEGER NOT NULL,
                            `sortOrder` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
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
                        `index_inventory_product_units_productId`
                        ON `inventory_product_units` (`productId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_inventory_product_units_productId_unitKey`
                        ON `inventory_product_units`
                        (`productId`, `unitKey`)
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_14_15 =
            object : Migration(
                14,
                15
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        ALTER TABLE `retail_sale_lines`
                        ADD COLUMN `unitFactor`
                        INTEGER NOT NULL DEFAULT 1
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        ALTER TABLE `retail_sale_lines`
                        ADD COLUMN `baseQuantity`
                        INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    /*
                     * Existing v14 sale quantities were already stored
                     * directly in base stock units.
                     */
                    db.execSQL(
                        """
                        UPDATE `retail_sale_lines`
                        SET `baseQuantity` = `quantity`
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_15_16 =
            object : Migration(
                15,
                16
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        ALTER TABLE
                        `dealer_business_delivery_challan_lines`
                        ADD COLUMN `unitSnapshot`
                        TEXT NOT NULL DEFAULT 'pcs'
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        ALTER TABLE
                        `dealer_business_delivery_challan_lines`
                        ADD COLUMN `unitFactor`
                        INTEGER NOT NULL DEFAULT 1
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        ALTER TABLE
                        `dealer_business_delivery_challan_lines`
                        ADD COLUMN `enteredQuantity`
                        INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    /*
                     * Old delivery data was stored as base-piece
                     * quantity. Preserve that meaning exactly.
                     */
                    db.execSQL(
                        """
                        UPDATE
                        `dealer_business_delivery_challan_lines`
                        SET
                            `enteredQuantity` =
                                `quantityPieces`,
                            `unitSnapshot` =
                                COALESCE(
                                    (
                                        SELECT `unit`
                                        FROM `inventory_products`
                                        WHERE
                                            `inventory_products`.`id` =
                                            `dealer_business_delivery_challan_lines`.`productId`
                                    ),
                                    'pcs'
                                ),
                            `unitFactor` = 1
                        """.trimIndent()
                    )
                }
            }


        private val MIGRATION_16_17 =
            object : Migration(
                16,
                17
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `retail_sale_payments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventKey` TEXT NOT NULL,
                            `saleId` INTEGER NOT NULL,
                            `financialAccountId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `paymentMethod` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `paidAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`saleId`)
                                REFERENCES `retail_sales`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_retail_sale_payments_saleId`
                        ON `retail_sale_payments`
                        (`saleId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_retail_sale_payments_financialAccountId`
                        ON `retail_sale_payments`
                        (`financialAccountId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_retail_sale_payments_eventKey`
                        ON `retail_sale_payments`
                        (`eventKey`)
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_17_18 =
            object : Migration(
                17,
                18
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_suppliers` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `phone` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `businessKey` TEXT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_suppliers_workspace_businessKey_name`
                        ON `purchase_suppliers`
                        (`workspace`, `businessKey`, `name`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_bills` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `purchaseNo` TEXT NOT NULL,
                            `supplierId` INTEGER NOT NULL,
                            `subtotal` REAL NOT NULL,
                            `discount` REAL NOT NULL,
                            `total` REAL NOT NULL,
                            `status` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `businessKey` TEXT NOT NULL,
                            `purchasedAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`supplierId`)
                                REFERENCES `purchase_suppliers`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_bills_supplierId`
                        ON `purchase_bills` (`supplierId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_bills_workspace_businessKey_purchasedAt`
                        ON `purchase_bills`
                        (`workspace`, `businessKey`, `purchasedAt`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_purchase_bills_workspace_businessKey_purchaseNo`
                        ON `purchase_bills`
                        (`workspace`, `businessKey`, `purchaseNo`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_bill_lines` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `productId` INTEGER NOT NULL,
                            `stockBatchId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `skuSnapshot` TEXT NOT NULL,
                            `unitSnapshot` TEXT NOT NULL,
                            `unitFactor` INTEGER NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `baseQuantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `lineTotal` REAL NOT NULL,
                            `batchNo` TEXT NOT NULL,
                            `expiryDate` INTEGER,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_billId` ON `purchase_bill_lines` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_productId` ON `purchase_bill_lines` (`productId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_stockBatchId` ON `purchase_bill_lines` (`stockBatchId`)"
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_payments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventKey` TEXT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `financialAccountId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `paymentMethod` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `paidAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_payments_billId` ON `purchase_payments` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_payments_financialAccountId` ON `purchase_payments` (`financialAccountId`)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_payments_eventKey` ON `purchase_payments` (`eventKey`)"
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_returns` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventKey` TEXT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `purchaseLineId` INTEGER NOT NULL,
                            `productId` INTEGER NOT NULL,
                            `stockBatchId` INTEGER,
                            `baseQuantity` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `returnedAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`purchaseLineId`)
                                REFERENCES `purchase_bill_lines`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_returns_billId` ON `purchase_returns` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_returns_purchaseLineId` ON `purchase_returns` (`purchaseLineId`)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_returns_eventKey` ON `purchase_returns` (`eventKey`)"
                    )
                }
            }

        private val MIGRATION_17_18 =
            object : Migration(
                17,
                18
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_suppliers` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `phone` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `businessKey` TEXT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_suppliers_workspace_businessKey_name`
                        ON `purchase_suppliers`
                        (`workspace`, `businessKey`, `name`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_bills` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `purchaseNo` TEXT NOT NULL,
                            `supplierId` INTEGER NOT NULL,
                            `subtotal` REAL NOT NULL,
                            `discount` REAL NOT NULL,
                            `total` REAL NOT NULL,
                            `status` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `businessKey` TEXT NOT NULL,
                            `purchasedAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`supplierId`)
                                REFERENCES `purchase_suppliers`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_bills_supplierId`
                        ON `purchase_bills` (`supplierId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_bills_workspace_businessKey_purchasedAt`
                        ON `purchase_bills`
                        (`workspace`, `businessKey`, `purchasedAt`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_purchase_bills_workspace_businessKey_purchaseNo`
                        ON `purchase_bills`
                        (`workspace`, `businessKey`, `purchaseNo`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_bill_lines` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `productId` INTEGER NOT NULL,
                            `stockBatchId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `skuSnapshot` TEXT NOT NULL,
                            `unitSnapshot` TEXT NOT NULL,
                            `unitFactor` INTEGER NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `baseQuantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `lineTotal` REAL NOT NULL,
                            `batchNo` TEXT NOT NULL,
                            `expiryDate` INTEGER,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_billId` ON `purchase_bill_lines` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_productId` ON `purchase_bill_lines` (`productId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_stockBatchId` ON `purchase_bill_lines` (`stockBatchId`)"
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_payments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventKey` TEXT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `financialAccountId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `paymentMethod` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `paidAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_payments_billId` ON `purchase_payments` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_payments_financialAccountId` ON `purchase_payments` (`financialAccountId`)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_payments_eventKey` ON `purchase_payments` (`eventKey`)"
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_returns` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventKey` TEXT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `purchaseLineId` INTEGER NOT NULL,
                            `productId` INTEGER NOT NULL,
                            `stockBatchId` INTEGER,
                            `baseQuantity` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `returnedAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`purchaseLineId`)
                                REFERENCES `purchase_bill_lines`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_returns_billId` ON `purchase_returns` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_returns_purchaseLineId` ON `purchase_returns` (`purchaseLineId`)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_returns_eventKey` ON `purchase_returns` (`eventKey`)"
                    )
                }
            }

        private val MIGRATION_17_18 =
            object : Migration(
                17,
                18
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_suppliers` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `phone` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `businessKey` TEXT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_suppliers_workspace_businessKey_name`
                        ON `purchase_suppliers`
                        (`workspace`, `businessKey`, `name`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_bills` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `purchaseNo` TEXT NOT NULL,
                            `supplierId` INTEGER NOT NULL,
                            `subtotal` REAL NOT NULL,
                            `discount` REAL NOT NULL,
                            `total` REAL NOT NULL,
                            `status` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `businessKey` TEXT NOT NULL,
                            `purchasedAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`supplierId`)
                                REFERENCES `purchase_suppliers`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_bills_supplierId`
                        ON `purchase_bills` (`supplierId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_purchase_bills_workspace_businessKey_purchasedAt`
                        ON `purchase_bills`
                        (`workspace`, `businessKey`, `purchasedAt`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_purchase_bills_workspace_businessKey_purchaseNo`
                        ON `purchase_bills`
                        (`workspace`, `businessKey`, `purchaseNo`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_bill_lines` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `productId` INTEGER NOT NULL,
                            `stockBatchId` INTEGER,
                            `productNameSnapshot` TEXT NOT NULL,
                            `skuSnapshot` TEXT NOT NULL,
                            `unitSnapshot` TEXT NOT NULL,
                            `unitFactor` INTEGER NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `baseQuantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            `lineTotal` REAL NOT NULL,
                            `batchNo` TEXT NOT NULL,
                            `expiryDate` INTEGER,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_billId` ON `purchase_bill_lines` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_productId` ON `purchase_bill_lines` (`productId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_bill_lines_stockBatchId` ON `purchase_bill_lines` (`stockBatchId`)"
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_payments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventKey` TEXT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `financialAccountId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `paymentMethod` TEXT NOT NULL,
                            `note` TEXT NOT NULL,
                            `paidAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_payments_billId` ON `purchase_payments` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_payments_financialAccountId` ON `purchase_payments` (`financialAccountId`)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_payments_eventKey` ON `purchase_payments` (`eventKey`)"
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `purchase_returns` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventKey` TEXT NOT NULL,
                            `billId` INTEGER NOT NULL,
                            `purchaseLineId` INTEGER NOT NULL,
                            `productId` INTEGER NOT NULL,
                            `stockBatchId` INTEGER,
                            `baseQuantity` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `returnedAt` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`billId`)
                                REFERENCES `purchase_bills`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE,
                            FOREIGN KEY(`purchaseLineId`)
                                REFERENCES `purchase_bill_lines`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_returns_billId` ON `purchase_returns` (`billId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_purchase_returns_purchaseLineId` ON `purchase_returns` (`purchaseLineId`)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_returns_eventKey` ON `purchase_returns` (`eventKey`)"
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
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_17_18,
                    MIGRATION_17_18
                )
                .build()
                .also { INSTANCE = it }
        }
    }
}

data class InventoryBackupData(
    val products: List<ProductEntity>,
    val batches: List<StockBatchEntity>,
    val unitConversions: List<ProductUnitConversionEntity>,
    val retailSales: List<RetailSaleEntity>,
    val retailSaleLines: List<RetailSaleLineEntity>,
    val retailSaleStockAllocations:
        List<RetailSaleStockAllocationEntity>,
    val retailSalePayments: List<RetailSalePaymentEntity>
)

object InventoryBackupBridge {
    suspend fun export(context: Context): InventoryBackupData {
        val dao = InventoryDatabase.get(context).dao()
        return InventoryBackupData(
            products = dao.getAllProducts(),
            batches = dao.getAllBatches(),
            unitConversions =
                dao.getAllProductUnitConversions(),
            retailSales =
                dao.getAllRetailSales(),
            retailSaleLines =
                dao.getAllRetailSaleLines(),
            retailSaleStockAllocations =
                dao.getAllRetailSaleStockAllocations(),
            retailSalePayments =
                dao.getAllRetailSalePayments()
        )
    }

    suspend fun restore(
        context: Context,
        products: List<ProductEntity>,
        batches: List<StockBatchEntity>,
        unitConversions:
            List<ProductUnitConversionEntity> =
                emptyList(),
        retailSales:
            List<RetailSaleEntity> =
                emptyList(),
        retailSaleLines:
            List<RetailSaleLineEntity> =
                emptyList(),
        retailSaleStockAllocations:
            List<RetailSaleStockAllocationEntity> =
                emptyList(),
        retailSalePayments:
            List<RetailSalePaymentEntity> =
                emptyList()
    ) {
        val db = InventoryDatabase.get(context)
        val dao = db.dao()

        db.withTransaction {
            dao.clearRetailSalePayments()
            dao.clearRetailSaleStockAllocations()
            dao.clearRetailSaleLines()
            dao.clearRetailSales()

            dao.clearProductUnitConversions()
            dao.clearBatches()
            dao.clearProducts()

            products.forEach {
                dao.insertProduct(it)
            }

            unitConversions.forEach {
                dao.insertProductUnitConversion(it)
            }

            batches.forEach {
                dao.insertBatch(it)
            }

            retailSales.forEach {
                dao.insertRetailSale(it)
            }

            retailSaleLines.forEach {
                dao.insertRetailSaleLine(it)
            }

            retailSaleStockAllocations.forEach {
                dao.insertRetailSaleStockAllocation(it)
            }

            retailSalePayments.forEach {
                dao.insertRetailSalePayment(it)
            }
        }
    }

    fun retailSalesToJson(
        items: List<RetailSaleEntity>
    ): JSONArray =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                    put("id", item.id)
                    put("invoiceNo", item.invoiceNo)
                    item.bakiPersonId?.let { value ->
                        put("bakiPersonId", value)
                    }
                    put("customerName", item.customerName)
                    put("customerPhone", item.customerPhone)
                    put("subtotal", item.subtotal)
                    put("discount", item.discount)
                    put("total", item.total)
                    put("paid", item.paid)
                    put("paymentMethod", item.paymentMethod)
                    put("status", item.status)
                    put("note", item.note)
                    put("workspace", item.workspace)
                    put("businessKey", item.businessKey)
                    put("soldAt", item.soldAt)
                    put("createdAt", item.createdAt)
                    }
                )
            }
        }

    fun retailSalesFromJson(
        array: JSONArray
    ): List<RetailSaleEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RetailSaleEntity(
                        id = item.getLong("id"),
                        invoiceNo = item.getString("invoiceNo"),
                        bakiPersonId = if (item.has("bakiPersonId") && !item.isNull("bakiPersonId")) item.getLong("bakiPersonId") else null,
                        customerName = item.getString("customerName"),
                        customerPhone = item.getString("customerPhone"),
                        subtotal = item.getDouble("subtotal"),
                        discount = item.getDouble("discount"),
                        total = item.getDouble("total"),
                        paid = item.getDouble("paid"),
                        paymentMethod = item.getString("paymentMethod"),
                        status = item.getString("status"),
                        note = item.getString("note"),
                        workspace = item.getString("workspace"),
                        businessKey = item.getString("businessKey"),
                        soldAt = item.getLong("soldAt"),
                        createdAt = item.getLong("createdAt")
                    )
                )
            }
        }


    fun retailSaleLinesToJson(
        items: List<RetailSaleLineEntity>
    ): JSONArray =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                    put("id", item.id)
                    put("saleId", item.saleId)
                    put("productId", item.productId)
                    put("productNameSnapshot", item.productNameSnapshot)
                    put("skuSnapshot", item.skuSnapshot)
                    put("unitSnapshot", item.unitSnapshot)
                    put("unitFactor", item.unitFactor)
                    put("quantity", item.quantity)
                    put("baseQuantity", item.baseQuantity)
                    put("unitPrice", item.unitPrice)
                    put("unitCost", item.unitCost)
                    put("lineTotal", item.lineTotal)
                    put("createdAt", item.createdAt)
                    }
                )
            }
        }

    fun retailSaleLinesFromJson(
        array: JSONArray
    ): List<RetailSaleLineEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RetailSaleLineEntity(
                        id = item.getLong("id"),
                        saleId = item.getLong("saleId"),
                        productId = item.getLong("productId"),
                        productNameSnapshot = item.getString("productNameSnapshot"),
                        skuSnapshot = item.getString("skuSnapshot"),
                        unitSnapshot = item.getString("unitSnapshot"),
                        unitFactor = item.getInt("unitFactor"),
                        quantity = item.getInt("quantity"),
                        baseQuantity = item.getInt("baseQuantity"),
                        unitPrice = item.getDouble("unitPrice"),
                        unitCost = item.getDouble("unitCost"),
                        lineTotal = item.getDouble("lineTotal"),
                        createdAt = item.getLong("createdAt")
                    )
                )
            }
        }


    fun retailSaleStockAllocationsToJson(
        items: List<RetailSaleStockAllocationEntity>
    ): JSONArray =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                    put("id", item.id)
                    put("saleLineId", item.saleLineId)
                    put("batchId", item.batchId)
                    put("quantity", item.quantity)
                    put("unitCost", item.unitCost)
                    put("createdAt", item.createdAt)
                    }
                )
            }
        }

    fun retailSaleStockAllocationsFromJson(
        array: JSONArray
    ): List<RetailSaleStockAllocationEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RetailSaleStockAllocationEntity(
                        id = item.getLong("id"),
                        saleLineId = item.getLong("saleLineId"),
                        batchId = item.getLong("batchId"),
                        quantity = item.getInt("quantity"),
                        unitCost = item.getDouble("unitCost"),
                        createdAt = item.getLong("createdAt")
                    )
                )
            }
        }


    fun retailSalePaymentsToJson(
        items: List<RetailSalePaymentEntity>
    ): JSONArray =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                    put("id", item.id)
                    put("eventKey", item.eventKey)
                    put("saleId", item.saleId)
                    put("financialAccountId", item.financialAccountId)
                    put("amount", item.amount)
                    put("paymentMethod", item.paymentMethod)
                    put("note", item.note)
                    put("paidAt", item.paidAt)
                    put("createdAt", item.createdAt)
                    }
                )
            }
        }

    fun retailSalePaymentsFromJson(
        array: JSONArray
    ): List<RetailSalePaymentEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RetailSalePaymentEntity(
                        id = item.getLong("id"),
                        eventKey = item.getString("eventKey"),
                        saleId = item.getLong("saleId"),
                        financialAccountId = item.getLong("financialAccountId"),
                        amount = item.getDouble("amount"),
                        paymentMethod = item.getString("paymentMethod"),
                        note = item.getString("note"),
                        paidAt = item.getLong("paidAt"),
                        createdAt = item.getLong("createdAt")
                    )
                )
            }
        }

}
