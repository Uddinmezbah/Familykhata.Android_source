package com.familykhata.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familykhata.app.data.AppDatabase
import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonEntity
import com.familykhata.app.data.BakiPersonSummary
import com.familykhata.app.data.DashboardTotals
import com.familykhata.app.data.TransactionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyKhataViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).dao()
    private val preferences = application.getSharedPreferences(
        "hisabi_khata_preferences",
        Context.MODE_PRIVATE
    )

    private val allowedWorkspaces = setOf("PERSONAL", "FAMILY", "SHOP")
    private val _selectedWorkspace = MutableStateFlow(
        preferences.getString("selected_workspace", "FAMILY")
            ?.takeIf { it in allowedWorkspaces }
            ?: "FAMILY"
    )

    val selectedWorkspace: StateFlow<String> = _selectedWorkspace.asStateFlow()

    val transactions: StateFlow<List<TransactionEntity>> = _selectedWorkspace
        .flatMapLatest { workspace -> dao.observeTransactions(workspace) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<DashboardTotals> = _selectedWorkspace
        .flatMapLatest { workspace -> dao.observeDashboardTotals(workspace) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardTotals(0.0, 0.0)
        )

    val bakiPeople: StateFlow<List<BakiPersonSummary>> = _selectedWorkspace
        .flatMapLatest { workspace -> dao.observeBakiSummaries(workspace) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectWorkspace(workspace: String) {
        if (workspace !in allowedWorkspaces || workspace == _selectedWorkspace.value) return
        _selectedWorkspace.value = workspace
        preferences.edit().putString("selected_workspace", workspace).apply()
    }

    fun addTransaction(type: String, amount: Double, category: String, note: String) {
        if (amount <= 0) return
        val workspace = _selectedWorkspace.value
        viewModelScope.launch {
            dao.insertTransaction(
                TransactionEntity(
                    type = type,
                    amount = amount,
                    category = category.trim().ifBlank { "অন্যান্য" },
                    note = note.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        viewModelScope.launch { dao.deleteTransaction(item) }
    }

    fun addBakiPerson(name: String, phone: String) {
        if (name.isBlank()) return
        val workspace = _selectedWorkspace.value
        viewModelScope.launch {
            dao.insertPerson(
                BakiPersonEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun addBakiEntry(personId: Long, action: String, amount: Double, note: String) {
        if (amount <= 0) return
        val delta = when (action) {
            "GAVE" -> amount
            "RECEIVED_BACK" -> -amount
            "TOOK" -> -amount
            "PAID_BACK" -> amount
            else -> 0.0
        }
        viewModelScope.launch {
            dao.insertBakiEntry(
                BakiEntryEntity(
                    personId = personId,
                    action = action,
                    amount = amount,
                    balanceDelta = delta,
                    note = note.trim()
                )
            )
        }
    }

    fun deleteBakiEntry(item: BakiEntryEntity) {
        viewModelScope.launch { dao.deleteBakiEntry(item) }
    }

    fun observeBakiEntries(personId: Long): Flow<List<BakiEntryEntity>> =
        dao.observeBakiEntries(personId)
}
