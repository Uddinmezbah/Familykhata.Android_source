package com.familykhata.app.booking

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "booking_customers",
    indices = [
        Index(value = ["workspace", "name"])
    ]
)
data class BookingCustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val identityReference: String = "",
    val note: String = "",
    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = BookingCustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("customerId"),
        Index("workspace"),
        Index("status"),
        Index("startAt")
    ]
)
data class BookingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long? = null,

    val bookingType: String = "TRAVEL",
    val serviceName: String,

    val referenceNo: String = "",
    val vehicleOrCarrier: String = "",

    val fromLocation: String = "",
    val toLocation: String = "",

    val startAt: Long = System.currentTimeMillis(),
    val endAt: Long? = null,

    val quantity: Int = 1,

    val status: String = "BOOKED",
    val note: String = "",

    val workspace: String = "SHOP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "booking_charges",
    foreignKeys = [
        ForeignKey(
            entity = BookingEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookingId"),
        Index("createdAt")
    ]
)
data class BookingChargeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookingId: Long,

    val chargeType: String = "BASE",
    val amount: Double,

    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "booking_payments",
    foreignKeys = [
        ForeignKey(
            entity = BookingEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookingId"),
        Index("paidAt")
    ]
)
data class BookingPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookingId: Long,

    val amount: Double,
    val paidAt: Long = System.currentTimeMillis(),

    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class BookingSummary(
    val bookingId: Long,

    val customerId: Long?,
    val customerName: String?,
    val phone: String?,

    val bookingType: String,
    val serviceName: String,

    val referenceNo: String,
    val vehicleOrCarrier: String,

    val fromLocation: String,
    val toLocation: String,

    val startAt: Long,
    val endAt: Long?,

    val quantity: Int,
    val status: String,

    val totalCharge: Double,
    val totalPaid: Double,
    val dueAmount: Double
)
