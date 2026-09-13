package com.familykhata.app.agency

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AgencyClientEntity::class,
        AgencyProjectEntity::class,
        AgencyPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AgencyDatabase :
    RoomDatabase() {

    abstract fun dao(): AgencyDao

    companion object {
        @Volatile
        private var INSTANCE:
            AgencyDatabase? = null

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
                        ).build().also {
                            INSTANCE = it
                        }
                }
    }
}
