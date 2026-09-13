package com.familykhata.app.membership

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MembershipMemberEntity::class,
        MembershipPlanEntity::class,
        MembershipSubscriptionEntity::class,
        MembershipPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MembershipDatabase :
    RoomDatabase() {

    abstract fun dao(): MembershipDao

    companion object {

        @Volatile
        private var INSTANCE:
            MembershipDatabase? = null

        fun get(
            context: Context
        ): MembershipDatabase =
            INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            MembershipDatabase::class.java,
                            "hisabi-membership.db"
                        ).build().also {
                            INSTANCE = it
                        }
                }
    }
}
