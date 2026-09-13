package com.familykhata.app.servicejob

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
class ServiceJobViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao =
        ServiceJobDatabase.get(application).dao()

    private val workspace =
        MutableStateFlow("SHOP")

    val customers: StateFlow<List<ServiceCustomerEntity>> =
        workspace
            .flatMapLatest {
                dao.observeCustomers(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val jobs: StateFlow<List<ServiceJobSummary>> =
        workspace
            .flatMapLatest {
                dao.observeJobSummaries(it)
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
        note: String,
        workspace: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            dao.insertCustomer(
                ServiceCustomerEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    note = note.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun addJob(
        customerId: Long?,
        title: String,
        serviceType: String,
        itemName: String,
        serialOrReference: String,
        initialCharge: Double,
        advance: Double,
        note: String,
        workspace: String
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val jobId =
                dao.insertJob(
                    ServiceJobEntity(
                        customerId = customerId,
                        title = title.trim(),
                        serviceType = serviceType.trim(),
                        itemName = itemName.trim(),
                        serialOrReference =
                            serialOrReference.trim(),
                        note = note.trim(),
                        workspace = workspace
                    )
                )

            if (
                jobId > 0 &&
                initialCharge > 0
            ) {
                dao.insertCharge(
                    ServiceChargeEntity(
                        jobId = jobId,
                        chargeType = "SERVICE",
                        amount =
                            initialCharge.coerceAtLeast(0.0),
                        note = "Initial service charge"
                    )
                )
            }

            if (
                jobId > 0 &&
                initialCharge > 0 &&
                advance > 0
            ) {
                dao.insertPayment(
                    ServicePaymentEntity(
                        jobId = jobId,
                        amount =
                            advance.coerceIn(
                                0.0,
                                initialCharge
                            ),
                        note = "Advance"
                    )
                )
            }
        }
    }

    fun addCharge(
        jobId: Long,
        amount: Double,
        chargeType: String,
        note: String
    ) {
        if (
            jobId <= 0 ||
            amount <= 0
        ) return

        viewModelScope.launch {
            dao.insertCharge(
                ServiceChargeEntity(
                    jobId = jobId,
                    amount = amount,
                    chargeType = chargeType,
                    note = note.trim()
                )
            )
        }
    }

    fun addPayment(
        jobId: Long,
        amount: Double,
        note: String
    ) {
        if (
            jobId <= 0 ||
            amount <= 0
        ) return

        viewModelScope.launch {
            dao.insertPayment(
                ServicePaymentEntity(
                    jobId = jobId,
                    amount = amount,
                    note = note.trim()
                )
            )
        }
    }

    fun setStatus(
        jobId: Long,
        status: String
    ) {
        if (jobId <= 0) return

        viewModelScope.launch {
            dao.updateJobStatus(
                jobId = jobId,
                status = status
            )
        }
    }

    fun observeCharges(
        jobId: Long
    ): Flow<List<ServiceChargeEntity>> =
        dao.observeCharges(jobId)

    fun observePayments(
        jobId: Long
    ): Flow<List<ServicePaymentEntity>> =
        dao.observePayments(jobId)
}
