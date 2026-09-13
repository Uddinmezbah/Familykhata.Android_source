package com.familykhata.app.booking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao =
        BookingDatabase.get(application).dao()

    private val workspace =
        MutableStateFlow("SHOP")

    val customers: StateFlow<List<BookingCustomerEntity>> =
        workspace
            .flatMapLatest {
                dao.observeCustomers(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val bookings: StateFlow<List<BookingSummary>> =
        workspace
            .flatMapLatest {
                dao.observeBookingSummaries(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun setWorkspace(value: String) {
        if (workspace.value != value) {
            workspace.value = value
        }
    }

    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        identityReference: String,
        note: String,
        workspace: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            dao.insertCustomer(
                BookingCustomerEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    identityReference =
                        identityReference.trim(),
                    note = note.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun addBooking(
        customerId: Long?,
        bookingType: String,
        serviceName: String,
        referenceNo: String,
        vehicleOrCarrier: String,
        fromLocation: String,
        toLocation: String,
        startAt: Long,
        endAt: Long?,
        quantity: Int,
        baseCharge: Double,
        advance: Double,
        note: String,
        workspace: String
    ) {
        if (serviceName.isBlank()) return

        viewModelScope.launch {
            val bookingId =
                dao.insertBooking(
                    BookingEntity(
                        customerId = customerId,
                        bookingType = bookingType,
                        serviceName = serviceName.trim(),
                        referenceNo = referenceNo.trim(),
                        vehicleOrCarrier =
                            vehicleOrCarrier.trim(),
                        fromLocation =
                            fromLocation.trim(),
                        toLocation =
                            toLocation.trim(),
                        startAt = startAt,
                        endAt = endAt,
                        quantity =
                            quantity.coerceAtLeast(1),
                        status = "BOOKED",
                        note = note.trim(),
                        workspace = workspace
                    )
                )

            if (
                bookingId > 0 &&
                baseCharge > 0
            ) {
                dao.insertCharge(
                    BookingChargeEntity(
                        bookingId = bookingId,
                        chargeType = "BASE",
                        amount =
                            baseCharge.coerceAtLeast(0.0),
                        note = "Base charge"
                    )
                )
            }

            if (
                bookingId > 0 &&
                baseCharge > 0 &&
                advance > 0
            ) {
                dao.insertPayment(
                    BookingPaymentEntity(
                        bookingId = bookingId,
                        amount =
                            advance.coerceAtMost(
                                baseCharge
                            ),
                        note = "Advance"
                    )
                )
            }
        }
    }

    fun addCharge(
        bookingId: Long,
        amount: Double,
        chargeType: String,
        note: String
    ) {
        if (
            bookingId <= 0 ||
            amount <= 0
        ) return

        viewModelScope.launch {
            dao.insertCharge(
                BookingChargeEntity(
                    bookingId = bookingId,
                    chargeType = chargeType,
                    amount = amount,
                    note = note.trim()
                )
            )
        }
    }

    fun addPayment(
        bookingId: Long,
        amount: Double,
        note: String
    ) {
        if (
            bookingId <= 0 ||
            amount <= 0
        ) return

        viewModelScope.launch {
            dao.insertPayment(
                BookingPaymentEntity(
                    bookingId = bookingId,
                    amount = amount,
                    note = note.trim()
                )
            )
        }
    }

    fun setStatus(
        bookingId: Long,
        status: String
    ) {
        if (bookingId <= 0) return

        viewModelScope.launch {
            dao.updateStatus(
                bookingId = bookingId,
                status = status
            )
        }
    }

    fun observeCharges(
        bookingId: Long
    ): Flow<List<BookingChargeEntity>> =
        dao.observeCharges(bookingId)

    fun observePayments(
        bookingId: Long
    ): Flow<List<BookingPaymentEntity>> =
        dao.observePayments(bookingId)
}
