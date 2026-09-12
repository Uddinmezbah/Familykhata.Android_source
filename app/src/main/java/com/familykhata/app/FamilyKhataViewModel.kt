package com.familykhata.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
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
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyKhataViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.get(application)
    private val dao = database.dao()
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


    fun createBackup(
        onReady: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val transactions = dao.getAllTransactions()
                val people = dao.getAllPeople()
                val entries = dao.getAllBakiEntries()

                JSONObject().apply {
                    put("format", "hisabi-khata-backup")
                    put("version", 1)
                    put("createdAt", System.currentTimeMillis())
                    put("transactions", JSONArray().apply {
                        transactions.forEach { item ->
                            put(JSONObject().apply {
                                put("id", item.id)
                                put("type", item.type)
                                put("amount", item.amount)
                                put("category", item.category)
                                put("note", item.note)
                                put("workspace", item.workspace)
                                put("createdAt", item.createdAt)
                            })
                        }
                    })
                    put("people", JSONArray().apply {
                        people.forEach { person ->
                            put(JSONObject().apply {
                                put("id", person.id)
                                put("name", person.name)
                                put("phone", person.phone)
                                put("note", person.note)
                                put("workspace", person.workspace)
                                put("createdAt", person.createdAt)
                            })
                        }
                    })
                    put("entries", JSONArray().apply {
                        entries.forEach { entry ->
                            put(JSONObject().apply {
                                put("id", entry.id)
                                put("personId", entry.personId)
                                put("action", entry.action)
                                put("amount", entry.amount)
                                put("balanceDelta", entry.balanceDelta)
                                put("note", entry.note)
                                put("createdAt", entry.createdAt)
                            })
                        }
                    })
                }.toString()
            }.onSuccess(onReady).onFailure {
                onError(it.message ?: "ব্যাকআপ তৈরি করা যায়নি")
            }
        }
    }

    fun restoreBackup(
        json: String,
        onDone: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val root = JSONObject(json)
                require(root.optString("format") == "hisabi-khata-backup") {
                    "এটি হিসাবী খাতার সঠিক ব্যাকআপ ফাইল নয়"
                }
                require(root.optInt("version") == 1) {
                    "এই ব্যাকআপ ভার্সনটি এখনো সমর্থিত নয়"
                }

                val transactions = mutableListOf<TransactionEntity>()
                val people = mutableListOf<BakiPersonEntity>()
                val entries = mutableListOf<BakiEntryEntity>()
                val actions = setOf("GAVE", "RECEIVED_BACK", "TOOK", "PAID_BACK")

                val transactionArray = root.getJSONArray("transactions")
                for (index in 0 until transactionArray.length()) {
                    val item = transactionArray.getJSONObject(index)
                    val type = item.getString("type")
                    val amount = item.getDouble("amount")
                    val workspace = item.getString("workspace")
                    require(type == "INCOME" || type == "EXPENSE") { "লেনদেনের ধরন সঠিক নয়" }
                    require(amount > 0) { "লেনদেনের টাকার পরিমাণ সঠিক নয়" }
                    require(workspace in allowedWorkspaces) { "ওয়ার্কস্পেস সঠিক নয়" }

                    transactions += TransactionEntity(
                        id = item.getLong("id"),
                        type = type,
                        amount = amount,
                        category = item.optString("category", "অন্যান্য"),
                        note = item.optString("note", ""),
                        workspace = workspace,
                        createdAt = item.optLong("createdAt", System.currentTimeMillis())
                    )
                }

                val peopleArray = root.getJSONArray("people")
                for (index in 0 until peopleArray.length()) {
                    val item = peopleArray.getJSONObject(index)
                    val workspace = item.getString("workspace")
                    val name = item.getString("name").trim()
                    require(name.isNotBlank()) { "খাতার নাম খালি হতে পারে না" }
                    require(workspace in allowedWorkspaces) { "ওয়ার্কস্পেস সঠিক নয়" }

                    people += BakiPersonEntity(
                        id = item.getLong("id"),
                        name = name,
                        phone = item.optString("phone", ""),
                        note = item.optString("note", ""),
                        workspace = workspace,
                        createdAt = item.optLong("createdAt", System.currentTimeMillis())
                    )
                }

                val validPersonIds = people.map { it.id }.toSet()
                val entriesArray = root.getJSONArray("entries")
                for (index in 0 until entriesArray.length()) {
                    val item = entriesArray.getJSONObject(index)
                    val personId = item.getLong("personId")
                    val action = item.getString("action")
                    val amount = item.getDouble("amount")
                    require(personId in validPersonIds) { "খাতার লেনদেনের ব্যক্তি পাওয়া যায়নি" }
                    require(action in actions) { "বাকি লেনদেনের ধরন সঠিক নয়" }
                    require(amount > 0) { "বাকি লেনদেনের টাকার পরিমাণ সঠিক নয়" }

                    val delta = when (action) {
                        "GAVE", "PAID_BACK" -> amount
                        "RECEIVED_BACK", "TOOK" -> -amount
                        else -> error("অজানা লেনদেন")
                    }

                    entries += BakiEntryEntity(
                        id = item.getLong("id"),
                        personId = personId,
                        action = action,
                        amount = amount,
                        balanceDelta = delta,
                        note = item.optString("note", ""),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis())
                    )
                }

                database.withTransaction {
                    dao.clearBakiEntries()
                    dao.clearPeople()
                    dao.clearTransactions()

                    transactions.forEach { dao.insertTransaction(it) }
                    people.forEach { dao.insertPerson(it) }
                    entries.forEach { dao.insertBakiEntry(it) }
                }

                transactions.size + people.size + entries.size
            }.onSuccess(onDone).onFailure {
                onError(it.message ?: "ব্যাকআপ রিস্টোর করা যায়নি")
            }
        }
    }

}
