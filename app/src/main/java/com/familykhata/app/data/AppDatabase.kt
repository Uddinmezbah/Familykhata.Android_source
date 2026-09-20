package com.familykhata.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        BakiPersonEntity::class,
        BakiEntryEntity::class,
        FinancialAccountEntity::class,
        FinancialAccountEntryEntity::class,
        DigitalServiceTransactionEntity::class,
        BusinessProfileEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FamilyKhataDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN workspace TEXT NOT NULL DEFAULT 'FAMILY'"
                )
                db.execSQL(
                    "ALTER TABLE baki_people ADD COLUMN workspace TEXT NOT NULL DEFAULT 'FAMILY'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE baki_entries ADD COLUMN dueAt INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE baki_entries ADD COLUMN sourceKey TEXT"
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_baki_entries_sourceKey
                    ON baki_entries(sourceKey)
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
                        CREATE TABLE IF NOT EXISTS `financial_accounts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `provider` TEXT NOT NULL,
                            `openingBalance` REAL NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_financial_accounts_workspace_name`
                        ON `financial_accounts` (`workspace`, `name`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_financial_accounts_workspace_type`
                        ON `financial_accounts` (`workspace`, `type`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `financial_account_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `accountId` INTEGER NOT NULL,
                            `entryType` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `balanceDelta` REAL NOT NULL,
                            `relatedAccountId` INTEGER,
                            `transferGroupId` TEXT,
                            `sourceKey` TEXT,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`)
                                REFERENCES `financial_accounts`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_financial_account_entries_accountId`
                        ON `financial_account_entries` (`accountId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_financial_account_entries_workspace_createdAt`
                        ON `financial_account_entries`
                        (`workspace`, `createdAt`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_financial_account_entries_transferGroupId`
                        ON `financial_account_entries` (`transferGroupId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_financial_account_entries_sourceKey`
                        ON `financial_account_entries` (`sourceKey`)
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
                        ALTER TABLE transactions
                        ADD COLUMN sourceKey TEXT
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_transactions_sourceKey`
                        ON `transactions` (`sourceKey`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `digital_service_transactions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventKey` TEXT NOT NULL,
                            `serviceType` TEXT NOT NULL,
                            `sourceAccountId` INTEGER NOT NULL,
                            `destinationAccountId` INTEGER NOT NULL,
                            `serviceAmount` REAL NOT NULL,
                            `customerFee` REAL NOT NULL,
                            `providerCharge` REAL NOT NULL,
                            `customerPaid` REAL NOT NULL,
                            `providerCost` REAL NOT NULL,
                            `sourceAmount` REAL NOT NULL,
                            `destinationAmount` REAL NOT NULL,
                            `profit` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `workspace` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`sourceAccountId`)
                                REFERENCES `financial_accounts`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION,
                            FOREIGN KEY(`destinationAccountId`)
                                REFERENCES `financial_accounts`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_digital_service_transactions_eventKey`
                        ON `digital_service_transactions` (`eventKey`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_digital_service_transactions_sourceAccountId`
                        ON `digital_service_transactions` (`sourceAccountId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_digital_service_transactions_destinationAccountId`
                        ON `digital_service_transactions` (`destinationAccountId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_digital_service_transactions_serviceType`
                        ON `digital_service_transactions` (`serviceType`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_digital_service_transactions_workspace_createdAt`
                        ON `digital_service_transactions`
                        (`workspace`, `createdAt`)
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
                        ALTER TABLE transactions
                        ADD COLUMN financialAccountId INTEGER
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_transactions_financialAccountId`
                        ON `transactions`
                        (`financialAccountId`)
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
                        CREATE TABLE IF NOT EXISTS `business_profiles` (
                            `businessId` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `businessType` TEXT NOT NULL,
                            `phone` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `logoPath` TEXT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            PRIMARY KEY(`businessId`)
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_business_profiles_name`
                        ON `business_profiles` (`name`)
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE baki_people ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE financial_accounts ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE financial_account_entries ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE digital_service_transactions ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                }
            }
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "family-khata.db"
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                )
                .build()
                .also { INSTANCE = it }
        }
    }
}
