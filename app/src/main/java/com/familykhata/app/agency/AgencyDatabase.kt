package com.familykhata.app.agency

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AgencyClientEntity::class,
        AgencyProjectEntity::class,
        AgencyChargeEntity::class,
        AgencyPaymentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AgencyDatabase :
    RoomDatabase() {

    abstract fun dao(): AgencyDao

    companion object {

        @Volatile
        private var INSTANCE:
            AgencyDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `agency_charges` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `projectId` INTEGER NOT NULL,
                            `chargeType` TEXT NOT NULL,
                            `periodKey` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `dueDate` INTEGER,
                            `note` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`projectId`)
                                REFERENCES `agency_projects`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agency_charges_projectId`
                        ON `agency_charges` (`projectId`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agency_charges_dueDate`
                        ON `agency_charges` (`dueDate`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_agency_charges_projectId_chargeType_periodKey`
                        ON `agency_charges`
                        (`projectId`, `chargeType`, `periodKey`)
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        INSERT INTO agency_charges (
                            projectId,
                            chargeType,
                            periodKey,
                            amount,
                            dueDate,
                            note,
                            createdAt
                        )
                        SELECT
                            id,
                            'PACKAGE',
                            'INITIAL',
                            totalPrice,
                            dueDate,
                            'Migrated project price',
                            createdAt
                        FROM agency_projects
                        WHERE totalPrice > 0
                        """.trimIndent()
                    )
                }
            }

        fun get(
            context: Context
        ): AgencyDatabase =
            INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            AgencyDatabase::class.java,
                            "hisabi-agency.db"
                        )
                            .addMigrations(
                                MIGRATION_1_2
                            )
                            .build()
                            .also {
                                INSTANCE = it
                            }
                }
    }
}
