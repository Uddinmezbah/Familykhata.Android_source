package com.familykhata.app.coaching

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CoachingStudentEntity::class,
        CoachingBatchEntity::class,
        CoachingEnrollmentEntity::class,
        CoachingChargeEntity::class,
        CoachingPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CoachingDatabase : RoomDatabase() {

    abstract fun dao(): CoachingDao

    companion object {
        @Volatile
        private var INSTANCE: CoachingDatabase? = null

        fun get(context: Context): CoachingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoachingDatabase::class.java,
                    "hisabi-coaching.db"
                ).build().also {
                    INSTANCE = it
                }
            }
    }
}
