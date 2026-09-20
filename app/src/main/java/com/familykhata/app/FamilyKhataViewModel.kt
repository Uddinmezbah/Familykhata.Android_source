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
import com.familykhata.app.data.BusinessProfileEntity
import com.familykhata.app.data.DashboardTotals
import com.familykhata.app.data.DueReceivableItem
import com.familykhata.app.data.DigitalServiceTransactionEntity
import com.familykhata.app.data.FinancialAccountEntity
import com.familykhata.app.data.FinancialAccountEntryEntity
import com.familykhata.app.data.FinancialAccountSummary
import com.familykhata.app.data.InventoryBackupBridge
import com.familykhata.app.data.InventoryDatabase
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

    private val businessProfilePreferences =
        application.getSharedPreferences(
            "hisabi_khata_v14_settings",
            Context.MODE_PRIVATE
        )

    private val legacyBusinessId: String =
        preferences.getString(
            "legacy_business_id",
            null
        )?.takeIf {
            it.isNotBlank()
        } ?: UUID.randomUUID().toString().also { id ->
            preferences.edit()
                .putString(
                    "legacy_business_id",
                    id
                )
                .putString(
                    "selected_business_id",
                    id
                )
                .apply()
        }

    private val _selectedBusinessId =
        MutableStateFlow(
            preferences.getString(
                "selected_business_id",
                legacyBusinessId
            )?.takeIf {
                it.isNotBlank()
            } ?: legacyBusinessId
        )

    val selectedBusinessId:
        StateFlow<String> =
        _selectedBusinessId.asStateFlow()

    val businessProfiles:
        StateFlow<List<BusinessProfileEntity>> =
        dao.observeBusinessProfiles()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(
                    5_000
                ),
                emptyList()
            )

    init {
        if (
            preferences.getString(
                "selected_business_id",
                null
            ).isNullOrBlank()
        ) {
            preferences.edit()
                .putString(
                    "selected_business_id",
                    legacyBusinessId
                )
                .apply()
        }
        ensureLegacyBusinessProfile()
        backfillLegacyBusinessScope()
        scopeLegacyInventoryData()
        scopeLegacyBusinessModules()
    }

    private fun ensureLegacyBusinessProfile() {
        viewModelScope.launch {
            if (
                dao.getBusinessProfile(
                    legacyBusinessId
                ) != null
            ) {
                return@launch
            }

            dao.upsertBusinessProfile(
                BusinessProfileEntity(
                    businessId =
                        legacyBusinessId,
                    name =
                        businessProfilePreferences
                            .getString(
                                "business_name",
                                ""
                            )
                            .orEmpty()
                            .trim()
                            .ifBlank {
                                "দোকান/প্রতিষ্ঠান"
                            },
                    businessType =
                        businessProfilePreferences
                            .getString(
                                "business_type",
                                ""
                            )
                            .orEmpty()
                            .trim(),
                    phone =
                        businessProfilePreferences
                            .getString(
                                "profile_phone",
                                ""
                            )
                            .orEmpty()
                            .trim(),
                    address =
                        businessProfilePreferences
                            .getString(
                                "business_address",
                                ""
                            )
                            .orEmpty()
                            .trim(),
                    logoPath =
                        businessProfilePreferences
                            .getString(
                                "business_logo_path",
                                ""
                            )
                            .orEmpty()
                            .trim()
                )
            )
        }
    }


    private fun backfillLegacyBusinessScope() {
        viewModelScope.launch {
            database.withTransaction {
                dao.backfillShopTransactionsBusinessId(legacyBusinessId)
                dao.backfillShopPeopleBusinessId(legacyBusinessId)
                dao.backfillShopAccountsBusinessId(legacyBusinessId)
                dao.backfillShopAccountEntriesBusinessId(legacyBusinessId)
                dao.backfillShopDigitalServicesBusinessId(legacyBusinessId)
            }
        }
    }
    private fun scopeLegacyInventoryData() {
        viewModelScope.launch {
            val legacyKey =
                businessDataKey(
                    businessProfilePreferences
                        .getString(
                            "business_type",
                            ""
                        )
                        .orEmpty()
                )

            val inventoryDao =
                InventoryDatabase
                    .get(getApplication())
                    .dao()

            inventoryDao.claimExistingBusinessProducts(
                workspace = "SHOP",
                legacyBusinessKey = legacyKey,
                targetBusinessId = legacyBusinessId
            )

            inventoryDao.claimExistingBusinessRetailSales(
                workspace = "SHOP",
                legacyBusinessKey = legacyKey,
                targetBusinessId = legacyBusinessId
            )
        }
    }

    private fun scopeLegacyBusinessModules() {
        if (
            preferences.getBoolean(
                "specialized_business_scope_v1",
                false
            )
        ) {
            return
        }

        viewModelScope.launch {
            runCatching {
                V15BusinessBackupBridge
                    .scopeLegacyShopWorkspace(
                        context = getApplication(),
                        scopedWorkspace =
                            businessWorkspaceKey(
                                "SHOP",
                                legacyBusinessId
                            )
                    )
            }.onSuccess {
                preferences.edit()
                    .putBoolean(
                        "specialized_business_scope_v1",
                        true
                    )
                    .apply()
            }
        }
    }

    fun selectBusiness(
        businessId: String
    ) {
        val cleanId = businessId.trim()
        if (
            cleanId.isBlank() ||
            cleanId == _selectedBusinessId.value
        ) {
            return
        }

        viewModelScope.launch {
            val profile =
                dao.getBusinessProfile(cleanId)
                    ?: return@launch

            if (!profile.isActive) {
                return@launch
            }

            _selectedBusinessId.value =
                cleanId

            preferences.edit()
                .putString(
                    "selected_business_id",
                    cleanId
                )
                .apply()
        }
    }

    fun syncCurrentBusinessProfile(
        businessName: String,
        businessType: String,
        phone: String,
        address: String,
        logoPath: String
    ) {
        val businessId =
            _selectedBusinessId.value

        viewModelScope.launch {
            val existing =
                dao.getBusinessProfile(
                    businessId
                )

            dao.upsertBusinessProfile(
                BusinessProfileEntity(
                    businessId = businessId,
                    name =
                        businessName
                            .trim()
                            .ifBlank {
                                "দোকান/প্রতিষ্ঠান"
                            },
                    businessType =
                        businessType.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    logoPath = logoPath.trim(),
                    isActive =
                        existing?.isActive
                            ?: true,
                    createdAt =
                        existing?.createdAt
                            ?: System.currentTimeMillis()
                )
            )
        }
    }

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

    private val activeDataScope =
        combine(_selectedWorkspace, _selectedBusinessId) { workspace, businessId ->
            workspace to businessId
        }

    val transactions: StateFlow<List<TransactionEntity>> = activeDataScope
        .flatMapLatest { (workspace, businessId) -> dao.observeTransactions(workspace, businessId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<DashboardTotals> = activeDataScope
        .flatMapLatest { (workspace, businessId) -> dao.observeDashboardTotals(workspace, businessId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardTotals(0.0, 0.0))

    val digitalServiceTransactions: StateFlow<List<DigitalServiceTransactionEntity>> = activeDataScope
        .flatMapLatest { (workspace, businessId) -> dao.observeDigitalServiceTransactions(workspace, businessId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val financialAccounts: StateFlow<List<FinancialAccountSummary>> = activeDataScope
        .flatMapLatest { (workspace, businessId) -> dao.observeFinancialAccounts(workspace, businessId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bakiPeople: StateFlow<List<BakiPersonSummary>> = activeDataScope
        .flatMapLatest { (workspace, businessId) -> dao.observeBakiSummaries(workspace, businessId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val workspacePeople = activeDataScope
        .flatMapLatest { (workspace, businessId) -> dao.observePeople(workspace, businessId) }

    private val workspaceBakiEntries = activeDataScope
        .flatMapLatest { (workspace, businessId) -> dao.observeWorkspaceBakiEntries(workspace, businessId) }

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

    private fun businessIdForWorkspace(workspace: String): String =
        if (workspace == "SHOP") {
            _selectedBusinessId.value
        } else {
            ""
        }

    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        note: String,
        financialAccountId: Long? = null,
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanType =
            type.trim().uppercase(Locale.ROOT)

        if (
            !canWriteNow() ||
            cleanType !in setOf(
                "INCOME",
                "EXPENSE"
            ) ||
            !amount.isFinite() ||
            amount <= 0.0
        ) {
            onDone(false)
            return
        }

        val workspace =
            _selectedWorkspace.value
        val businessId = businessIdForWorkspace(workspace)

        if (
            workspace == "SHOP" &&
            financialAccountId == null
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val account =
                            if (
                                workspace == "SHOP"
                            ) {
                                requireNotNull(
                                    dao.getFinancialAccountOnce(
                                        financialAccountId!!
                                    )
                                ).also {
                                    require(
                                        it.workspace == workspace &&
                                            it.businessId == businessId &&
                                            it.isActive
                                    )
                                }
                            } else {
                                null
                            }

                        if (
                            cleanType ==
                                "EXPENSE" &&
                            account != null
                        ) {
                            val available =
                                requireNotNull(
                                    dao.getFinancialAccountBalanceOnce(
                                        account.id
                                    )
                                )

                            require(
                                available + 0.0001 >=
                                    amount
                            )
                        }

                        val now =
                            System.currentTimeMillis()

                        val transactionId =
                            dao.insertTransaction(
                                TransactionEntity(
                                    type =
                                        cleanType,
                                    amount =
                                        amount,
                                    category =
                                        category
                                            .trim()
                                            .ifBlank {
                                                "অন্যান্য"
                                            },
                                    note =
                                        note.trim(),
                                    workspace =
                                        workspace,
                                    businessId = businessId,
                                    financialAccountId =
                                        account?.id,
                                    createdAt =
                                        now
                                )
                            )

                        require(
                            transactionId > 0L
                        )

                        if (account != null) {
                            dao.insertFinancialAccountEntry(
                                FinancialAccountEntryEntity(
                                    accountId =
                                        account.id,
                                    entryType =
                                        if (
                                            cleanType ==
                                                "INCOME"
                                        ) {
                                            "TRANSACTION_IN"
                                        } else {
                                            "TRANSACTION_OUT"
                                        },
                                    amount =
                                        amount,
                                    balanceDelta =
                                        if (
                                            cleanType ==
                                                "INCOME"
                                        ) {
                                            amount
                                        } else {
                                            -amount
                                        },
                                    sourceKey =
                                        "TRANSACTION:" +
                                            transactionId,
                                    note =
                                        listOf(
                                            category.trim(),
                                            note.trim()
                                        )
                                            .filter {
                                                it.isNotBlank()
                                            }
                                            .joinToString(
                                                " • "
                                            ),
                                    workspace =
                                        workspace,
                                    businessId = businessId,
                                    createdAt =
                                        now
                                )
                            )
                        }
                    }

                    true
                }.getOrDefault(false)

            onDone(success)
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
        val businessId = businessIdForWorkspace(workspace)

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
                            workspace = workspace,
                            businessId = businessId
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
        val businessId = businessIdForWorkspace(workspace)

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
                                to.workspace == workspace &&
                                (
                                    workspace != "SHOP" ||
                                        (
                                            from.businessId == businessId &&
                                                to.businessId == businessId
                                        )
                                )
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
                                businessId = businessId,
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
                                businessId = businessId,
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

    fun recordAgentCashOut(
        cashAccountId: Long,
        walletAccountId: Long,
        principalAmount: Double,
        customerFee: Double,
        providerCharge: Double,
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            !principalAmount.isFinite() ||
            !customerFee.isFinite() ||
            !providerCharge.isFinite() ||
            principalAmount <= 0.0 ||
            customerFee < 0.0 ||
            providerCharge < 0.0
        ) {
            onDone(false)
            return
        }

        val netProfit =
            customerFee -
                providerCharge

        val walletIncrease =
            principalAmount +
                netProfit

        if (
            !walletIncrease.isFinite() ||
            walletIncrease <= 0.0
        ) {
            onDone(false)
            return
        }

        recordDigitalService(
            serviceType =
                "AGENT_CASH_OUT",
            sourceAccountId =
                cashAccountId,
            destinationAccountId =
                walletAccountId,
            serviceAmount =
                principalAmount,
            customerFee =
                customerFee,
            providerCharge =
                providerCharge,
            customerPaid =
                0.0,
            providerCost =
                0.0,
            sourceAmount =
                principalAmount,
            destinationAmount =
                walletIncrease,
            profit =
                netProfit,
            note =
                note,
            onDone =
                onDone
        )
    }

    fun recordMobileRecharge(
        rechargeAccountId: Long,
        receiveAccountId: Long,
        faceValue: Double,
        customerPaid: Double,
        providerCost: Double,
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            !faceValue.isFinite() ||
            !customerPaid.isFinite() ||
            !providerCost.isFinite() ||
            faceValue <= 0.0 ||
            customerPaid <= 0.0 ||
            providerCost <= 0.0
        ) {
            onDone(false)
            return
        }

        val profit =
            customerPaid -
                providerCost

        if (!profit.isFinite()) {
            onDone(false)
            return
        }

        recordDigitalService(
            serviceType =
                "MOBILE_RECHARGE",
            sourceAccountId =
                rechargeAccountId,
            destinationAccountId =
                receiveAccountId,
            serviceAmount =
                faceValue,
            customerFee =
                0.0,
            providerCharge =
                0.0,
            customerPaid =
                customerPaid,
            providerCost =
                providerCost,
            sourceAmount =
                providerCost,
            destinationAmount =
                customerPaid,
            profit =
                profit,
            note =
                note,
            onDone =
                onDone
        )
    }

    private fun recordDigitalService(
        serviceType: String,
        sourceAccountId: Long,
        destinationAccountId: Long,
        serviceAmount: Double,
        customerFee: Double,
        providerCharge: Double,
        customerPaid: Double,
        providerCost: Double,
        sourceAmount: Double,
        destinationAmount: Double,
        profit: Double,
        note: String,
        onDone: (Boolean) -> Unit
    ) {
        if (
            !canWriteNow() ||
            _selectedWorkspace.value !=
                "SHOP" ||
            sourceAccountId <= 0L ||
            destinationAccountId <= 0L ||
            sourceAccountId ==
                destinationAccountId ||
            serviceType !in
                setOf(
                    "AGENT_CASH_OUT",
                    "MOBILE_RECHARGE"
                ) ||
            !serviceAmount.isFinite() ||
            !sourceAmount.isFinite() ||
            !destinationAmount.isFinite() ||
            !profit.isFinite() ||
            serviceAmount <= 0.0 ||
            sourceAmount <= 0.0 ||
            destinationAmount <= 0.0
        ) {
            onDone(false)
            return
        }

        val workspace =
            _selectedWorkspace.value
        val businessId = businessIdForWorkspace(workspace)

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val source =
                            requireNotNull(
                                dao.getFinancialAccountOnce(
                                    sourceAccountId
                                )
                            )

                        val destination =
                            requireNotNull(
                                dao.getFinancialAccountOnce(
                                    destinationAccountId
                                )
                            )

                        require(
                            source.workspace == workspace &&
                                destination.workspace == workspace &&
                                (
                                    workspace != "SHOP" ||
                                        (
                                            source.businessId == businessId &&
                                                destination.businessId == businessId
                                        )
                                )
                        )

                        require(
                            source.isActive &&
                                destination.isActive
                        )

                        if (
                            serviceType ==
                                "AGENT_CASH_OUT"
                        ) {
                            require(
                                source.type ==
                                    "CASH" &&
                                    destination.type ==
                                        "MOBILE_WALLET"
                            )
                        }

                        val available =
                            requireNotNull(
                                dao.getFinancialAccountBalanceOnce(
                                    source.id
                                )
                            )

                        require(
                            available +
                                0.0001 >=
                                sourceAmount
                        )

                        val eventKey =
                            UUID.randomUUID()
                                .toString()

                        val now =
                            System.currentTimeMillis()

                        val serviceId =
                            dao.insertDigitalServiceTransaction(
                                DigitalServiceTransactionEntity(
                                    eventKey =
                                        eventKey,
                                    serviceType =
                                        serviceType,
                                    sourceAccountId =
                                        source.id,
                                    destinationAccountId =
                                        destination.id,
                                    serviceAmount =
                                        serviceAmount,
                                    customerFee =
                                        customerFee,
                                    providerCharge =
                                        providerCharge,
                                    customerPaid =
                                        customerPaid,
                                    providerCost =
                                        providerCost,
                                    sourceAmount =
                                        sourceAmount,
                                    destinationAmount =
                                        destinationAmount,
                                    profit =
                                        profit,
                                    note =
                                        note.trim(),
                                    workspace =
                                        workspace,
                                    businessId = businessId,
                                    createdAt =
                                        now
                                )
                            )

                        require(
                            serviceId > 0L
                        )

                        dao.insertFinancialAccountEntry(
                            FinancialAccountEntryEntity(
                                accountId =
                                    source.id,
                                entryType =
                                    "SERVICE_OUT",
                                amount =
                                    sourceAmount,
                                balanceDelta =
                                    -sourceAmount,
                                relatedAccountId =
                                    destination.id,
                                transferGroupId =
                                    null,
                                sourceKey =
                                    "DIGITAL_SERVICE:" +
                                        "$eventKey:SOURCE",
                                note =
                                    note.trim(),
                                workspace =
                                    workspace,
                                    businessId = businessId,
                                createdAt =
                                    now
                            )
                        )

                        dao.insertFinancialAccountEntry(
                            FinancialAccountEntryEntity(
                                accountId =
                                    destination.id,
                                entryType =
                                    "SERVICE_IN",
                                amount =
                                    destinationAmount,
                                balanceDelta =
                                    destinationAmount,
                                relatedAccountId =
                                    source.id,
                                transferGroupId =
                                    null,
                                sourceKey =
                                    "DIGITAL_SERVICE:" +
                                        "$eventKey:DESTINATION",
                                note =
                                    note.trim(),
                                workspace =
                                    workspace,
                                    businessId = businessId,
                                createdAt =
                                    now
                            )
                        )

                        if (
                            kotlin.math.abs(
                                profit
                            ) >= 0.0001
                        ) {
                            val profitType =
                                if (
                                    profit > 0.0
                                ) {
                                    "INCOME"
                                } else {
                                    "EXPENSE"
                                }

                            val profitCategory =
                                if (
                                    serviceType ==
                                        "AGENT_CASH_OUT"
                                ) {
                                    "Agent Cash Out Profit"
                                } else {
                                    "Mobile Recharge Profit"
                                }

                            dao.insertTransaction(
                                TransactionEntity(
                                    type =
                                        profitType,
                                    amount =
                                        kotlin.math.abs(
                                            profit
                                        ),
                                    category =
                                        profitCategory,
                                    note =
                                        note.trim(),
                                    workspace =
                                        workspace,
                                    businessId = businessId,
                                    sourceKey =
                                        "DIGITAL_SERVICE:" +
                                            "$eventKey:PROFIT",
                                    createdAt =
                                        now
                                )
                            )
                        }
                    }

                    true
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        if (
            !canWriteNow() ||
            item.sourceKey
                ?.startsWith(
                    "DIGITAL_SERVICE:"
                ) == true
        ) {
            return
        }

        viewModelScope.launch {
            database.withTransaction {
                if (
                    item.financialAccountId !=
                        null
                ) {
                    dao.deleteFinancialAccountEntryBySourceKey(
                        "TRANSACTION:" +
                            item.id
                    )
                }

                dao.deleteTransaction(item)
            }
        }
    }

    fun updateTransaction(
        item: TransactionEntity,
        type: String,
        amount: Double,
        category: String,
        note: String,
        financialAccountId: Long? =
            item.financialAccountId,
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanType =
            type.trim().uppercase(Locale.US)

        val businessId = businessIdForWorkspace(item.workspace)

        if (
            !canWriteNow() ||
            item.sourceKey
                ?.startsWith(
                    "DIGITAL_SERVICE:"
                ) == true ||
            cleanType !in setOf(
                "INCOME",
                "EXPENSE"
            ) ||
            !amount.isFinite() ||
            amount <= 0.0 ||
            (
                item.workspace == "SHOP" &&
                    financialAccountId == null
            )
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val entryKey =
                            "TRANSACTION:" +
                                item.id

                        val oldEntry =
                            dao.getFinancialAccountEntryBySourceKey(
                                entryKey
                            )

                        val account =
                            if (
                                item.workspace ==
                                    "SHOP"
                            ) {
                                requireNotNull(
                                    dao.getFinancialAccountOnce(
                                        financialAccountId!!
                                    )
                                ).also {
                                    require(
                                        it.workspace == item.workspace &&
                                            it.businessId == businessId &&
                                            (
                                                it.isActive ||
                                                it.id ==
                                                    item.financialAccountId
                                            )
                                    )
                                }
                            } else {
                                null
                            }

                        val newDelta =
                            if (
                                cleanType ==
                                    "INCOME"
                            ) {
                                amount
                            } else {
                                -amount
                            }

                        if (
                            oldEntry != null &&
                            (
                                account == null ||
                                oldEntry.accountId !=
                                    account.id
                            )
                        ) {
                            val oldAccountBalance =
                                requireNotNull(
                                    dao.getFinancialAccountBalanceOnce(
                                        oldEntry.accountId
                                    )
                                )

                            val oldAccountAfterRemoval =
                                oldAccountBalance -
                                    oldEntry.balanceDelta

                            require(
                                oldAccountAfterRemoval >=
                                    -0.0001
                            )
                        }

                        if (account != null) {
                            val targetBalance =
                                requireNotNull(
                                    dao.getFinancialAccountBalanceOnce(
                                        account.id
                                    )
                                )

                            val oldDeltaOnTarget =
                                oldEntry
                                    ?.takeIf {
                                        it.accountId ==
                                            account.id
                                    }
                                    ?.balanceDelta
                                    ?: 0.0

                            val targetAfterUpdate =
                                targetBalance -
                                    oldDeltaOnTarget +
                                    newDelta

                            require(
                                targetAfterUpdate >=
                                    -0.0001
                            )
                        }

                        dao.deleteFinancialAccountEntryBySourceKey(
                            entryKey
                        )

                        dao.updateTransaction(
                            transactionId =
                                item.id,
                            type =
                                cleanType,
                            amount =
                                amount,
                            category =
                                category
                                    .trim()
                                    .ifBlank {
                                        "অন্যান্য"
                                    },
                            note =
                                note.trim(),
                            financialAccountId =
                                account?.id
                        )

                        if (account != null) {
                            dao.insertFinancialAccountEntry(
                                FinancialAccountEntryEntity(
                                    accountId =
                                        account.id,
                                    entryType =
                                        if (
                                            cleanType ==
                                                "INCOME"
                                        ) {
                                            "TRANSACTION_IN"
                                        } else {
                                            "TRANSACTION_OUT"
                                        },
                                    amount =
                                        amount,
                                    balanceDelta =
                                        if (
                                            cleanType ==
                                                "INCOME"
                                        ) {
                                            amount
                                        } else {
                                            -amount
                                        },
                                    sourceKey =
                                        entryKey,
                                    note =
                                        listOf(
                                            category.trim(),
                                            note.trim()
                                        )
                                            .filter {
                                                it.isNotBlank()
                                            }
                                            .joinToString(
                                                " • "
                                            ),
                                    workspace =
                                        item.workspace,
                                    businessId = businessId,
                                    createdAt =
                                        item.createdAt
                                )
                            )
                        }
                    }

                    true
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun addBakiPerson(name: String, phone: String) {
        if (!canWriteNow() || name.isBlank()) return
        val workspace = _selectedWorkspace.value
        val businessId = businessIdForWorkspace(workspace)
        viewModelScope.launch {
            dao.insertPerson(
                BakiPersonEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    workspace = workspace,
                    businessId = businessId
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
                val person = requireNotNull(dao.getStatementPerson(personId, workspace, businessIdForWorkspace(workspace))) {
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

                val reportBusinessId = businessIdForWorkspace(workspace)
                val people = dao.getAllPeople().filter {
                    it.workspace == workspace &&
                        (workspace != "SHOP" || it.businessId == reportBusinessId)
                }
                val personMap = people.associateBy { it.id }
                val personIds = personMap.keys
                val rows = mutableListOf<Pair<Long, List<String>>>()

                dao.getAllTransactions()
                    .asSequence()
                    .filter {
                        it.workspace == workspace &&
                            (workspace != "SHOP" || it.businessId == reportBusinessId) &&
                            it.createdAt in startAt..now
                    }
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
                        .filterNot {
                            it.entryType == "RETAIL_SALE_IN"
                        }
                val digitalServiceTransactions =
                    dao.getAllDigitalServiceTransactions()
                val businessProfiles = dao.getAllBusinessProfiles()
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
                    put("version", 13)
                    put("createdAt", System.currentTimeMillis())
                    put("selectedBusinessId", _selectedBusinessId.value)
                    put("businessProfiles", JSONArray().apply {
                        businessProfiles.forEach { profile ->
                            put(JSONObject().apply {
                                put("businessId", profile.businessId)
                                put("name", profile.name)
                                put("businessType", profile.businessType)
                                put("phone", profile.phone)
                                put("address", profile.address)
                                put("isActive", profile.isActive)
                                put("createdAt", profile.createdAt)
                            })
                        }
                    })
                    put(
                        "inventoryRetailSales",
                        InventoryBackupBridge.retailSalesToJson(
                            inventory.retailSales
                        )
                    )
                    put(
                        "inventoryRetailSaleLines",
                        InventoryBackupBridge.retailSaleLinesToJson(
                            inventory.retailSaleLines
                        )
                    )
                    put(
                        "inventoryRetailSaleStockAllocations",
                        InventoryBackupBridge
                            .retailSaleStockAllocationsToJson(
                                inventory.retailSaleStockAllocations
                            )
                    )
                    put(
                        "inventoryRetailSalePayments",
                        InventoryBackupBridge.retailSalePaymentsToJson(
                            inventory.retailSalePayments
                        )
                    )
                    put("transactions", JSONArray().apply {
                        transactions.forEach { item ->
                            put(JSONObject().apply {
                                put("id", item.id)
                                put("type", item.type)
                                put("amount", item.amount)
                                put("category", item.category)
                                put("note", item.note)
                                put("workspace", item.workspace)
                                put("businessId", item.businessId)
                                item.sourceKey?.let {
                                    put(
                                        "sourceKey",
                                        it
                                    )
                                }

                                item.financialAccountId
                                    ?.let {
                                        put(
                                            "financialAccountId",
                                            it
                                        )
                                    }

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
                                put("businessId", person.businessId)
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
                                        put("businessId", account.businessId)
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
                                        put("businessId", entry.businessId)
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
                        "digitalServiceTransactions",
                        JSONArray().apply {
                            digitalServiceTransactions
                                .forEach { service ->
                                    put(
                                        JSONObject().apply {
                                            put(
                                                "id",
                                                service.id
                                            )
                                            put(
                                                "eventKey",
                                                service.eventKey
                                            )
                                            put(
                                                "serviceType",
                                                service.serviceType
                                            )
                                            put(
                                                "sourceAccountId",
                                                service.sourceAccountId
                                            )
                                            put(
                                                "destinationAccountId",
                                                service.destinationAccountId
                                            )
                                            put(
                                                "serviceAmount",
                                                service.serviceAmount
                                            )
                                            put(
                                                "customerFee",
                                                service.customerFee
                                            )
                                            put(
                                                "providerCharge",
                                                service.providerCharge
                                            )
                                            put(
                                                "customerPaid",
                                                service.customerPaid
                                            )
                                            put(
                                                "providerCost",
                                                service.providerCost
                                            )
                                            put(
                                                "sourceAmount",
                                                service.sourceAmount
                                            )
                                            put(
                                                "destinationAmount",
                                                service.destinationAmount
                                            )
                                            put(
                                                "profit",
                                                service.profit
                                            )
                                            put(
                                                "note",
                                                service.note
                                            )
                                            put(
                                                "workspace",
                                                service.workspace
                                            )
                                            put("businessId", service.businessId)
                                            put(
                                                "createdAt",
                                                service.createdAt
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
                require(backupVersion in 1..13) {
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

                val digitalServiceTransactions =
                    mutableListOf<
                        DigitalServiceTransactionEntity
                    >()

                val inventoryProducts =
                    mutableListOf<ProductEntity>()
                val inventoryBatches = mutableListOf<StockBatchEntity>()
                val inventoryUnits =
                    mutableListOf<ProductUnitConversionEntity>()
                val actions = setOf("GAVE", "RECEIVED_BACK", "TOOK", "PAID_BACK")
                val restoredBusinessProfiles = mutableListOf<BusinessProfileEntity>()
                val restoredSelectedBusinessId: String

                if (backupVersion >= 13) {
                    val profileArray = root.optJSONArray("businessProfiles") ?: JSONArray()
                    val seenBusinessIds = mutableSetOf<String>()

                    for (index in 0 until profileArray.length()) {
                        val item = profileArray.getJSONObject(index)
                        val businessId = item.getString("businessId").trim()
                        val name = item.getString("name").trim()
                        require(businessId.isNotBlank() && businessId !in seenBusinessIds) { "Business ID সঠিক নয়" }
                        require(name.isNotBlank()) { "Business name খালি হতে পারে না" }
                        seenBusinessIds += businessId
                        restoredBusinessProfiles += BusinessProfileEntity(
                            businessId = businessId,
                            name = name,
                            businessType = item.optString("businessType", "").trim(),
                            phone = item.optString("phone", "").trim(),
                            address = item.optString("address", "").trim(),
                            logoPath = "",
                            isActive = item.optBoolean("isActive", true),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    }

                    require(restoredBusinessProfiles.isNotEmpty()) { "Business profile পাওয়া যায়নি" }
                    val requestedBusinessId = root.optString("selectedBusinessId", "").trim()
                    restoredSelectedBusinessId = restoredBusinessProfiles
                        .firstOrNull { it.businessId == requestedBusinessId && it.isActive }
                        ?.businessId
                        ?: restoredBusinessProfiles.firstOrNull { it.isActive }?.businessId
                        ?: restoredBusinessProfiles.first().businessId
                } else {
                    val settingsJson = root.optJSONObject("settings")
                    restoredBusinessProfiles += BusinessProfileEntity(
                        businessId = legacyBusinessId,
                        name = settingsJson?.optString("businessName", "")?.trim().orEmpty().ifBlank { "দোকান/প্রতিষ্ঠান" },
                        businessType = settingsJson?.optString("businessType", "")?.trim().orEmpty(),
                        phone = settingsJson?.optString("profilePhone", "")?.trim().orEmpty(),
                        address = settingsJson?.optString("businessAddress", "")?.trim().orEmpty(),
                        logoPath = "",
                        isActive = true
                    )
                    restoredSelectedBusinessId = legacyBusinessId
                }

                val validBusinessIds = restoredBusinessProfiles.map { it.businessId }.toSet()

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
                        businessId = if (workspace == "SHOP") {
                            if (backupVersion >= 13) {
                                item.optString("businessId", "").trim().also {
                                    require(it in validBusinessIds) { "Transaction business সঠিক নয়" }
                                }
                            } else restoredSelectedBusinessId
                        } else "",
                        sourceKey =
                            if (
                                backupVersion >= 10
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
                        financialAccountId =
                            if (
                                backupVersion >= 11
                            ) {
                                item.optLong(
                                    "financialAccountId",
                                    0L
                                ).takeIf {
                                    it > 0L
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

                val transactionSourceKeys =
                    transactions.mapNotNull {
                        it.sourceKey
                    }

                require(
                    transactionSourceKeys.size ==
                        transactionSourceKeys
                            .toSet()
                            .size
                ) {
                    "ব্যাকআপে একই transaction sourceKey একাধিকবার আছে"
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
                        businessId = if (workspace == "SHOP") {
                            if (backupVersion >= 13) {
                                item.optString("businessId", "").trim().also {
                                    require(it in validBusinessIds) { "Person business সঠিক নয়" }
                                }
                            } else restoredSelectedBusinessId
                        } else "",
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
                                businessId = if (workspace == "SHOP") {
                                    if (backupVersion >= 13) {
                                        item.optString("businessId", "").trim().also {
                                            require(it in validBusinessIds) { "Account business সঠিক নয়" }
                                        }
                                    } else restoredSelectedBusinessId
                                } else "",
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

                        val allowedEntryTypes =
                            when {
                                backupVersion >= 12 ->
                                    setOf(
                                        "TRANSFER_IN",
                                        "TRANSFER_OUT",
                                        "SERVICE_IN",
                                        "SERVICE_OUT",
                                        "TRANSACTION_IN",
                                        "TRANSACTION_OUT",
                                        "RETAIL_SALE_IN"
                                    )

                                backupVersion >= 11 ->
                                    setOf(
                                        "TRANSFER_IN",
                                        "TRANSFER_OUT",
                                        "SERVICE_IN",
                                        "SERVICE_OUT",
                                        "TRANSACTION_IN",
                                        "TRANSACTION_OUT"
                                    )

                                backupVersion >= 10 ->
                                    setOf(
                                        "TRANSFER_IN",
                                        "TRANSFER_OUT",
                                        "SERVICE_IN",
                                        "SERVICE_OUT"
                                    )

                                else ->
                                    setOf(
                                        "TRANSFER_IN",
                                        "TRANSFER_OUT"
                                    )
                            }

                        require(
                            entryType in
                                allowedEntryTypes
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
                            relatedAccountId
                                ?.let {
                                    accountById[it]
                                }

                        val needsRelatedAccount =
                            entryType ==
                                "TRANSFER_IN" ||
                                entryType ==
                                    "TRANSFER_OUT" ||
                                entryType ==
                                    "SERVICE_IN" ||
                                entryType ==
                                    "SERVICE_OUT"

                        if (needsRelatedAccount) {
                            require(
                                related != null &&
                                    related.id !=
                                        account.id &&
                                    related.workspace ==
                                        workspace
                            ) {
                                "Related account সঠিক নয়"
                            }
                        } else {
                            require(
                                relatedAccountId ==
                                    null
                            ) {
                                "Transaction entry-তে related account থাকা যাবে না"
                            }
                        }

                        val groupId =
                            item.optString(
                                "transferGroupId",
                                ""
                            )
                                .trim()
                                .takeIf {
                                    it.isNotEmpty()
                                }

                        if (
                            entryType ==
                                "TRANSFER_IN" ||
                            entryType ==
                                "TRANSFER_OUT"
                        ) {
                            require(
                                groupId != null
                            ) {
                                "Transfer group পাওয়া যায়নি"
                            }
                        } else {
                            require(
                                groupId == null
                            ) {
                                "Service entry-তে transfer group থাকা যাবে না"
                            }
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
                                        entryType.endsWith(
                                            "_IN"
                                        )
                                    ) {
                                        amount
                                    } else {
                                        -amount
                                    },
                                relatedAccountId =
                                    related?.id,
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
                                businessId = account.businessId,
                                createdAt =
                                    item.optLong(
                                        "createdAt",
                                        System.currentTimeMillis()
                                    )
                            )
                    }

                    financialAccountEntries
                        .filter {
                            it.entryType ==
                                "TRANSFER_IN" ||
                                it.entryType ==
                                    "TRANSFER_OUT"
                        }
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

                if (backupVersion >= 11) {
                    val transactionAccountById =
                        financialAccounts
                            .associateBy {
                                it.id
                            }

                    val transactionAccountEntries =
                        financialAccountEntries
                            .filter {
                                it.entryType ==
                                    "TRANSACTION_IN" ||
                                    it.entryType ==
                                        "TRANSACTION_OUT"
                            }

                    val linkedTransactions =
                        transactions.filter {
                            it.financialAccountId !=
                                null
                        }

                    require(
                        transactionAccountEntries.size ==
                            linkedTransactions.size
                    ) {
                        "Transaction account entry সংখ্যা সঠিক নয়"
                    }

                    linkedTransactions.forEach {
                            transaction ->

                        require(
                            transaction.workspace ==
                                "SHOP"
                        ) {
                            "Linked transaction workspace সঠিক নয়"
                        }

                        val account =
                            requireNotNull(
                                transactionAccountById[
                                    transaction
                                        .financialAccountId
                                ]
                            ) {
                                "Transaction-এর account পাওয়া যায়নি"
                            }

                        require(
                            account.workspace ==
                                transaction.workspace
                        ) {
                            "Transaction account workspace সঠিক নয়"
                        }

                        val expectedKey =
                            "TRANSACTION:" +
                                transaction.id

                        val linkedEntry =
                            transactionAccountEntries
                                .singleOrNull {
                                    it.sourceKey ==
                                        expectedKey
                                }

                        val expectedType =
                            if (
                                transaction.type ==
                                    "INCOME"
                            ) {
                                "TRANSACTION_IN"
                            } else {
                                "TRANSACTION_OUT"
                            }

                        val expectedDelta =
                            if (
                                transaction.type ==
                                    "INCOME"
                            ) {
                                transaction.amount
                            } else {
                                -transaction.amount
                            }

                        require(
                            linkedEntry != null &&
                                linkedEntry.accountId ==
                                    account.id &&
                                linkedEntry.entryType ==
                                    expectedType &&
                                kotlin.math.abs(
                                    linkedEntry.amount -
                                        transaction.amount
                                ) < 0.0001 &&
                                kotlin.math.abs(
                                    linkedEntry.balanceDelta -
                                        expectedDelta
                                ) < 0.0001 &&
                                linkedEntry.relatedAccountId ==
                                    null &&
                                linkedEntry.transferGroupId ==
                                    null &&
                                linkedEntry.workspace ==
                                    transaction.workspace
                        ) {
                            "Transaction account link সঠিক নয়"
                        }
                    }
                }

                if (backupVersion >= 10) {
                    val accountById =
                        financialAccounts
                            .associateBy {
                                it.id
                            }

                    val serviceArray =
                        root.optJSONArray(
                            "digitalServiceTransactions"
                        ) ?: JSONArray()

                    val seenServiceIds =
                        mutableSetOf<Long>()

                    val seenEventKeys =
                        mutableSetOf<String>()

                    for (
                        index in
                        0 until serviceArray.length()
                    ) {
                        val item =
                            serviceArray
                                .getJSONObject(index)

                        val id =
                            item.getLong("id")

                        val eventKey =
                            item.getString(
                                "eventKey"
                            ).trim()

                        val serviceType =
                            item.getString(
                                "serviceType"
                            )
                                .trim()
                                .uppercase(
                                    Locale.ROOT
                                )

                        val sourceAccountId =
                            item.getLong(
                                "sourceAccountId"
                            )

                        val destinationAccountId =
                            item.getLong(
                                "destinationAccountId"
                            )

                        val sourceAccount =
                            requireNotNull(
                                accountById[
                                    sourceAccountId
                                ]
                            ) {
                                "Service source account পাওয়া যায়নি"
                            }

                        val destinationAccount =
                            requireNotNull(
                                accountById[
                                    destinationAccountId
                                ]
                            ) {
                                "Service destination account পাওয়া যায়নি"
                            }

                        val workspace =
                            item.getString(
                                "workspace"
                            )

                        val serviceAmount =
                            item.getDouble(
                                "serviceAmount"
                            )

                        val customerFee =
                            item.optDouble(
                                "customerFee",
                                0.0
                            )

                        val providerCharge =
                            item.optDouble(
                                "providerCharge",
                                0.0
                            )

                        val customerPaid =
                            item.optDouble(
                                "customerPaid",
                                0.0
                            )

                        val providerCost =
                            item.optDouble(
                                "providerCost",
                                0.0
                            )

                        require(
                            id > 0L &&
                                id !in seenServiceIds
                        ) {
                            "Service ID সঠিক নয়"
                        }

                        require(
                            eventKey.isNotBlank() &&
                                eventKey !in
                                    seenEventKeys
                        ) {
                            "Service event key সঠিক নয়"
                        }

                        require(
                            serviceType in
                                setOf(
                                    "AGENT_CASH_OUT",
                                    "MOBILE_RECHARGE"
                                )
                        ) {
                            "Service type সঠিক নয়"
                        }

                        require(
                            sourceAccountId !=
                                destinationAccountId &&
                                sourceAccount.workspace ==
                                    workspace &&
                                destinationAccount.workspace ==
                                    workspace &&
                                sourceAccount.businessId == destinationAccount.businessId &&
                                workspace == "SHOP"
                        ) {
                            "Service account/workspace সঠিক নয়"
                        }

                        require(
                            serviceAmount.isFinite() &&
                                customerFee.isFinite() &&
                                providerCharge.isFinite() &&
                                customerPaid.isFinite() &&
                                providerCost.isFinite() &&
                                serviceAmount > 0.0 &&
                                customerFee >= 0.0 &&
                                providerCharge >= 0.0 &&
                                customerPaid >= 0.0 &&
                                providerCost >= 0.0
                        ) {
                            "Service amount সঠিক নয়"
                        }

                        val calculatedSourceAmount: Double
                        val calculatedDestinationAmount: Double
                        val calculatedProfit: Double

                        if (
                            serviceType ==
                                "AGENT_CASH_OUT"
                        ) {
                            require(
                                kotlin.math.abs(
                                    customerPaid
                                ) < 0.0001 &&
                                    kotlin.math.abs(
                                        providerCost
                                    ) < 0.0001
                            ) {
                                "Cash Out field সঠিক নয়"
                            }

                            calculatedProfit =
                                customerFee -
                                    providerCharge

                            calculatedSourceAmount =
                                serviceAmount

                            calculatedDestinationAmount =
                                serviceAmount +
                                    calculatedProfit
                        } else {
                            require(
                                kotlin.math.abs(
                                    customerFee
                                ) < 0.0001 &&
                                    kotlin.math.abs(
                                        providerCharge
                                    ) < 0.0001 &&
                                    customerPaid >
                                        0.0 &&
                                    providerCost >
                                        0.0
                            ) {
                                "Recharge field সঠিক নয়"
                            }

                            calculatedProfit =
                                customerPaid -
                                    providerCost

                            calculatedSourceAmount =
                                providerCost

                            calculatedDestinationAmount =
                                customerPaid
                        }

                        require(
                            calculatedSourceAmount >
                                0.0 &&
                                calculatedDestinationAmount >
                                    0.0 &&
                                calculatedProfit.isFinite()
                        ) {
                            "Service accounting সঠিক নয়"
                        }

                        seenServiceIds += id
                        seenEventKeys += eventKey

                        digitalServiceTransactions +=
                            DigitalServiceTransactionEntity(
                                id = id,
                                eventKey =
                                    eventKey,
                                serviceType =
                                    serviceType,
                                sourceAccountId =
                                    sourceAccountId,
                                destinationAccountId =
                                    destinationAccountId,
                                serviceAmount =
                                    serviceAmount,
                                customerFee =
                                    customerFee,
                                providerCharge =
                                    providerCharge,
                                customerPaid =
                                    customerPaid,
                                providerCost =
                                    providerCost,
                                sourceAmount =
                                    calculatedSourceAmount,
                                destinationAmount =
                                    calculatedDestinationAmount,
                                profit =
                                    calculatedProfit,
                                note =
                                    item.optString(
                                        "note",
                                        ""
                                    ),
                                workspace =
                                    workspace,
                                businessId = sourceAccount.businessId,
                                createdAt =
                                    item.optLong(
                                        "createdAt",
                                        System.currentTimeMillis()
                                    )
                            )
                    }

                    val serviceEntries =
                        financialAccountEntries
                            .filter {
                                it.entryType ==
                                    "SERVICE_IN" ||
                                    it.entryType ==
                                        "SERVICE_OUT"
                            }

                    require(
                        serviceEntries.size ==
                            digitalServiceTransactions
                                .size * 2
                    ) {
                        "Service account entry সংখ্যা সঠিক নয়"
                    }

                    digitalServiceTransactions
                        .forEach { service ->
                            val sourceKey =
                                "DIGITAL_SERVICE:" +
                                    "${service.eventKey}:SOURCE"

                            val destinationKey =
                                "DIGITAL_SERVICE:" +
                                    "${service.eventKey}:DESTINATION"

                            val sourceEntry =
                                serviceEntries
                                    .singleOrNull {
                                        it.sourceKey ==
                                            sourceKey
                                    }

                            val destinationEntry =
                                serviceEntries
                                    .singleOrNull {
                                        it.sourceKey ==
                                            destinationKey
                                    }

                            require(
                                sourceEntry != null &&
                                    destinationEntry != null &&
                                    sourceEntry.entryType ==
                                        "SERVICE_OUT" &&
                                    destinationEntry.entryType ==
                                        "SERVICE_IN" &&
                                    sourceEntry.accountId ==
                                        service.sourceAccountId &&
                                    destinationEntry.accountId ==
                                        service.destinationAccountId &&
                                    sourceEntry.relatedAccountId ==
                                        service.destinationAccountId &&
                                    destinationEntry.relatedAccountId ==
                                        service.sourceAccountId &&
                                    kotlin.math.abs(
                                        sourceEntry.amount -
                                            service.sourceAmount
                                    ) < 0.0001 &&
                                    kotlin.math.abs(
                                        destinationEntry.amount -
                                            service.destinationAmount
                                    ) < 0.0001 &&
                                    sourceEntry.workspace ==
                                        service.workspace &&
                                    destinationEntry.workspace ==
                                        service.workspace
                            ) {
                                "Service account movement সঠিক নয়"
                            }

                            val profitKey =
                                "DIGITAL_SERVICE:" +
                                    "${service.eventKey}:PROFIT"

                            val profitTransaction =
                                transactions
                                    .singleOrNull {
                                        it.sourceKey ==
                                            profitKey
                                    }

                            if (
                                kotlin.math.abs(
                                    service.profit
                                ) >= 0.0001
                            ) {
                                require(
                                    profitTransaction !=
                                        null &&
                                        profitTransaction.type ==
                                            if (
                                                service.profit >
                                                    0.0
                                            ) {
                                                "INCOME"
                                            } else {
                                                "EXPENSE"
                                            } &&
                                        kotlin.math.abs(
                                            profitTransaction.amount -
                                                kotlin.math.abs(
                                                    service.profit
                                                )
                                        ) < 0.0001 &&
                                        profitTransaction.workspace ==
                                            service.workspace
                                ) {
                                    "Service profit transaction সঠিক নয়"
                                }
                            } else {
                                require(
                                    profitTransaction ==
                                        null
                                ) {
                                    "Zero-profit service-এ profit transaction থাকা যাবে না"
                                }
                            }
                        }

                    val generatedProfitRows =
                        transactions.filter {
                            it.sourceKey
                                ?.startsWith(
                                    "DIGITAL_SERVICE:"
                                ) == true
                        }

                    val expectedProfitRows =
                        digitalServiceTransactions
                            .count {
                                kotlin.math.abs(
                                    it.profit
                                ) >= 0.0001
                            }

                    require(
                        generatedProfitRows.size ==
                            expectedProfitRows
                    ) {
                        "Service profit row সংখ্যা সঠিক নয়"
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

                val inventoryRetailSales =
                    if (backupVersion >= 12) {
                        InventoryBackupBridge.retailSalesFromJson(
                            root.optJSONArray(
                                "inventoryRetailSales"
                            ) ?: JSONArray()
                        )
                    } else {
                        emptyList()
                    }

                val inventoryRetailSaleLines =
                    if (backupVersion >= 12) {
                        InventoryBackupBridge.retailSaleLinesFromJson(
                            root.optJSONArray(
                                "inventoryRetailSaleLines"
                            ) ?: JSONArray()
                        )
                    } else {
                        emptyList()
                    }

                val inventoryRetailSaleStockAllocations =
                    if (backupVersion >= 12) {
                        InventoryBackupBridge
                            .retailSaleStockAllocationsFromJson(
                                root.optJSONArray(
                                    "inventoryRetailSaleStockAllocations"
                                ) ?: JSONArray()
                            )
                    } else {
                        emptyList()
                    }

                val inventoryRetailSalePayments =
                    if (backupVersion >= 12) {
                        InventoryBackupBridge.retailSalePaymentsFromJson(
                            root.optJSONArray(
                                "inventoryRetailSalePayments"
                            ) ?: JSONArray()
                        )
                    } else {
                        emptyList()
                    }

                val restorableFinancialAccountEntries =
                    if (backupVersion >= 12) {
                        financialAccountEntries.filterNot {
                            it.entryType == "RETAIL_SALE_IN"
                        }
                    } else {
                        financialAccountEntries
                    }

                database.withTransaction {
                    dao.clearBusinessProfiles()
                    restoredBusinessProfiles.forEach { dao.upsertBusinessProfile(it) }
                    dao.clearDigitalServiceTransactions()
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

                    digitalServiceTransactions.forEach {
                        dao.insertDigitalServiceTransaction(
                            it
                        )
                    }

                    restorableFinancialAccountEntries.forEach {
                        dao.insertFinancialAccountEntry(it)
                    }
                }

                _selectedBusinessId.value = restoredSelectedBusinessId
                preferences.edit()
                    .putString("selected_business_id", restoredSelectedBusinessId)
                    .putString("legacy_business_id", restoredSelectedBusinessId)
                    .apply()

                if (backupVersion >= 3) {
                    InventoryBackupBridge.restore(
                        context = getApplication(),
                        products = inventoryProducts,
                        batches = inventoryBatches,
                        unitConversions = inventoryUnits,
                        retailSales = inventoryRetailSales,
                        retailSaleLines =
                            inventoryRetailSaleLines,
                        retailSaleStockAllocations =
                            inventoryRetailSaleStockAllocations,
                        retailSalePayments =
                            inventoryRetailSalePayments
                    )
                }

                if (backupVersion >= 12) {
                    val retailSalesById =
                        inventoryRetailSales.associateBy {
                            it.id
                        }

                    database.withTransaction {
                        inventoryRetailSalePayments.forEach {
                                payment ->

                            val sale =
                                requireNotNull(
                                    retailSalesById[
                                        payment.saleId
                                    ]
                                ) {
                                    "Retail payment sale missing"
                                }

                            if (sale.status != "CANCELLED") {
                                val account =
                                    requireNotNull(
                                        dao.getFinancialAccountOnce(
                                            payment.financialAccountId
                                        )
                                    ) {
                                        "Retail payment account missing"
                                    }

                                require(
                                    account.workspace ==
                                        sale.workspace
                                ) {
                                    "Retail payment account workspace mismatch"
                                }

                                val sourceKey =
                                    "RETAIL_SALE_ACCOUNT:${payment.eventKey}"

                                dao.deleteFinancialAccountEntryBySourceKey(
                                    sourceKey
                                )

                                val inserted =
                                    dao.insertFinancialAccountEntry(
                                        FinancialAccountEntryEntity(
                                            accountId =
                                                payment.financialAccountId,
                                            entryType =
                                                "RETAIL_SALE_IN",
                                            amount =
                                                payment.amount,
                                            balanceDelta =
                                                payment.amount,
                                            sourceKey =
                                                sourceKey,
                                            note =
                                                "Retail sale ${sale.invoiceNo} • ${payment.paymentMethod}",
                                            workspace =
                                                sale.workspace,
                                            businessId = account.businessId,
                                            createdAt =
                                                payment.paidAt
                                        )
                                    )

                                require(inserted > 0L) {
                                    "Retail projection restore failed"
                                }
                            }
                        }
                    }
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

                val restoredLegacyBusinessKey =
                    businessDataKey(
                        root.optJSONObject("settings")
                            ?.optString(
                                "businessType",
                                ""
                            )
                            .orEmpty()
                    )

                val restoredInventoryDao =
                    InventoryDatabase
                        .get(getApplication())
                        .dao()

                restoredInventoryDao
                    .claimExistingBusinessProducts(
                        workspace = "SHOP",
                        legacyBusinessKey =
                            restoredLegacyBusinessKey,
                        targetBusinessId =
                            restoredSelectedBusinessId
                    )

                restoredInventoryDao
                    .claimExistingBusinessRetailSales(
                        workspace = "SHOP",
                        legacyBusinessKey =
                            restoredLegacyBusinessKey,
                        targetBusinessId =
                            restoredSelectedBusinessId
                    )

                V15BusinessBackupBridge
                    .scopeLegacyShopWorkspace(
                        context = getApplication(),
                        scopedWorkspace =
                            businessWorkspaceKey(
                                "SHOP",
                                restoredSelectedBusinessId
                            )
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
                    restorableFinancialAccountEntries.size +
                    digitalServiceTransactions.size +
                    inventoryProducts.size +
                    inventoryBatches.size +
                    inventoryUnits.size +
                    inventoryRetailSales.size +
                    inventoryRetailSaleLines.size +
                    inventoryRetailSaleStockAllocations.size +
                    inventoryRetailSalePayments.size +
                    businessRowsRestored
            }.onSuccess(onDone).onFailure {
                onError(it.message ?: "ব্যাকআপ রিস্টোর করা যায়নি")
            }
        }
    }

}
