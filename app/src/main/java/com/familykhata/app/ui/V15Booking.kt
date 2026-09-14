package com.familykhata.app.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.booking.BookingChargeEntity
import com.familykhata.app.booking.BookingCustomerEntity
import com.familykhata.app.booking.BookingPaymentEntity
import com.familykhata.app.booking.BookingSummary
import com.familykhata.app.booking.BookingViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun V15BookingScreen(
    workspace: String,
    shopType: String,
    canWrite: Boolean,
    onExit: () -> Unit
) {
    val vm: BookingViewModel =
        viewModel()

    val customers by
        vm.customers.collectAsState()

    val bookings by
        vm.bookings.collectAsState()

    var selectedId by remember {
        mutableStateOf<Long?>(null)
    }

    var showCustomerDialog by remember {
        mutableStateOf(false)
    }

    var showBookingDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(workspace) {
        vm.setWorkspace(workspace)
    }

    val selected =
        selectedId?.let { id ->
            bookings.firstOrNull {
                it.bookingId == id
            }
        }

    TrackV15DeepScreen(
        owner = "booking-detail",
        active = selected != null
    )

    BackHandler(
        enabled = selectedId != null
    ) {
        selectedId = null
    }

    BackHandler(
        enabled = selectedId == null
    ) {
        onExit()
    }

    if (selected != null) {
        V15DeepScreenContainer(
            title = v15Text(
                "বুকিং বিস্তারিত",
                "Booking details"
            ),
            onBack = {
                selectedId = null
            }
        ) {
            BookingLedger(
                item = selected,
                shopType = shopType,
                viewModel = vm,
                canWrite = canWrite,
                onBack = {
                    selectedId = null
                }
            )
        }
        return
    }

    val activeCount =
        bookings.count {
            it.status == "BOOKED" ||
            it.status == "ONGOING"
        }

    val totalPaid =
        bookings.sumOf {
            it.totalPaid
        }

    val totalDue =
        bookings.sumOf {
            it.dueAmount.coerceAtLeast(0.0)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Text(
            bookingBusinessTitle(
                shopType
            ),
            style =
                MaterialTheme.typography
                    .headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            bookingBusinessSubtitle(
                shopType
            )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            BookingMetric(
                title =
                    v15Text(
                        "বুকিং",
                        "Bookings"
                    ),
                value =
                    bookings.size.toString(),
                modifier =
                    Modifier.weight(1f)
            )

            BookingMetric(
                title =
                    v15Text(
                        "চলমান",
                        "Active"
                    ),
                value =
                    activeCount.toString(),
                modifier =
                    Modifier.weight(1f)
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            BookingMetric(
                title =
                    v15Text(
                        "আদায়",
                        "Paid"
                    ),
                value =
                    bookingMoney(totalPaid),
                modifier =
                    Modifier.weight(1f)
            )

            BookingMetric(
                title =
                    v15Text(
                        "বকেয়া",
                        "Due"
                    ),
                value =
                    bookingMoney(totalDue),
                modifier =
                    Modifier.weight(1f)
            )
        }

        if (canWrite) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Button(
                    onClick = {
                        showBookingDialog = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ বুকিং",
                            "+ Booking"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        showCustomerDialog = true
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "+ কাস্টমার",
                            "+ Customer"
                        )
                    )
                }
            }
        }

        Text(
            v15Text(
                "বুকিং তালিকা",
                "Booking list"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (bookings.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো বুকিং যোগ করা হয়নি।",
                    "No bookings added yet."
                )
            )
        } else {
            bookings.forEach { item ->
                BookingCard(
                    item = item,
                    shopType = shopType,
                    onClick = {
                        selectedId =
                            item.bookingId
                    }
                )
            }
        }
    }

    if (showCustomerDialog) {
        AddBookingCustomerDialog(
            onDismiss = {
                showCustomerDialog = false
            }
        ) {
                name,
                phone,
                address,
                identity,
                note ->

            vm.addCustomer(
                name = name,
                phone = phone,
                address = address,
                identityReference = identity,
                note = note,
                workspace = workspace
            )

            showCustomerDialog = false
        }
    }

    if (showBookingDialog) {
        AddBookingDialog(
            customers = customers,
            shopType = shopType,
            onDismiss = {
                showBookingDialog = false
            }
        ) {
                customerId,
                serviceName,
                reference,
                carrier,
                from,
                to,
                start,
                end,
                quantity,
                charge,
                advance,
                note ->

            vm.addBooking(
                customerId = customerId,
                bookingType =
                    bookingTypeValue(shopType),
                serviceName = serviceName,
                referenceNo = reference,
                vehicleOrCarrier = carrier,
                fromLocation = from,
                toLocation = to,
                startAt = start,
                endAt = end,
                quantity = quantity,
                baseCharge = charge,
                advance = advance,
                note = note,
                workspace = workspace
            )

            showBookingDialog = false
        }
    }
}

@Composable
private fun BookingMetric(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography
                        .labelSmall
            )

            Text(
                value,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BookingCard(
    item: BookingSummary,
    shopType: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
    ) {
        Column(
            modifier =
                Modifier.padding(13.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    item.serviceName,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    bookingStatusLabel(
                        item.status
                    )
                )
            }

            Text(
                if (
                    item.customerName
                        .isNullOrBlank()
                ) {
                    v15Text(
                        "কাস্টমার: ওয়াক-ইন",
                        "Customer: Walk-in"
                    )
                } else {
                    v15Text(
                        "কাস্টমার: ${item.customerName}",
                        "Customer: ${item.customerName}"
                    )
                }
            )

            if (
                item.fromLocation.isNotBlank() ||
                item.toLocation.isNotBlank()
            ) {
                Text(
                    bookingRouteText(
                        item,
                        shopType
                    )
                )
            }

            Text(
                v15Text(
                    "তারিখ: ${bookingDate(item.startAt)}",
                    "Date: ${bookingDate(item.startAt)}"
                )
            )

            Text(
                v15Text(
                    "মোট: ${bookingMoney(item.totalCharge)}",
                    "Total: ${bookingMoney(item.totalCharge)}"
                )
            )

            Text(
                v15Text(
                    "বকেয়া: ${bookingMoney(item.dueAmount)}",
                    "Due: ${bookingMoney(item.dueAmount)}"
                ),
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AddBookingCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var phone by remember {
        mutableStateOf("")
    }

    var address by remember {
        mutableStateOf("")
    }

    var identity by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                v15Text(
                    "নতুন কাস্টমার",
                    "New customer"
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                BookingField(
                    name,
                    { name = it },
                    v15Text(
                        "নাম",
                        "Name"
                    )
                )

                BookingField(
                    phone,
                    { phone = it },
                    v15Text(
                        "ফোন",
                        "Phone"
                    )
                )

                BookingField(
                    address,
                    { address = it },
                    v15Text(
                        "ঠিকানা",
                        "Address"
                    )
                )

                BookingField(
                    identity,
                    { identity = it },
                    v15Text(
                        "NID / পাসপোর্ট / রেফারেন্স",
                        "NID / Passport / Reference"
                    )
                )

                BookingField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.isNotBlank(),
                onClick = {
                    onSave(
                        name,
                        phone,
                        address,
                        identity,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "সেভ",
                        "Save"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun AddBookingDialog(
    customers:
        List<BookingCustomerEntity>,
    shopType: String,
    onDismiss: () -> Unit,
    onSave: (
        Long?,
        String,
        String,
        String,
        String,
        String,
        Long,
        Long?,
        Int,
        Double,
        Double,
        String
    ) -> Unit
) {
    val context =
        LocalContext.current

    var customerId by remember {
        mutableStateOf<Long?>(null)
    }

    var serviceName by remember {
        mutableStateOf("")
    }

    var reference by remember {
        mutableStateOf("")
    }

    var carrier by remember {
        mutableStateOf("")
    }

    var from by remember {
        mutableStateOf("")
    }

    var to by remember {
        mutableStateOf("")
    }

    var startAt by remember {
        mutableStateOf(
            System.currentTimeMillis()
        )
    }

    var endAt by remember {
        mutableStateOf<Long?>(null)
    }

    var quantity by remember {
        mutableStateOf("1")
    }

    var charge by remember {
        mutableStateOf("")
    }

    var advance by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    val rental =
        isCarRental(shopType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (rental)
                    v15Text(
                        "নতুন গাড়ি বুকিং",
                        "New rental booking"
                    )
                else
                    v15Text(
                        "নতুন ট্রাভেল / টিকেট বুকিং",
                        "New travel / ticket booking"
                    )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    v15Text(
                        "কাস্টমার",
                        "Customer"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        customerId = null
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (customerId == null)
                            v15Text(
                                "✓ ওয়াক-ইন",
                                "✓ Walk-in"
                            )
                        else
                            v15Text(
                                "ওয়াক-ইন",
                                "Walk-in"
                            )
                    )
                }

                customers.forEach { customer ->
                    OutlinedButton(
                        onClick = {
                            customerId =
                                customer.id
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                customerId ==
                                customer.id
                            ) {
                                "✓ ${customer.name}"
                            } else {
                                customer.name
                            }
                        )
                    }
                }

                BookingField(
                    serviceName,
                    { serviceName = it },
                    if (rental)
                        v15Text(
                            "রেন্টাল / প্যাকেজ",
                            "Rental / package"
                        )
                    else
                        v15Text(
                            "টিকেট / সার্ভিস",
                            "Ticket / service"
                        )
                )

                BookingField(
                    reference,
                    { reference = it },
                    v15Text(
                        "বুকিং / টিকেট রেফারেন্স",
                        "Booking / ticket reference"
                    )
                )

                BookingField(
                    carrier,
                    { carrier = it },
                    if (rental)
                        v15Text(
                            "গাড়ি / নম্বর প্লেট",
                            "Vehicle / plate"
                        )
                    else
                        v15Text(
                            "এয়ারলাইন / বাস / ট্রেন",
                            "Airline / Bus / Train"
                        )
                )

                BookingField(
                    from,
                    { from = it },
                    if (rental)
                        v15Text(
                            "পিকআপ লোকেশন",
                            "Pickup location"
                        )
                    else
                        v15Text(
                            "কোথা থেকে",
                            "From"
                        )
                )

                BookingField(
                    to,
                    { to = it },
                    if (rental)
                        v15Text(
                            "ড্রপ লোকেশন",
                            "Drop location"
                        )
                    else
                        v15Text(
                            "গন্তব্য",
                            "Destination"
                        )
                )

                OutlinedButton(
                    onClick = {
                        showBookingDatePicker(
                            context = context,
                            initial = startAt
                        ) {
                            startAt = it
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "শুরুর তারিখ: ${bookingDate(startAt)}",
                            "Start date: ${bookingDate(startAt)}"
                        )
                    )
                }

                if (rental) {
                    OutlinedButton(
                        onClick = {
                            showBookingDatePicker(
                                context = context,
                                initial =
                                    endAt
                                        ?: startAt
                            ) {
                                endAt = it
                            }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            v15Text(
                                "শেষের তারিখ: ${
                                    endAt?.let {
                                        bookingDate(it)
                                    } ?: "—"
                                }",
                                "End date: ${
                                    endAt?.let {
                                        bookingDate(it)
                                    } ?: "—"
                                }"
                            )
                        )
                    }
                }

                BookingField(
                    quantity,
                    { quantity = it },
                    if (rental)
                        v15Text(
                            "গাড়ির সংখ্যা",
                            "Vehicle quantity"
                        )
                    else
                        v15Text(
                            "টিকেট / যাত্রী সংখ্যা",
                            "Tickets / passengers"
                        )
                )

                BookingField(
                    charge,
                    { charge = it },
                    if (rental)
                        v15Text(
                            "মোট ভাড়া",
                            "Rental charge"
                        )
                    else
                        v15Text(
                            "ভাড়া / টিকেট মূল্য",
                            "Fare / ticket price"
                        )
                )

                BookingField(
                    advance,
                    { advance = it },
                    v15Text(
                        "অগ্রিম",
                        "Advance"
                    )
                )

                BookingField(
                    note,
                    { note = it },
                    v15Text(
                        "নোট",
                        "Note"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    serviceName.isNotBlank(),
                onClick = {
                    onSave(
                        customerId,
                        serviceName,
                        reference,
                        carrier,
                        from,
                        to,
                        startAt,
                        endAt,
                        quantity.toIntOrNull()
                            ?: 1,
                        charge.toDoubleOrNull()
                            ?: 0.0,
                        advance.toDoubleOrNull()
                            ?: 0.0,
                        note
                    )
                }
            ) {
                Text(
                    v15Text(
                        "বুকিং সেভ",
                        "Save booking"
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    v15Text(
                        "বাতিল",
                        "Cancel"
                    )
                )
            }
        }
    )
}

@Composable
private fun BookingLedger(
    item: BookingSummary,
    shopType: String,
    viewModel: BookingViewModel,
    canWrite: Boolean,
    onBack: () -> Unit
) {
    val charges by
        viewModel.observeCharges(
            item.bookingId
        ).collectAsState(
            initial = emptyList()
        )

    val payments by
        viewModel.observePayments(
            item.bookingId
        ).collectAsState(
            initial = emptyList()
        )

    var chargeAmount by remember {
        mutableStateOf("")
    }

    var chargeNote by remember {
        mutableStateOf("")
    }

    var paymentAmount by remember {
        mutableStateOf("")
    }

    var paymentNote by remember {
        mutableStateOf("")
    }

    val totalCharge =
        charges.sumOf {
            it.amount
        }

    val totalPaid =
        payments.sumOf {
            it.amount
        }

    val due =
        (
            totalCharge -
            totalPaid
        ).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text(
                v15Text(
                    "← বুকিং তালিকা",
                    "← Booking list"
                )
            )
        }

        Text(
            item.serviceName,
            style =
                MaterialTheme.typography
                    .headlineSmall,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            if (
                item.customerName
                    .isNullOrBlank()
            ) {
                v15Text(
                    "কাস্টমার: ওয়াক-ইন",
                    "Customer: Walk-in"
                )
            } else {
                v15Text(
                    "কাস্টমার: ${item.customerName}",
                    "Customer: ${item.customerName}"
                )
            }
        )

        if (
            item.referenceNo.isNotBlank()
        ) {
            Text(
                v15Text(
                    "রেফারেন্স: ${item.referenceNo}",
                    "Reference: ${item.referenceNo}"
                )
            )
        }

        if (
            item.vehicleOrCarrier
                .isNotBlank()
        ) {
            Text(
                if (isCarRental(shopType))
                    v15Text(
                        "গাড়ি: ${item.vehicleOrCarrier}",
                        "Vehicle: ${item.vehicleOrCarrier}"
                    )
                else
                    v15Text(
                        "ক্যারিয়ার: ${item.vehicleOrCarrier}",
                        "Carrier: ${item.vehicleOrCarrier}"
                    )
            )
        }

        if (
            item.fromLocation.isNotBlank() ||
            item.toLocation.isNotBlank()
        ) {
            Text(
                bookingRouteText(
                    item,
                    shopType
                )
            )
        }

        Text(
            v15Text(
                "শুরু: ${bookingDate(item.startAt)}",
                "Start: ${bookingDate(item.startAt)}"
            )
        )

        item.endAt?.let {
            Text(
                v15Text(
                    "শেষ: ${bookingDate(it)}",
                    "End: ${bookingDate(it)}"
                )
            )
        }

        Text(
            v15Text(
                "মোট চার্জ: ${bookingMoney(totalCharge)}",
                "Total charge: ${bookingMoney(totalCharge)}"
            )
        )

        Text(
            v15Text(
                "পরিশোধ: ${bookingMoney(totalPaid)}",
                "Paid: ${bookingMoney(totalPaid)}"
            )
        )

        Text(
            v15Text(
                "বকেয়া: ${bookingMoney(due)}",
                "Due: ${bookingMoney(due)}"
            ),
            fontWeight =
                FontWeight.Bold
        )

        Text(
            v15Text(
                "অবস্থা: ${bookingStatusLabel(item.status)}",
                "Status: ${bookingStatusLabel(item.status)}"
            )
        )

        if (canWrite) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.setStatus(
                            item.bookingId,
                            "BOOKED"
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "বুকড",
                            "Booked"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.setStatus(
                            item.bookingId,
                            "ONGOING"
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "চলমান",
                            "Ongoing"
                        )
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.setStatus(
                            item.bookingId,
                            "COMPLETED"
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "সম্পন্ন",
                            "Completed"
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.setStatus(
                            item.bookingId,
                            "CANCELLED"
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        v15Text(
                            "বাতিল",
                            "Cancelled"
                        )
                    )
                }
            }

            Text(
                v15Text(
                    "অতিরিক্ত চার্জ",
                    "Extra charge"
                ),
                fontWeight =
                    FontWeight.Bold
            )

            BookingField(
                chargeAmount,
                { chargeAmount = it },
                v15Text(
                    "পরিমাণ",
                    "Amount"
                )
            )

            BookingField(
                chargeNote,
                { chargeNote = it },
                v15Text(
                    "চার্জ নোট",
                    "Charge note"
                )
            )

            Button(
                onClick = {
                    val amount =
                        chargeAmount
                            .toDoubleOrNull()
                            ?: 0.0

                    if (amount > 0) {
                        viewModel.addCharge(
                            bookingId =
                                item.bookingId,
                            amount = amount,
                            chargeType = "EXTRA",
                            note = chargeNote
                        )

                        chargeAmount = ""
                        chargeNote = ""
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    v15Text(
                        "চার্জ যোগ করুন",
                        "Add charge"
                    )
                )
            }

            if (due > 0) {
                Text(
                    v15Text(
                        "পেমেন্ট",
                        "Payment"
                    ),
                    fontWeight =
                        FontWeight.Bold
                )

                BookingField(
                    paymentAmount,
                    { paymentAmount = it },
                    v15Text(
                        "পেমেন্টের পরিমাণ",
                        "Payment amount"
                    )
                )

                BookingField(
                    paymentNote,
                    { paymentNote = it },
                    v15Text(
                        "পেমেন্ট নোট",
                        "Payment note"
                    )
                )

                Button(
                    onClick = {
                        val requested =
                            paymentAmount
                                .toDoubleOrNull()
                                ?: 0.0

                        if (requested > 0) {
                            viewModel.addPayment(
                                bookingId =
                                    item.bookingId,
                                amount =
                                    requested.coerceAtMost(
                                        due
                                    ),
                                note =
                                    paymentNote
                            )

                            paymentAmount = ""
                            paymentNote = ""
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        v15Text(
                            "পেমেন্ট যোগ করুন",
                            "Add payment"
                        )
                    )
                }
            }
        }

        Text(
            v15Text(
                "চার্জের ইতিহাস",
                "Charge history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        charges.forEach {
            BookingChargeCard(it)
        }

        Text(
            v15Text(
                "পেমেন্ট ইতিহাস",
                "Payment history"
            ),
            fontWeight =
                FontWeight.Bold
        )

        if (payments.isEmpty()) {
            Text(
                v15Text(
                    "এখনো কোনো পেমেন্ট নেই।",
                    "No payments yet."
                )
            )
        }

        payments.forEach {
            BookingPaymentCard(it)
        }
    }
}

@Composable
private fun BookingChargeCard(
    item: BookingChargeEntity
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    if (
                        item.chargeType ==
                        "BASE"
                    )
                        v15Text(
                            "মূল চার্জ",
                            "Base charge"
                        )
                    else
                        v15Text(
                            "অতিরিক্ত",
                            "Extra"
                        )
                )

                Text(
                    bookingMoney(
                        item.amount
                    ),
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (item.note.isNotBlank()) {
                Text(
                    item.note,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
private fun BookingPaymentCard(
    item: BookingPaymentEntity
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    bookingDate(
                        item.paidAt
                    )
                )

                Text(
                    bookingMoney(
                        item.amount
                    ),
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (item.note.isNotBlank()) {
                Text(
                    item.note,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
private fun BookingField(
    value: String,
    onChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = {
            Text(label)
        },
        modifier =
            Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun bookingBusinessTitle(
    shopType: String
): String =
    if (isCarRental(shopType))
        v15Text(
            "কার রেন্টাল",
            "Car Rental"
        )
    else
        v15Text(
            "ট্রাভেল ও টিকেট",
            "Travel & Ticket"
        )

private fun bookingBusinessSubtitle(
    shopType: String
): String =
    if (isCarRental(shopType))
        v15Text(
            "গাড়ি বুকিং, ভাড়া, অগ্রিম, পেমেন্ট ও বকেয়া",
            "Vehicle bookings, rent, advance, payments and dues"
        )
    else
        v15Text(
            "কাস্টমার, টিকেট, রুট, পেমেন্ট ও বকেয়া",
            "Customers, tickets, routes, payments and dues"
        )

private fun isCarRental(
    shopType: String
): Boolean =
    shopType.contains(
        "car rental",
        ignoreCase = true
    ) ||
    shopType.contains(
        "কার রেন্টাল"
    )

private fun bookingTypeValue(
    shopType: String
): String =
    if (isCarRental(shopType))
        "CAR_RENTAL"
    else
        "TRAVEL"

private fun bookingStatusLabel(
    status: String
): String =
    when (status) {
        "ONGOING" ->
            v15Text(
                "চলমান",
                "Ongoing"
            )

        "COMPLETED" ->
            v15Text(
                "সম্পন্ন",
                "Completed"
            )

        "CANCELLED" ->
            v15Text(
                "বাতিল",
                "Cancelled"
            )

        else ->
            v15Text(
                "বুকড",
                "Booked"
            )
    }

private fun bookingRouteText(
    item: BookingSummary,
    shopType: String
): String =
    if (isCarRental(shopType)) {
        v15Text(
            "পিকআপ: ${item.fromLocation} → ড্রপ: ${item.toLocation}",
            "Pickup: ${item.fromLocation} → Drop: ${item.toLocation}"
        )
    } else {
        "${item.fromLocation} → ${item.toLocation}"
    }

private fun bookingMoney(
    value: Double
): String =
    String.format(
        Locale.US,
        "%.2f",
        value
    )

private fun bookingDate(
    value: Long
): String =
    SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(value)
    )

private fun showBookingDatePicker(
    context: android.content.Context,
    initial: Long,
    onDate: (Long) -> Unit
) {
    val calendar =
        Calendar.getInstance().apply {
            timeInMillis = initial
        }

    DatePickerDialog(
        context,
        {
                _,
                year,
                month,
                day ->

            val selected =
                Calendar.getInstance().apply {
                    set(
                        year,
                        month,
                        day,
                        0,
                        0,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

            onDate(
                selected.timeInMillis
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
