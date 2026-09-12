package com.familykhata.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familykhata.app.data.AppDatabase
import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonEntity
import com.familykhata.app.data.BakiPersonSummary
import com.familykhata.app.data.DashboardTotals
import com.familykhata.app.data.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FamilyKhataViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).dao()

    val transactions: StateFlow<List<TransactionEntity>> = dao.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<DashboardTotals> = dao.observeDashboardTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardTotals(0.0, 0.0))

    val bakiPeople: StateFlow<List<BakiPersonSummary>> = dao.observeBakiSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTransaction(type: String, amount: Double, category: String, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            dao.insertTransaction(
                TransactionEntity(
                    type = type,
                    amount = amount,
                    category = category.trim().ifBlank { "অন্যান্য" },
                    note = note.trim()
                )
            )
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        viewModelScope.launch { dao.deleteTransaction(item) }
    }

    fun addBakiPerson(name: String, phone: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insertPerson(BakiPersonEntity(name = name.trim(), phone = phone.trim()))
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

    fun observeBakiEntries(personId: Long): Flow<List<BakiEntryEntity>> = dao.observeBakiEntries(personId)
}
