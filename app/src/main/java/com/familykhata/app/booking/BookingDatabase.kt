package com.familykhata.app.booking

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookingCustomerEntity::class,
        BookingEntity::class,
        BookingChargeEntity::class,
        BookingPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BookingDatabase :
    RoomDatabase() {

    abstract fun dao(): BookingDao

    companion object {

        @Volatile
        private var INSTANCE:
            BookingDatabase? = null

        fun get(
            context: Context
        ): BookingDatabase =
            INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            BookingDatabase::class.java,
                            "hisabi-booking.db"
                        ).build().also {
                            INSTANCE = it
                        }
                }
    }
}
