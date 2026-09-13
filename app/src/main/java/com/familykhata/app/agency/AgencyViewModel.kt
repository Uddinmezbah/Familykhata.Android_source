package com.familykhata.app.agency

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
class AgencyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao =
        AgencyDatabase.get(application).dao()

    private val workspace =
        MutableStateFlow("SHOP")

    val clients: StateFlow<List<AgencyClientEntity>> =
        workspace.flatMapLatest {
            dao.observeClients(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val projects: StateFlow<List<AgencyProjectSummary>> =
        workspace.flatMapLatest {
            dao.observeProjectSummaries(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun setWorkspace(value: String) {
        if (workspace.value != value) {
            workspace.value = value
        }
    }

    fun addClient(
        name: String,
        phone: String,
        email: String,
        company: String,
        note: String,
        workspace: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            dao.insertClient(
                AgencyClientEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    company = company.trim(),
                    note = note.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun addProject(
        clientId: Long,
        title: String,
        serviceType: String,
        totalPrice: Double,
        advance: Double,
        note: String
    ) {
        if (
            clientId <= 0 ||
            title.isBlank() ||
            totalPrice <= 0
        ) return

        viewModelScope.launch {
            val projectId =
                dao.insertProject(
                    AgencyProjectEntity(
                        clientId = clientId,
                        title = title.trim(),
                        serviceType =
                            serviceType.trim(),
                        totalPrice =
                            totalPrice.coerceAtLeast(0.0),
                        note = note.trim()
                    )
                )

            if (advance > 0 && projectId > 0) {
                dao.insertPayment(
                    AgencyPaymentEntity(
                        projectId = projectId,
                        amount = advance
                            .coerceAtMost(totalPrice),
                        note = "Advance"
                    )
                )
            }
        }
    }

    fun addPayment(
        projectId: Long,
        amount: Double,
        note: String = ""
    ) {
        if (
            projectId <= 0 ||
            amount <= 0
        ) return

        viewModelScope.launch {
            dao.insertPayment(
                AgencyPaymentEntity(
                    projectId = projectId,
                    amount = amount,
                    note = note.trim()
                )
            )
        }
    }

    fun setProjectStatus(
        projectId: Long,
        status: String
    ) {
        if (projectId <= 0) return

        viewModelScope.launch {
            dao.updateProjectStatus(
                projectId = projectId,
                status = status
            )
        }
    }

    fun observePayments(
        projectId: Long
    ): Flow<List<AgencyPaymentEntity>> =
        dao.observePayments(projectId)
}
