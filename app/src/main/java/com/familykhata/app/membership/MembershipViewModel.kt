package com.familykhata.app.membership

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
class MembershipViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao =
        MembershipDatabase.get(application).dao()

    private val workspace =
        MutableStateFlow("SHOP")

    val members: StateFlow<List<MembershipMemberEntity>> =
        workspace
            .flatMapLatest {
                dao.observeMembers(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val plans: StateFlow<List<MembershipPlanEntity>> =
        workspace
            .flatMapLatest {
                dao.observePlans(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val memberships: StateFlow<List<MembershipSummary>> =
        workspace
            .flatMapLatest {
                dao.observeMembershipSummaries(it)
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

        refreshExpiry()
    }

    fun refreshExpiry() {
        viewModelScope.launch {
            dao.expireOldSubscriptions(
                System.currentTimeMillis()
            )
        }
    }

    fun addMember(
        name: String,
        phone: String,
        address: String,
        note: String,
        workspace: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            dao.insertMember(
                MembershipMemberEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    note = note.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun addPlan(
        name: String,
        durationDays: Int,
        fee: Double,
        note: String,
        workspace: String
    ) {
        if (
            name.isBlank() ||
            durationDays <= 0 ||
            fee < 0
        ) return

        viewModelScope.launch {
            dao.insertPlan(
                MembershipPlanEntity(
                    name = name.trim(),
                    durationDays = durationDays,
                    fee = fee,
                    note = note.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun startMembership(
        memberId: Long,
        plan: MembershipPlanEntity,
        advance: Double,
        note: String = ""
    ) {
        if (
            memberId <= 0 ||
            plan.id <= 0 ||
            plan.durationDays <= 0
        ) return

        viewModelScope.launch {
            val start =
                System.currentTimeMillis()

            val duration =
                plan.durationDays.toLong() *
                    86_400_000L

            val subscriptionId =
                dao.insertSubscription(
                    MembershipSubscriptionEntity(
                        memberId = memberId,
                        planId = plan.id,
                        startDate = start,
                        endDate = start + duration,
                        totalFee =
                            plan.fee.coerceAtLeast(0.0),
                        status = "ACTIVE",
                        note = note.trim()
                    )
                )

            if (
                subscriptionId > 0 &&
                advance > 0
            ) {
                dao.insertPayment(
                    MembershipPaymentEntity(
                        subscriptionId =
                            subscriptionId,
                        amount =
                            advance.coerceAtMost(
                                plan.fee
                            ),
                        note = "Advance"
                    )
                )
            }
        }
    }

    fun renewMembership(
        memberId: Long,
        plan: MembershipPlanEntity,
        previousEndDate: Long
    ) {
        if (
            memberId <= 0 ||
            plan.id <= 0 ||
            plan.durationDays <= 0
        ) return

        viewModelScope.launch {
            val now =
                System.currentTimeMillis()

            val start =
                maxOf(
                    now,
                    previousEndDate
                )

            val duration =
                plan.durationDays.toLong() *
                    86_400_000L

            dao.insertSubscription(
                MembershipSubscriptionEntity(
                    memberId = memberId,
                    planId = plan.id,
                    startDate = start,
                    endDate = start + duration,
                    totalFee =
                        plan.fee.coerceAtLeast(0.0),
                    status = "ACTIVE",
                    note = "Renewal"
                )
            )
        }
    }

    fun addPayment(
        subscriptionId: Long,
        amount: Double,
        note: String
    ) {
        if (
            subscriptionId <= 0 ||
            amount <= 0
        ) return

        viewModelScope.launch {
            dao.insertPayment(
                MembershipPaymentEntity(
                    subscriptionId =
                        subscriptionId,
                    amount = amount,
                    note = note.trim()
                )
            )
        }
    }

    fun setStatus(
        subscriptionId: Long,
        status: String
    ) {
        if (subscriptionId <= 0) return

        viewModelScope.launch {
            dao.updateSubscriptionStatus(
                subscriptionId =
                    subscriptionId,
                status = status
            )
        }
    }

    fun observePayments(
        subscriptionId: Long
    ): Flow<List<MembershipPaymentEntity>> =
        dao.observePayments(subscriptionId)
}
