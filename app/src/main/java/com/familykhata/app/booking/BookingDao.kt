package com.familykhata.app.booking

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(
        item: BookingCustomerEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(
        item: BookingEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharge(
        item: BookingChargeEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(
        item: BookingPaymentEntity
    ): Long

    @Query("""
        SELECT *
        FROM booking_customers
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeCustomers(
        workspace: String
    ): Flow<List<BookingCustomerEntity>>

    @Query("""
        SELECT
            b.id AS bookingId,

            b.customerId AS customerId,
            c.name AS customerName,
            c.phone AS phone,

            b.bookingType AS bookingType,
            b.serviceName AS serviceName,

            b.referenceNo AS referenceNo,
            b.vehicleOrCarrier AS vehicleOrCarrier,

            b.fromLocation AS fromLocation,
            b.toLocation AS toLocation,

            b.startAt AS startAt,
            b.endAt AS endAt,

            b.quantity AS quantity,
            b.status AS status,

            COALESCE(
                (
                    SELECT SUM(bc.amount)
                    FROM booking_charges bc
                    WHERE bc.bookingId = b.id
                ),
                0
            ) AS totalCharge,

            COALESCE(
                (
                    SELECT SUM(bp.amount)
                    FROM booking_payments bp
                    WHERE bp.bookingId = b.id
                ),
                0
            ) AS totalPaid,

            COALESCE(
                (
                    SELECT SUM(bc.amount)
                    FROM booking_charges bc
                    WHERE bc.bookingId = b.id
                ),
                0
            )
            -
            COALESCE(
                (
                    SELECT SUM(bp.amount)
                    FROM booking_payments bp
                    WHERE bp.bookingId = b.id
                ),
                0
            ) AS dueAmount

        FROM bookings b

        LEFT JOIN booking_customers c
            ON c.id = b.customerId

        WHERE b.workspace = :workspace

        ORDER BY
            CASE
                WHEN b.status = 'BOOKED' THEN 0
                WHEN b.status = 'ONGOING' THEN 1
                WHEN b.status = 'COMPLETED' THEN 2
                ELSE 3
            END,
            b.startAt ASC
    """)
    fun observeBookingSummaries(
        workspace: String
    ): Flow<List<BookingSummary>>

    @Query("""
        SELECT *
        FROM booking_charges
        WHERE bookingId = :bookingId
        ORDER BY createdAt DESC
    """)
    fun observeCharges(
        bookingId: Long
    ): Flow<List<BookingChargeEntity>>

    @Query("""
        SELECT *
        FROM booking_payments
        WHERE bookingId = :bookingId
        ORDER BY paidAt DESC
    """)
    fun observePayments(
        bookingId: Long
    ): Flow<List<BookingPaymentEntity>>

    @Query("""
        UPDATE bookings
        SET status = :status
        WHERE id = :bookingId
    """)
    suspend fun updateStatus(
        bookingId: Long,
        status: String
    )

    @Delete
    suspend fun deleteCharge(
        item: BookingChargeEntity
    )

    @Delete
    suspend fun deletePayment(
        item: BookingPaymentEntity
    )

    @Query("SELECT * FROM booking_customers ORDER BY id")
    suspend fun getAllCustomers():
        List<BookingCustomerEntity>

    @Query("SELECT * FROM bookings ORDER BY id")
    suspend fun getAllBookings():
        List<BookingEntity>

    @Query("SELECT * FROM booking_charges ORDER BY id")
    suspend fun getAllCharges():
        List<BookingChargeEntity>

    @Query("SELECT * FROM booking_payments ORDER BY id")
    suspend fun getAllPayments():
        List<BookingPaymentEntity>
}
