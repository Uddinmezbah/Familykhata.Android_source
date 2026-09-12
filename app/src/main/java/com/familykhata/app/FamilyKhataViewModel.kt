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
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TRIAL_DAYS = 30
private const val DAY_MS = 86_400_000L

data class TrialStatus(
    val trialDays: Int,
    val daysRemaining: Int,
    val expired: Boolean,
    val premiumUnlocked: Boolean,
    val startedAt: Long,
    val expiresAt: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyKhataViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.get(application)
    private val dao = database.dao()
    private val preferences = application.getSharedPreferences(
        "hisabi_khata_preferences",
        Context.MODE_PRIVATE
    )

    private val trialStartedAt: Long = preferences.getLong("trial_started_at", 0L).let { saved ->
        if (saved > 0L) saved else System.currentTimeMillis().also { started ->
            preferences.edit().putLong("trial_started_at", started).apply()
        }
    }
    private val _trialStatus = MutableStateFlow(calculateTrialStatus())
    val trialStatus: StateFlow<TrialStatus> = _trialStatus.asStateFlow()

    private val _isPinConfigured = MutableStateFlow(
        !preferences.getString("pin_hash", null).isNullOrBlank() &&
            !preferences.getString("pin_salt", null).isNullOrBlank()
    )
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(!_isPinConfigured.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

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
        if (!canWriteNow() || amount <= 0) return
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
        if (!canWriteNow() || name.isBlank()) return
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

    fun updateBakiPerson(personId: Long, name: String, phone: String) {
        val cleanName = name.trim()
        if (!canWriteNow() || cleanName.isBlank()) return
        viewModelScope.launch {
            dao.updatePerson(personId, cleanName, phone.trim())
        }
    }

    fun deleteBakiPerson(personId: Long) {
        viewModelScope.launch { dao.deletePersonById(personId) }
    }

    fun addBakiEntry(personId: Long, action: String, amount: Double, note: String) {
        if (!canWriteNow() || amount <= 0) return
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



    fun refreshTrialStatus() {
        _trialStatus.value = calculateTrialStatus()
    }

    private fun calculateTrialStatus(now: Long = System.currentTimeMillis()): TrialStatus {
        val premiumUnlocked = preferences.getBoolean("premium_unlocked", false)
        val expiresAt = trialStartedAt + TRIAL_DAYS * DAY_MS
        val remainingMillis = (expiresAt - now).coerceAtLeast(0L)
        val daysRemaining = if (premiumUnlocked) {
            TRIAL_DAYS
        } else if (remainingMillis == 0L) {
            0
        } else {
            ((remainingMillis + DAY_MS - 1L) / DAY_MS).toInt()
        }
        return TrialStatus(
            trialDays = TRIAL_DAYS,
            daysRemaining = daysRemaining,
            expired = !premiumUnlocked && now >= expiresAt,
            premiumUnlocked = premiumUnlocked,
            startedAt = trialStartedAt,
            expiresAt = expiresAt
        )
    }

    private fun canWriteNow(): Boolean {
        val current = calculateTrialStatus()
        _trialStatus.value = current
        return !current.expired
    }

    fun setPin(pin: String): Boolean {
        if (!pin.matches(Regex("^\\d{4,6}$"))) return false
        val salt = randomSalt()
        preferences.edit()
            .putString("pin_salt", salt)
            .putString("pin_hash", hashPin(pin, salt))
            .apply()
        _isPinConfigured.value = true
        _isAppUnlocked.value = true
        return true
    }

    fun verifyPin(pin: String): Boolean {
        if (!_isPinConfigured.value) {
            _isAppUnlocked.value = true
            return true
        }
        val ok = matchesStoredPin(pin)
        if (ok) _isAppUnlocked.value = true
        return ok
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!matchesStoredPin(currentPin) || !newPin.matches(Regex("^\\d{4,6}$"))) return false
        val salt = randomSalt()
        preferences.edit()
            .putString("pin_salt", salt)
            .putString("pin_hash", hashPin(newPin, salt))
            .apply()
        _isPinConfigured.value = true
        _isAppUnlocked.value = true
        return true
    }

    fun disablePin(currentPin: String): Boolean {
        if (!matchesStoredPin(currentPin)) return false
        preferences.edit()
            .remove("pin_salt")
            .remove("pin_hash")
            .apply()
        _isPinConfigured.value = false
        _isAppUnlocked.value = true
        return true
    }

    fun lockApp() {
        if (_isPinConfigured.value) _isAppUnlocked.value = false
    }

    private fun matchesStoredPin(pin: String): Boolean {
        val salt = preferences.getString("pin_salt", null) ?: return false
        val storedHash = preferences.getString("pin_hash", null) ?: return false
        return hashPin(pin, salt) == storedHash
    }

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    fun createCsvReport(
        period: String,
        onReady: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                require(period in setOf("MONTH", "YEAR", "ALL")) { "রিপোর্ট সময়সীমা সঠিক নয়" }
                val now = System.currentTimeMillis()
                val startAt = reportStart(period, now)
                val workspace = _selectedWorkspace.value

                val people = dao.getAllPeople().filter { it.workspace == workspace }
                val personMap = people.associateBy { it.id }
                val personIds = personMap.keys
                val rows = mutableListOf<Pair<Long, List<String>>>()

                dao.getAllTransactions()
                    .asSequence()
                    .filter { it.workspace == workspace && it.createdAt in startAt..now }
                    .forEach { item ->
                        rows += item.createdAt to listOf(
                            formatReportDate(item.createdAt),
                            "আয়-খরচ",
                            item.category,
                            if (item.type == "INCOME") "আয়" else "খরচ",
                            item.amount.toString(),
                            item.note
                        )
                    }

                dao.getAllBakiEntries()
                    .asSequence()
                    .filter { it.personId in personIds && it.createdAt in startAt..now }
                    .forEach { entry ->
                        rows += entry.createdAt to listOf(
                            formatReportDate(entry.createdAt),
                            "বাকি/পাওনা",
                            personMap[entry.personId]?.name.orEmpty(),
                            reportActionLabel(entry.action),
                            entry.amount.toString(),
                            entry.note
                        )
                    }

                buildString {
                    append('\uFEFF')
                    appendLine("তারিখ,ধরন,ব্যক্তি/ক্যাটাগরি,লেনদেন,টাকা,নোট")
                    rows.sortedByDescending { it.first }.forEach { (_, columns) ->
                        appendLine(columns.joinToString(",") { csvEscape(it) })
                    }
                }
            }.onSuccess(onReady).onFailure {
                onError(it.message ?: "রিপোর্ট তৈরি করা যায়নি")
            }
        }
    }

    private fun reportStart(period: String, now: Long): Long {
        if (period == "ALL") return 0L
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (period == "MONTH") {
                set(Calendar.DAY_OF_MONTH, 1)
            } else {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis
    }

    private fun csvEscape(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    private fun formatReportDate(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timestamp))

    private fun reportActionLabel(action: String): String = when (action) {
        "GAVE" -> "দিলাম"
        "RECEIVED_BACK" -> "ফেরত পেলাম"
        "TOOK" -> "নিলাম"
        "PAID_BACK" -> "ফেরত দিলাম"
        else -> action
    }

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
