package com.familykhata.app.agency

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AgencyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        AgencyDatabase.get(application)

    private val dao =
        database.dao()

    private val workspace =
        MutableStateFlow("SHOP")

    private val refreshTick =
        MutableStateFlow(0L)

    private val liveWorkspace =
        combine(
            workspace,
            refreshTick
        ) { currentWorkspace, _ ->
            currentWorkspace
        }

    val clients: StateFlow<List<AgencyClientEntity>> =
        liveWorkspace.flatMapLatest {
            dao.observeClients(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val projects: StateFlow<List<AgencyProjectSummary>> =
        liveWorkspace.flatMapLatest {
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

    fun refresh() {
        refreshTick.value =
            refreshTick.value + 1L
    }

    fun updateClient(
        item: AgencyClientEntity,
        name: String,
        phone: String,
        email: String,
        company: String,
        note: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            dao.updateClient(
                item.copy(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    company = company.trim(),
                    note = note.trim()
                )
            )
        }
    }

    fun deleteClient(
        item: AgencyClientEntity
    ) {
        viewModelScope.launch {
            dao.deleteClient(item)
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
            database.withTransaction {

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

                if (projectId > 0) {
                    dao.insertCharge(
                        AgencyChargeEntity(
                            projectId = projectId,
                            chargeType = "PACKAGE",
                            periodKey = "INITIAL",
                            amount =
                                totalPrice.coerceAtLeast(0.0),
                            note = "Initial package"
                        )
                    )
                }

                if (
                    advance > 0 &&
                    projectId > 0
                ) {
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
    }

    fun updateProject(
        projectId: Long,
        clientId: Long,
        title: String,
        serviceType: String,
        basePrice: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            projectId <= 0 ||
            clientId <= 0 ||
            title.isBlank() ||
            basePrice <= 0
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val updated =
                            dao.updateProjectFields(
                                projectId = projectId,
                                clientId = clientId,
                                title = title.trim(),
                                serviceType =
                                    serviceType.trim(),
                                basePrice =
                                    basePrice.coerceAtLeast(0.0),
                                note = note.trim()
                            )

                        if (updated <= 0) {
                            error("Project not found")
                        }

                        val packageUpdated =
                            dao.updateInitialPackageCharge(
                                projectId = projectId,
                                amount =
                                    basePrice.coerceAtLeast(0.0)
                            )

                        if (packageUpdated <= 0) {
                            dao.insertCharge(
                                AgencyChargeEntity(
                                    projectId = projectId,
                                    chargeType = "PACKAGE",
                                    periodKey = "INITIAL",
                                    amount =
                                        basePrice.coerceAtLeast(0.0),
                                    note = ""
                                )
                            )
                        }
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun deleteProject(
        projectId: Long,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (projectId <= 0) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.deleteProjectById(
                        projectId
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun addCharge(
        projectId: Long,
        chargeType: String,
        periodKey: String,
        amount: Double,
        dueDate: Long?,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            projectId <= 0 ||
            amount <= 0
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {

            val type =
                chargeType.trim()
                    .uppercase()
                    .ifBlank { "OTHER" }

            val period =
                periodKey.trim()
                    .uppercase()

            val needsDuplicateGuard =
                type == "PACKAGE" ||
                type == "RECURRING"

            if (
                needsDuplicateGuard &&
                period.isBlank()
            ) {
                onDone(false)
                return@launch
            }

            if (needsDuplicateGuard) {
                val existing =
                    dao.countCharge(
                        projectId = projectId,
                        chargeType = type,
                        periodKey = period
                    )

                if (existing > 0) {
                    onDone(false)
                    return@launch
                }
            }

            val result =
                runCatching {
                    dao.insertCharge(
                        AgencyChargeEntity(
                            projectId = projectId,
                            chargeType = type,
                            periodKey = period,
                            amount =
                                amount.coerceAtLeast(0.0),
                            dueDate = dueDate,
                            note = note.trim()
                        )
                    )
                }.isSuccess

            onDone(result)
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

    fun observeCharges(
        projectId: Long
    ): Flow<List<AgencyChargeEntity>> =
        dao.observeCharges(projectId)

    fun observePayments(
        projectId: Long
    ): Flow<List<AgencyPaymentEntity>> =
        dao.observePayments(projectId)
}
