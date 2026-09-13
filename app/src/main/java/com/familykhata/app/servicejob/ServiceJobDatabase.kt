package com.familykhata.app.servicejob

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ServiceCustomerEntity::class,
        ServiceJobEntity::class,
        ServiceChargeEntity::class,
        ServicePaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ServiceJobDatabase :
    RoomDatabase() {

    abstract fun dao(): ServiceJobDao

    companion object {

        @Volatile
        private var INSTANCE:
            ServiceJobDatabase? = null

        fun get(
            context: Context
        ): ServiceJobDatabase =
            INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            ServiceJobDatabase::class.java,
                            "hisabi-service-job.db"
                        ).build().also {
                            INSTANCE = it
                        }
                }
    }
}
