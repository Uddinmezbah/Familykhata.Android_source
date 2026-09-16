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
import com.familykhata.app.data.DueReceivableItem
import com.familykhata.app.data.FinancialAccountEntity
import com.familykhata.app.data.FinancialAccountEntryEntity
import com.familykhata.app.data.FinancialAccountSummary
import com.familykhata.app.data.InventoryBackupBridge
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.ProductUnitConversionEntity
import com.familykhata.app.data.StockBatchEntity
import com.familykhata.app.data.TransactionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
import java.util.UUID

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

    val financialAccounts:
        StateFlow<List<FinancialAccountSummary>> =
        _selectedWorkspace
            .flatMapLatest { workspace ->
                dao.observeFinancialAccounts(workspace)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val bakiPeople: StateFlow<List<BakiPersonSummary>> = _selectedWorkspace
        .flatMapLatest { workspace -> dao.observeBakiSummaries(workspace) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val workspacePeople = _selectedWorkspace
        .flatMapLatest { workspace -> dao.observePeople(workspace) }

    private val workspaceBakiEntries = _selectedWorkspace
        .flatMapLatest { workspace -> dao.observeWorkspaceBakiEntries(workspace) }

    val dueReceivables: StateFlow<List<DueReceivableItem>> = combine(
        workspacePeople,
        workspaceBakiEntries
    ) { people, entries ->
        calculateDueReceivables(people, entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun addFinancialAccount(
        name: String,
        type: String,
        provider: String = "",
        openingBalance: Double = 0.0,
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanName = name.trim()
        val cleanType =
            type.trim().uppercase(Locale.ROOT)

        val allowedTypes =
            setOf(
                "CASH",
                "MOBILE_WALLET",
                "BANK",
                "CARD",
                "OTHER"
            )

        if (
            !canWriteNow() ||
            cleanName.isBlank() ||
            cleanType !in allowedTypes ||
            !openingBalance.isFinite() ||
            openingBalance < 0.0
        ) {
            onDone(false)
            return
        }

        val workspace =
            _selectedWorkspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertFinancialAccount(
                        FinancialAccountEntity(
                            name = cleanName,
                            type = cleanType,
                            provider = provider.trim(),
                            openingBalance =
                                openingBalance,
                            workspace = workspace
                        )
                    ) > 0L
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            !canWriteNow() ||
            fromAccountId <= 0L ||
            toAccountId <= 0L ||
            fromAccountId == toAccountId ||
            !amount.isFinite() ||
            amount <= 0.0
        ) {
            onDone(false)
            return
        }

        val workspace =
            _selectedWorkspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val from =
                            requireNotNull(
                                dao.getFinancialAccountOnce(
                                    fromAccountId
                                )
                            )

                        val to =
                            requireNotNull(
                                dao.getFinancialAccountOnce(
                                    toAccountId
                                )
                            )

                        require(
                            from.workspace == workspace &&
                                to.workspace == workspace
                        )

                        require(
                            from.isActive &&
                                to.isActive
                        )

                        val available =
                            requireNotNull(
                                dao.getFinancialAccountBalanceOnce(
                                    from.id
                                )
                            )

                        require(
                            available + 0.0001 >=
                                amount
                        )

                        val groupId =
                            UUID.randomUUID()
                                .toString()

                        val now =
                            System.currentTimeMillis()

                        dao.insertFinancialAccountEntry(
                            FinancialAccountEntryEntity(
                                accountId = from.id,
                                entryType = "TRANSFER_OUT",
                                amount = amount,
                                balanceDelta = -amount,
                                relatedAccountId = to.id,
                                transferGroupId = groupId,
                                sourceKey =
                                    "ACCOUNT_TRANSFER:" +
                                        "$groupId:OUT",
                                note = note.trim(),
                                workspace = workspace,
                                createdAt = now
                            )
                        )

                        dao.insertFinancialAccountEntry(
                            FinancialAccountEntryEntity(
                                accountId = to.id,
                                entryType = "TRANSFER_IN",
                                amount = amount,
                                balanceDelta = amount,
                                relatedAccountId = from.id,
                                transferGroupId = groupId,
                                sourceKey =
                                    "ACCOUNT_TRANSFER:" +
                                        "$groupId:IN",
                                note = note.trim(),
                                workspace = workspace,
                                createdAt = now
                            )
                        )
                    }

                    true
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun observeFinancialAccountEntries(
        accountId: Long
    ): Flow<List<FinancialAccountEntryEntity>> =
        dao.observeFinancialAccountEntries(
            accountId
        )

    fun deleteTransaction(item: TransactionEntity) {
        if (!canWriteNow()) return
        viewModelScope.launch { dao.deleteTransaction(item) }
    }

    fun updateTransaction(
        item: TransactionEntity,
        type: String,
        amount: Double,
        category: String,
        note: String
    ) {
        val cleanType = type.trim().uppercase(Locale.US)

        if (
            !canWriteNow() ||
            cleanType !in setOf("INCOME", "EXPENSE") ||
            amount <= 0
        ) {
            return
        }

        viewModelScope.launch {
            dao.updateTransaction(
                transactionId = item.id,
                type = cleanType,
                amount = amount,
                category =
                    category.trim().ifBlank {
                        "অন্যান্য"
                    },
                note = note.trim()
            )
        }
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
        if (!canWriteNow()) return
        viewModelScope.launch { dao.deletePersonById(personId) }
    }

    fun addBakiEntry(
        personId: Long,
        action: String,
        amount: Double,
        note: String,
        dueAt: Long? = null
    ) {
        if (!canWriteNow() || amount <= 0) return
        val delta = balanceDelta(action, amount)
        if (delta == null) return
        viewModelScope.launch {
            dao.insertBakiEntry(
                BakiEntryEntity(
                    personId = personId,
                    action = action,
                    amount = amount,
                    balanceDelta = delta,
                    note = note.trim(),
                    dueAt = dueAt
                )
            )
        }
    }

    fun updateBakiEntry(
        item: BakiEntryEntity,
        action: String,
        amount: Double,
        note: String,
        dueAt: Long?
    ) {
        if (
            item.sourceKey
                ?.startsWith(
                    "RETAIL_SALE_"
                ) == true
        ) {
            return
        }

        if (!canWriteNow() || amount <= 0) return

        val delta =
            balanceDelta(action, amount)
                ?: return

        viewModelScope.launch {
            dao.updateBakiEntry(
                entryId = item.id,
                action = action,
                amount = amount,
                balanceDelta = delta,
                note = note.trim(),
                dueAt = dueAt
            )
        }
    }

    private fun balanceDelta(action: String, amount: Double): Double? = when (action) {
        "GAVE" -> amount
        "RECEIVED_BACK" -> -amount
        "TOOK" -> -amount
        "PAID_BACK" -> amount
        else -> null
    }

    fun deleteBakiEntry(item: BakiEntryEntity) {
        if (
            !canWriteNow() ||
            item.sourceKey
                ?.startsWith(
                    "RETAIL_SALE_"
                ) == true
        ) {
            return
        }

        viewModelScope.launch {
            dao.deleteBakiEntry(item)
        }
    }

    fun observeBakiEntries(personId: Long): Flow<List<BakiEntryEntity>> =
        dao.observeBakiEntries(personId)

    suspend fun loadLedgerStatement(
        personId: Long,
        workspace: String,
        startInclusive: Long,
        endExclusive: Long
    ): com.familykhata.app.report.LedgerStatement =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            require(workspace in allowedWorkspaces)
            // One database snapshot prevents mixing a person's details and edited ledger entries.
            database.withTransaction {
                val person = requireNotNull(dao.getStatementPerson(personId, workspace)) {
                    "Ledger no longer exists in this workspace"
                }
                com.familykhata.app.report.buildLedgerStatement(
                    person, dao.getStatementEntries(personId), startInclusive, endExclusive
                )
            }
        }



    private data class ReceivableLot(
        val entry: BakiEntryEntity,
        var remaining: Double
    )

    private fun calculateDueReceivables(
        people: List<BakiPersonEntity>,
        entries: List<BakiEntryEntity>
    ): List<DueReceivableItem> {
        val entriesByPerson = entries.groupBy { it.personId }
        return people.flatMap { person ->
            val lots = mutableListOf<ReceivableLot>()
            entriesByPerson[person.id].orEmpty()
                .sortedWith(compareBy<BakiEntryEntity> { it.createdAt }.thenBy { it.id })
                .forEach { entry ->
                    when (entry.action) {
                        "GAVE" -> lots += ReceivableLot(entry, entry.amount)
                        "RECEIVED_BACK" -> {
                            var paymentLeft = entry.amount
                            for (lot in lots) {
                                if (paymentLeft <= 0.0) break
                                if (lot.remaining <= 0.0) continue
                                val applied = minOf(lot.remaining, paymentLeft)
                                lot.remaining -= applied
                                paymentLeft -= applied
                            }
                        }
                    }
                }
            lots.asSequence()
                .filter { it.remaining > 0.0001 && it.entry.dueAt != null }
                .map { lot ->
                    DueReceivableItem(
                        entryId = lot.entry.id,
                        personId = person.id,
                        personName = person.name,
                        phone = person.phone,
                        originalAmount = lot.entry.amount,
                        remainingAmount = lot.remaining,
                        dueAt = lot.entry.dueAt!!,
                        note = lot.entry.note
                    )
                }
                .toList()
        }.sortedWith(compareBy<DueReceivableItem> { it.dueAt }.thenBy { it.personName })
    }

    fun refreshTrialStatus() {
        _trialStatus.value = calculateTrialStatus()
    }

    private fun calculateTrialStatus(now: Long = System.currentTimeMillis()): TrialStatus {
        val premiumUnlocked =
            preferences.getBoolean(
                PremiumBillingManager.KEY_PLAY_PREMIUM_UNLOCKED,
                false
            )
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
        return true
    }

    fun verifyPin(pin: String): Boolean {
        if (!_isPinConfigured.value) return false
        return matchesStoredPin(pin)
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!matchesStoredPin(currentPin) || !newPin.matches(Regex("^\\d{4,6}$"))) return false
        val salt = randomSalt()
        preferences.edit()
            .putString("pin_salt", salt)
            .putString("pin_hash", hashPin(newPin, salt))
            .apply()
        _isPinConfigured.value = true
        return true
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
                val financialAccounts =
                    dao.getAllFinancialAccounts()
                val financialAccountEntries =
                    dao.getAllFinancialAccountEntries()
                val inventory =
                    InventoryBackupBridge.export(
                        getApplication()
                    )
                val businessData =
                    V15BusinessBackupBridge.export(
                        getApplication()
                    )

                JSONObject().apply {
                    put("format", "hisabi-khata-backup")
                    put("version", 9)
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
                                if (entry.dueAt != null) {
                                    put("dueAt", entry.dueAt)
                                }
                                if (entry.sourceKey != null) {
                                    put("sourceKey", entry.sourceKey)
                                }
                                put("createdAt", entry.createdAt)
                            })
                        }
                    })
                    put(
                        "financialAccounts",
                        JSONArray().apply {
                            financialAccounts.forEach { account ->
                                put(
                                    JSONObject().apply {
                                        put("id", account.id)
                                        put("name", account.name)
                                        put("type", account.type)
                                        put(
                                            "provider",
                                            account.provider
                                        )
                                        put(
                                            "openingBalance",
                                            account.openingBalance
                                        )
                                        put(
                                            "workspace",
                                            account.workspace
                                        )
                                        put(
                                            "isActive",
                                            account.isActive
                                        )
                                        put(
                                            "createdAt",
                                            account.createdAt
                                        )
                                    }
                                )
                            }
                        }
                    )

                    put(
                        "financialAccountEntries",
                        JSONArray().apply {
                            financialAccountEntries.forEach { entry ->
                                put(
                                    JSONObject().apply {
                                        put("id", entry.id)
                                        put(
                                            "accountId",
                                            entry.accountId
                                        )
                                        put(
                                            "entryType",
                                            entry.entryType
                                        )
                                        put(
                                            "amount",
                                            entry.amount
                                        )
                                        put(
                                            "balanceDelta",
                                            entry.balanceDelta
                                        )

                                        entry.relatedAccountId?.let {
                                            put(
                                                "relatedAccountId",
                                                it
                                            )
                                        }

                                        entry.transferGroupId?.let {
                                            put(
                                                "transferGroupId",
                                                it
                                            )
                                        }

                                        entry.sourceKey?.let {
                                            put(
                                                "sourceKey",
                                                it
                                            )
                                        }

                                        put(
                                            "note",
                                            entry.note
                                        )
                                        put(
                                            "workspace",
                                            entry.workspace
                                        )
                                        put(
                                            "createdAt",
                                            entry.createdAt
                                        )
                                    }
                                )
                            }
                        }
                    )

                    put(
                        "settings",
                        V15SettingsBackupBridge.export(
                            getApplication(),
                            _selectedWorkspace.value
                        )
                    )

                    put(
                        "businessData",
                        businessData
                    )

                    put("inventoryProducts", JSONArray().apply {
                        inventory.products.forEach { product ->
                            put(JSONObject().apply {
                                put("id", product.id)
                                put("name", product.name)
                                put("category", product.category)
                                put("sku", product.sku)
                                put("unit", product.unit)
                                put("brand", product.brand)
                                put("genericName", product.genericName)
                                put("modelName", product.modelName)
                                put("serialOrImei", product.serialOrImei)
                                put("size", product.size)
                                put("color", product.color)
                                put("warrantyMonths", product.warrantyMonths)
                                put("sellingPrice", product.sellingPrice)
                                put("mrp", product.mrp)
                                put("rackLocation", product.rackLocation)
                                put("lowStockLevel", product.lowStockLevel)
                                put("note", product.note)
                                put("workspace", product.workspace)
                                put("businessKey", product.businessKey)
                                put("createdAt", product.createdAt)
                            })
                        }
                    })
                    put(
                        "inventoryUnits",
                        JSONArray().apply {
                            inventory.unitConversions
                                .forEach { unit ->
                                    put(
                                        JSONObject().apply {
                                            put(
                                                "id",
                                                unit.id
                                            )
                                            put(
                                                "productId",
                                                unit.productId
                                            )
                                            put(
                                                "unitName",
                                                unit.unitName
                                            )
                                            put(
                                                "baseQuantity",
                                                unit.baseQuantity
                                            )
                                            put(
                                                "sortOrder",
                                                unit.sortOrder
                                            )
                                            put(
                                                "createdAt",
                                                unit.createdAt
                                            )
                                        }
                                    )
                                }
                        }
                    )

                    put("inventoryBatches", JSONArray().apply {
                        inventory.batches.forEach { batch ->
                            put(JSONObject().apply {
                                put("id", batch.id)
                                put("productId", batch.productId)
                                put("batchNo", batch.batchNo)
                                put("quantity", batch.quantity)
                                put("purchasePrice", batch.purchasePrice)
                                put("purchaseDate", batch.purchaseDate)
                                if (batch.expiryDate != null) put("expiryDate", batch.expiryDate)
                                put("createdAt", batch.createdAt)
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
                val backupVersion = root.optInt("version")
                require(backupVersion in 1..9) {
                    "এই ব্যাকআপ ভার্সনটি এখনো সমর্থিত নয়"
                }

                val transactions = mutableListOf<TransactionEntity>()
                val people = mutableListOf<BakiPersonEntity>()
                val entries =
                    mutableListOf<BakiEntryEntity>()

                val financialAccounts =
                    mutableListOf<
                        FinancialAccountEntity
                    >()

                val financialAccountEntries =
                    mutableListOf<
                        FinancialAccountEntryEntity
                    >()

                val inventoryProducts =
                    mutableListOf<ProductEntity>()
                val inventoryBatches = mutableListOf<StockBatchEntity>()
                val inventoryUnits =
                    mutableListOf<ProductUnitConversionEntity>()
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
                        dueAt =
                            item.optLong(
                                "dueAt",
                                0L
                            ).takeIf {
                                it > 0L
                            },
                        sourceKey =
                            if (
                                backupVersion >= 6
                            ) {
                                item.optString(
                                    "sourceKey",
                                    ""
                                ).trim().takeIf {
                                    it.isNotEmpty()
                                }
                            } else {
                                null
                            },
                        createdAt =
                            item.optLong(
                                "createdAt",
                                System.currentTimeMillis()
                            )
                    )
                }

                val restoredSourceKeys =
                    entries.mapNotNull {
                        it.sourceKey
                    }

                require(
                    restoredSourceKeys.size ==
                        restoredSourceKeys.toSet().size
                ) {
                    "ব্যাকআপে একই sourceKey একাধিকবার আছে"
                }

                if (backupVersion >= 9) {
                    val accountTypes =
                        setOf(
                            "CASH",
                            "MOBILE_WALLET",
                            "BANK",
                            "CARD",
                            "OTHER"
                        )

                    val accountArray =
                        root.optJSONArray(
                            "financialAccounts"
                        ) ?: JSONArray()

                    val seenAccountIds =
                        mutableSetOf<Long>()

                    for (
                        index in
                        0 until accountArray.length()
                    ) {
                        val item =
                            accountArray
                                .getJSONObject(index)

                        val id =
                            item.getLong("id")

                        val name =
                            item.getString("name")
                                .trim()

                        val type =
                            item.getString("type")
                                .trim()
                                .uppercase(
                                    Locale.ROOT
                                )

                        val openingBalance =
                            item.getDouble(
                                "openingBalance"
                            )

                        val workspace =
                            item.getString(
                                "workspace"
                            )

                        require(
                            id > 0L &&
                                id !in seenAccountIds
                        ) {
                            "অ্যাকাউন্ট ID সঠিক নয়"
                        }

                        require(
                            name.isNotBlank()
                        ) {
                            "অ্যাকাউন্টের নাম খালি হতে পারে না"
                        }

                        require(
                            type in accountTypes
                        ) {
                            "অ্যাকাউন্টের ধরন সঠিক নয়"
                        }

                        require(
                            openingBalance.isFinite() &&
                                openingBalance >= 0.0
                        ) {
                            "Opening balance সঠিক নয়"
                        }

                        require(
                            workspace in
                                allowedWorkspaces
                        ) {
                            "অ্যাকাউন্ট workspace সঠিক নয়"
                        }

                        seenAccountIds += id

                        financialAccounts +=
                            FinancialAccountEntity(
                                id = id,
                                name = name,
                                type = type,
                                provider =
                                    item.optString(
                                        "provider",
                                        ""
                                    ).trim(),
                                openingBalance =
                                    openingBalance,
                                workspace =
                                    workspace,
                                isActive =
                                    item.optBoolean(
                                        "isActive",
                                        true
                                    ),
                                createdAt =
                                    item.optLong(
                                        "createdAt",
                                        System.currentTimeMillis()
                                    )
                            )
                    }

                    val accountById =
                        financialAccounts
                            .associateBy {
                                it.id
                            }

                    val entryArray =
                        root.optJSONArray(
                            "financialAccountEntries"
                        ) ?: JSONArray()

                    val seenEntryIds =
                        mutableSetOf<Long>()

                    val seenSourceKeys =
                        mutableSetOf<String>()

                    for (
                        index in
                        0 until entryArray.length()
                    ) {
                        val item =
                            entryArray
                                .getJSONObject(index)

                        val id =
                            item.getLong("id")

                        val accountId =
                            item.getLong(
                                "accountId"
                            )

                        val account =
                            requireNotNull(
                                accountById[
                                    accountId
                                ]
                            ) {
                                "Account entry-এর account পাওয়া যায়নি"
                            }

                        val entryType =
                            item.getString(
                                "entryType"
                            )
                                .trim()
                                .uppercase(
                                    Locale.ROOT
                                )

                        val amount =
                            item.getDouble(
                                "amount"
                            )

                        val workspace =
                            item.getString(
                                "workspace"
                            )

                        require(
                            id > 0L &&
                                id !in seenEntryIds
                        ) {
                            "Account entry ID সঠিক নয়"
                        }

                        require(
                            entryType ==
                                "TRANSFER_IN" ||
                                entryType ==
                                "TRANSFER_OUT"
                        ) {
                            "Account entry type সঠিক নয়"
                        }

                        require(
                            amount.isFinite() &&
                                amount > 0.0
                        ) {
                            "Account entry amount সঠিক নয়"
                        }

                        require(
                            workspace ==
                                account.workspace
                        ) {
                            "Account entry workspace সঠিক নয়"
                        }

                        val relatedAccountId =
                            item.optLong(
                                "relatedAccountId",
                                0L
                            ).takeIf {
                                it > 0L
                            }

                        val related =
                            requireNotNull(
                                relatedAccountId
                                    ?.let {
                                        accountById[it]
                                    }
                            ) {
                                "Transfer-এর অন্য account পাওয়া যায়নি"
                            }

                        require(
                            related.id !=
                                account.id &&
                                related.workspace ==
                                    workspace
                        ) {
                            "Transfer account সঠিক নয়"
                        }

                        val groupId =
                            item.optString(
                                "transferGroupId",
                                ""
                            ).trim()

                        require(
                            groupId.isNotBlank()
                        ) {
                            "Transfer group পাওয়া যায়নি"
                        }

                        val sourceKey =
                            item.optString(
                                "sourceKey",
                                ""
                            )
                                .trim()
                                .takeIf {
                                    it.isNotEmpty()
                                }

                        if (sourceKey != null) {
                            require(
                                sourceKey !in
                                    seenSourceKeys
                            ) {
                                "একই account sourceKey একাধিকবার আছে"
                            }

                            seenSourceKeys +=
                                sourceKey
                        }

                        seenEntryIds += id

                        financialAccountEntries +=
                            FinancialAccountEntryEntity(
                                id = id,
                                accountId =
                                    account.id,
                                entryType =
                                    entryType,
                                amount =
                                    amount,
                                balanceDelta =
                                    if (
                                        entryType ==
                                            "TRANSFER_IN"
                                    ) {
                                        amount
                                    } else {
                                        -amount
                                    },
                                relatedAccountId =
                                    related.id,
                                transferGroupId =
                                    groupId,
                                sourceKey =
                                    sourceKey,
                                note =
                                    item.optString(
                                        "note",
                                        ""
                                    ),
                                workspace =
                                    workspace,
                                createdAt =
                                    item.optLong(
                                        "createdAt",
                                        System.currentTimeMillis()
                                    )
                            )
                    }

                    financialAccountEntries
                        .groupBy {
                            it.transferGroupId
                        }
                        .forEach {
                                (_, pair) ->

                            require(
                                pair.size == 2
                            ) {
                                "Transfer pair অসম্পূর্ণ"
                            }

                            val outgoing =
                                pair.singleOrNull {
                                    it.entryType ==
                                        "TRANSFER_OUT"
                                }

                            val incoming =
                                pair.singleOrNull {
                                    it.entryType ==
                                        "TRANSFER_IN"
                                }

                            require(
                                outgoing != null &&
                                    incoming != null &&
                                    kotlin.math.abs(
                                        outgoing.amount -
                                            incoming.amount
                                    ) < 0.0001 &&
                                    outgoing.accountId ==
                                        incoming.relatedAccountId &&
                                    incoming.accountId ==
                                        outgoing.relatedAccountId &&
                                    outgoing.workspace ==
                                        incoming.workspace
                            ) {
                                "Transfer pair সঠিক নয়"
                            }
                        }
                }

                if (backupVersion >= 3) {
                    val productArray = root.optJSONArray("inventoryProducts") ?: JSONArray()
                    for (index in 0 until productArray.length()) {
                        val item = productArray.getJSONObject(index)
                        val name = item.getString("name").trim()
                        val workspace = item.optString("workspace", "SHOP")
                        require(name.isNotBlank()) { "পণ্যের নাম খালি হতে পারে না" }
                        require(workspace in allowedWorkspaces) { "পণ্যের workspace সঠিক নয়" }
                        inventoryProducts += ProductEntity(
                            id = item.getLong("id"),
                            name = name,
                            category = item.optString("category", ""),
                            sku = item.optString("sku", ""),
                            unit = item.optString("unit", "pcs").ifBlank { "pcs" },
                            brand = item.optString("brand", ""),
                            genericName = item.optString("genericName", ""),
                            modelName = item.optString("modelName", ""),
                            serialOrImei = item.optString("serialOrImei", ""),
                            size = item.optString("size", ""),
                            color = item.optString("color", ""),
                            warrantyMonths = item.optInt("warrantyMonths", 0).coerceAtLeast(0),
                            sellingPrice = item.optDouble("sellingPrice", 0.0).coerceAtLeast(0.0),
                            mrp = item.optDouble("mrp", 0.0).coerceAtLeast(0.0),
                            rackLocation = item.optString("rackLocation", ""),
                            lowStockLevel = item.optInt("lowStockLevel", 0).coerceAtLeast(0),
                            note = item.optString("note", ""),
                            workspace = workspace,
                            businessKey =
                                item.optString(
                                    "businessKey",
                                    "legacy"
                                ).ifBlank {
                                    "legacy"
                                },
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    }
                    val productIds =
                        inventoryProducts
                            .map {
                                it.id
                            }
                            .toSet()

                    val productBaseUnitKeys =
                        inventoryProducts
                            .associate { product ->
                                product.id to
                                    product.unit
                                        .trim()
                                        .ifBlank {
                                            "pcs"
                                        }
                                        .lowercase(
                                            Locale.ROOT
                                        )
                            }

                    if (backupVersion >= 7) {
                        val unitArray =
                            root.optJSONArray(
                                "inventoryUnits"
                            ) ?: JSONArray()

                        val seenUnits =
                            mutableSetOf<Pair<Long, String>>()

                        for (
                            index in
                            0 until unitArray.length()
                        ) {
                            val item =
                                unitArray
                                    .getJSONObject(
                                        index
                                    )

                            val productId =
                                item.getLong(
                                    "productId"
                                )

                            require(
                                productId in
                                    productIds
                            ) {
                                "ইউনিটের পণ্য পাওয়া যায়নি"
                            }

                            val unitName =
                                item.getString(
                                    "unitName"
                                ).trim()

                            val unitKey =
                                unitName.lowercase(
                                    Locale.ROOT
                                )

                            val baseQuantity =
                                item.getInt(
                                    "baseQuantity"
                                )

                            val sortOrder =
                                item.getInt(
                                    "sortOrder"
                                )

                            require(
                                unitName.isNotBlank() &&
                                    baseQuantity > 1 &&
                                    sortOrder > 0
                            ) {
                                "পণ্যের ইউনিট তথ্য সঠিক নয়"
                            }

                            require(
                                unitKey !=
                                    productBaseUnitKeys[
                                        productId
                                    ]
                            ) {
                                "অতিরিক্ত ইউনিট Base Unit-এর সমান হতে পারবে না"
                            }

                            require(
                                seenUnits.add(
                                    productId to
                                        unitKey
                                )
                            ) {
                                "একই পণ্যের একই ইউনিট একাধিকবার আছে"
                            }

                            inventoryUnits +=
                                ProductUnitConversionEntity(
                                    id =
                                        item.optLong(
                                            "id",
                                            0L
                                        ),
                                    productId =
                                        productId,
                                    unitName =
                                        unitName,
                                    unitKey =
                                        unitKey,
                                    baseQuantity =
                                        baseQuantity,
                                    sortOrder =
                                        sortOrder,
                                    createdAt =
                                        item.optLong(
                                            "createdAt",
                                            System.currentTimeMillis()
                                        )
                                )
                        }
                    }

                    val batchArray = root.optJSONArray("inventoryBatches") ?: JSONArray()
                    for (index in 0 until batchArray.length()) {
                        val item = batchArray.getJSONObject(index)
                        val productId = item.getLong("productId")
                        require(productId in productIds) { "স্টক ব্যাচের পণ্য পাওয়া যায়নি" }
                        inventoryBatches += StockBatchEntity(
                            id = item.getLong("id"),
                            productId = productId,
                            batchNo = item.optString("batchNo", ""),
                            quantity = item.optInt("quantity", 0).coerceAtLeast(0),
                            purchasePrice = item.optDouble("purchasePrice", 0.0).coerceAtLeast(0.0),
                            purchaseDate = item.optLong("purchaseDate", System.currentTimeMillis()),
                            expiryDate = item.optLong("expiryDate", 0L).takeIf { it > 0L },
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    }
                }

                database.withTransaction {
                    dao.clearFinancialAccountEntries()
                    dao.clearFinancialAccounts()
                    dao.clearBakiEntries()
                    dao.clearPeople()
                    dao.clearTransactions()

                    transactions.forEach {
                        dao.insertTransaction(it)
                    }

                    people.forEach {
                        dao.insertPerson(it)
                    }

                    entries.forEach {
                        dao.insertBakiEntry(it)
                    }

                    financialAccounts.forEach {
                        dao.insertFinancialAccount(it)
                    }

                    financialAccountEntries.forEach {
                        dao.insertFinancialAccountEntry(it)
                    }
                }

                if (backupVersion >= 3) {
                    InventoryBackupBridge.restore(
                        getApplication(),
                        inventoryProducts,
                        inventoryBatches,
                        inventoryUnits
                    )
                }

                val businessRowsRestored =
                    V15BusinessBackupBridge.restore(
                        getApplication(),
                        if (backupVersion >= 5) {
                            root.optJSONObject(
                                "businessData"
                            )
                        } else {
                            null
                        }
                    )

                if (backupVersion >= 4) {
                    V15SettingsBackupBridge.restore(
                        getApplication(),
                        root.optJSONObject("settings")
                    )?.let { restoredWorkspace ->
                        selectWorkspace(restoredWorkspace)
                    }
                }

                transactions.size +
                    people.size +
                    entries.size +
                    financialAccounts.size +
                    financialAccountEntries.size +
                    inventoryProducts.size +
                    inventoryBatches.size +
                    inventoryUnits.size +
                    businessRowsRestored
            }.onSuccess(onDone).onFailure {
                onError(it.message ?: "ব্যাকআপ রিস্টোর করা যায়নি")
            }
        }
    }

}
