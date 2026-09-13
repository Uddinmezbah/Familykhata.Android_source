package com.familykhata.app.membership

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "membership_members",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class MembershipMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "membership_plans",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class MembershipPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val durationDays: Int,
    val fee: Double,
    val note: String = "",
    val workspace: String = "SHOP",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "membership_subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = MembershipMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MembershipPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("memberId"),
        Index("planId"),
        Index("endDate"),
        Index("status")
    ]
)
data class MembershipSubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val planId: Long,
    val startDate: Long,
    val endDate: Long,
    val totalFee: Double,
    val status: String = "ACTIVE",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "membership_payments",
    foreignKeys = [
        ForeignKey(
            entity = MembershipSubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("subscriptionId"),
        Index("paidAt")
    ]
)
data class MembershipPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subscriptionId: Long,
    val amount: Double,
    val paidAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class MembershipSummary(
    val subscriptionId: Long,
    val memberId: Long,
    val memberName: String,
    val phone: String,
    val planId: Long,
    val planName: String,
    val startDate: Long,
    val endDate: Long,
    val totalFee: Double,
    val totalPaid: Double,
    val dueAmount: Double,
    val status: String
)
