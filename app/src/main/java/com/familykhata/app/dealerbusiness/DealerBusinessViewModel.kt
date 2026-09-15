package com.familykhata.app.dealerbusiness

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.familykhata.app.businessDataKey
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.data.ProductEntity
import com.familykhata.app.data.StockBatchEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DealerPurchaseLineInput(
    val productId: Long,
    val quantity: Int,
    val unitCost: Double,
    val batchNo: String = ""
)

data class DealerSaleLineInput(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class DealerBusinessViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        InventoryDatabase.get(application)

    private val dao =
        database.dealerBusinessDao()

    private val inventoryDao =
        database.dao()

    private val workspace =
        MutableStateFlow("SHOP")

    private val businessKey =
        MutableStateFlow("dealer_business")

    val companies: StateFlow<List<DealerCompanyEntity>> =
        workspace
            .flatMapLatest {
                dao.observeCompanies(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val areas: StateFlow<List<DealerAreaEntity>> =
        workspace
            .flatMapLatest {
                dao.observeAreas(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val customers: StateFlow<List<DealerCustomerEntity>> =
        workspace
            .flatMapLatest {
                dao.observeCustomers(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val purchases: StateFlow<List<DealerPurchaseEntity>> =
        workspace
            .flatMapLatest {
                dao.observePurchases(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val sales: StateFlow<List<DealerSaleEntity>> =
        workspace
            .flatMapLatest {
                dao.observeSales(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val expenses: StateFlow<List<DealerExpenseEntity>> =
        workspace
            .flatMapLatest {
                dao.observeExpenses(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val collections:
        StateFlow<List<DealerCollectionEntity>> =
        workspace
            .flatMapLatest {
                dao.observeAllCollections(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val supplierPayments:
        StateFlow<List<DealerSupplierPaymentEntity>> =
        workspace
            .flatMapLatest {
                dao.observeAllSupplierPayments(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val products: StateFlow<List<ProductEntity>> =
        workspace
            .flatMapLatest { workspaceValue ->
                inventoryDao.observeProductsForBusiness(
                    workspace = workspaceValue,
                    businessKey = businessKey.value
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun setBusinessContext(
        workspaceValue: String,
        shopType: String
    ) {
        workspace.value = workspaceValue

        val key = businessDataKey(shopType)

        businessKey.value =
            if (key == "dealer_business") {
                key
            } else {
                "dealer_business"
            }
    }

    fun observePurchaseLines(
        purchaseId: Long
    ): Flow<List<DealerPurchaseLineEntity>> =
        dao.observePurchaseLines(purchaseId)

    fun observeSaleLines(
        saleId: Long
    ): Flow<List<DealerSaleLineEntity>> =
        dao.observeSaleLines(saleId)

    fun observeCollections(
        customerId: Long
    ): Flow<List<DealerCollectionEntity>> =
        dao.observeCollections(customerId)

    fun observeSupplierPayments(
        companyId: Long
    ): Flow<List<DealerSupplierPaymentEntity>> =
        dao.observeSupplierPayments(companyId)

    fun observeSalesReturns(
        saleId: Long
    ): Flow<List<DealerSalesReturnEntity>> =
        dao.observeSalesReturns(saleId)

    fun observePurchaseReturns(
        purchaseId: Long
    ): Flow<List<DealerPurchaseReturnEntity>> =
        dao.observePurchaseReturns(purchaseId)

    fun addCompany(
        name: String,
        code: String,
        phone: String,
        contactPerson: String,
        address: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertCompany(
                        DealerCompanyEntity(
                            name = name.trim(),
                            code = code.trim(),
                            phone = phone.trim(),
                            contactPerson =
                                contactPerson.trim(),
                            address = address.trim(),
                            note = note.trim(),
                            workspace = currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateCompany(
        item: DealerCompanyEntity,
        name: String,
        code: String,
        phone: String,
        contactPerson: String,
        address: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            name.isBlank() ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateCompany(
                        item.copy(
                            name = name.trim(),
                            code = code.trim(),
                            phone = phone.trim(),
                            contactPerson =
                                contactPerson.trim(),
                            address = address.trim(),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun addArea(
        name: String,
        code: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertArea(
                        DealerAreaEntity(
                            name = name.trim(),
                            code = code.trim(),
                            note = note.trim(),
                            workspace = currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateArea(
        item: DealerAreaEntity,
        name: String,
        code: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            name.isBlank() ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateArea(
                        item.copy(
                            name = name.trim(),
                            code = code.trim(),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun addCustomer(
        areaId: Long?,
        name: String,
        customerCode: String,
        phone: String,
        address: String,
        creditLimit: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    if (areaId != null) {
                        val area =
                            dao.getAreaOnce(areaId)

                        require(
                            area != null &&
                                area.workspace ==
                                    currentWorkspace
                        )
                    }

                    dao.insertCustomer(
                        DealerCustomerEntity(
                            areaId = areaId,
                            name = name.trim(),
                            customerCode =
                                customerCode.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            creditLimit =
                                creditLimit.coerceAtLeast(0.0),
                            note = note.trim(),
                            workspace = currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateCustomer(
        item: DealerCustomerEntity,
        areaId: Long?,
        name: String,
        customerCode: String,
        phone: String,
        address: String,
        creditLimit: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            name.isBlank() ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    if (areaId != null) {
                        val area =
                            dao.getAreaOnce(areaId)

                        require(
                            area != null &&
                                area.workspace ==
                                    currentWorkspace
                        )
                    }

                    dao.updateCustomer(
                        item.copy(
                            areaId = areaId,
                            name = name.trim(),
                            customerCode =
                                customerCode.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            creditLimit =
                                creditLimit.coerceAtLeast(0.0),
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun addExpense(
        category: String,
        amount: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            category.isBlank() ||
            amount <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.insertExpense(
                        DealerExpenseEntity(
                            category = category.trim(),
                            amount = amount,
                            note = note.trim(),
                            workspace = currentWorkspace
                        )
                    ) > 0
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateExpense(
        item: DealerExpenseEntity,
        category: String,
        amount: Double,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            category.isBlank() ||
            amount <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateExpense(
                        item.copy(
                            category = category.trim(),
                            amount = amount,
                            note = note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    private suspend fun refreshSaleStatus(
        saleId: Long
    ) {
        val total =
            dao.getSaleTotal(saleId)

        val returned =
            dao.getSaleReturnTotal(saleId)

        val collected =
            dao.getSaleCollected(saleId)

        val net =
            (
                total -
                    returned
            ).coerceAtLeast(0.0)

        val due =
            (
                net -
                    collected
            ).coerceAtLeast(0.0)

        dao.updateSaleStatus(
            saleId,
            when {
                due <= 0.0001 ->
                    "PAID"

                collected > 0.0001 ->
                    "PARTIAL"

                else ->
                    "OPEN"
            }
        )
    }

    private suspend fun refreshPurchaseStatus(
        purchaseId: Long
    ) {
        val total =
            dao.getPurchaseTotal(purchaseId)

        val returned =
            dao.getPurchaseReturnTotal(
                purchaseId
            )

        val paid =
            dao.getPurchasePaid(purchaseId)

        val net =
            (
                total -
                    returned
            ).coerceAtLeast(0.0)

        val due =
            (
                net -
                    paid
            ).coerceAtLeast(0.0)

        dao.updatePurchaseStatus(
            purchaseId,
            when {
                due <= 0.0001 ->
                    "PAID"

                paid > 0.0001 ->
                    "PARTIAL"

                else ->
                    "OPEN"
            }
        )
    }

    private suspend fun allocateCollectionAmount(
        collectionId: Long,
        customerId: Long,
        amount: Double,
        workspaceValue: String
    ) {
        var remaining =
            amount.coerceAtLeast(0.0)

        val sales =
            dao.getSalesForCustomerOnce(
                customerId,
                workspaceValue
            )

        for (sale in sales) {
            if (remaining <= 0.0001) break

            val total =
                dao.getSaleTotal(sale.id)

            val returned =
                dao.getSaleReturnTotal(
                    sale.id
                )

            val collected =
                dao.getSaleCollected(
                    sale.id
                )

            val due =
                (
                    total -
                        returned -
                        collected
                ).coerceAtLeast(0.0)

            val use =
                minOf(
                    due,
                    remaining
                )

            if (use > 0.0001) {
                dao.insertCollectionAllocation(
                    DealerCollectionAllocationEntity(
                        collectionId =
                            collectionId,
                        saleId =
                            sale.id,
                        amount =
                            use
                    )
                )

                remaining -= use

                refreshSaleStatus(
                    sale.id
                )
            }
        }

        // Remaining money intentionally stays
        // unallocated as customer advance.
    }

    private suspend fun allocateSupplierPaymentAmount(
        paymentId: Long,
        companyId: Long,
        amount: Double,
        workspaceValue: String
    ) {
        var remaining =
            amount.coerceAtLeast(0.0)

        val purchases =
            dao.getPurchasesForCompanyOnce(
                companyId,
                workspaceValue
            )

        for (purchase in purchases) {
            if (remaining <= 0.0001) break

            val total =
                dao.getPurchaseTotal(
                    purchase.id
                )

            val returned =
                dao.getPurchaseReturnTotal(
                    purchase.id
                )

            val paid =
                dao.getPurchasePaid(
                    purchase.id
                )

            val due =
                (
                    total -
                        returned -
                        paid
                ).coerceAtLeast(0.0)

            val use =
                minOf(
                    due,
                    remaining
                )

            if (use > 0.0001) {
                dao.insertSupplierPaymentAllocation(
                    DealerSupplierPaymentAllocationEntity(
                        paymentId =
                            paymentId,
                        purchaseId =
                            purchase.id,
                        amount =
                            use
                    )
                )

                remaining -= use

                refreshPurchaseStatus(
                    purchase.id
                )
            }
        }

        // Remaining money intentionally stays
        // unallocated as supplier advance.
    }

    fun updatePurchaseMeta(
        item: DealerPurchaseEntity,
        invoiceNo: String,
        purchasedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updatePurchase(
                        item.copy(
                            invoiceNo =
                                invoiceNo.trim(),
                            purchasedAt =
                                purchasedAt,
                            note =
                                note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updateSaleMeta(
        item: DealerSaleEntity,
        invoiceNo: String,
        soldAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateSale(
                        item.copy(
                            invoiceNo =
                                invoiceNo.trim(),
                            soldAt =
                                soldAt,
                            note =
                                note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun correctCollection(
        item: DealerCollectionEntity,
        customerId: Long,
        amount: Double,
        collectedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            customerId <= 0 ||
            amount <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val customer =
                            requireNotNull(
                                dao.getCustomerOnce(
                                    customerId
                                )
                            )

                        require(
                            customer.workspace ==
                                currentWorkspace
                        )

                        val oldAllocations =
                            dao.getCollectionAllocationsForCollectionOnce(
                                item.id
                            )

                        dao.deleteCollectionAllocationsByCollection(
                            item.id
                        )

                        oldAllocations
                            .map {
                                it.saleId
                            }
                            .distinct()
                            .forEach {
                                refreshSaleStatus(it)
                            }

                        require(
                            dao.updateCollection(
                                item.copy(
                                    customerId =
                                        customer.id,
                                    customerNameSnapshot =
                                        customer.name,
                                    amount =
                                        amount,
                                    collectedAt =
                                        collectedAt,
                                    note =
                                        note.trim()
                                )
                            ) == 1
                        )

                        allocateCollectionAmount(
                            collectionId =
                                item.id,
                            customerId =
                                customer.id,
                            amount =
                                amount,
                            workspaceValue =
                                currentWorkspace
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun correctSupplierPayment(
        item: DealerSupplierPaymentEntity,
        companyId: Long,
        amount: Double,
        paidAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            companyId <= 0 ||
            amount <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val company =
                            requireNotNull(
                                dao.getCompanyOnce(
                                    companyId
                                )
                            )

                        require(
                            company.workspace ==
                                currentWorkspace
                        )

                        val oldAllocations =
                            dao.getSupplierPaymentAllocationsForPaymentOnce(
                                item.id
                            )

                        dao.deleteSupplierPaymentAllocationsByPayment(
                            item.id
                        )

                        oldAllocations
                            .map {
                                it.purchaseId
                            }
                            .distinct()
                            .forEach {
                                refreshPurchaseStatus(
                                    it
                                )
                            }

                        require(
                            dao.updateSupplierPayment(
                                item.copy(
                                    companyId =
                                        company.id,
                                    companyNameSnapshot =
                                        company.name,
                                    amount =
                                        amount,
                                    paidAt =
                                        paidAt,
                                    note =
                                        note.trim()
                                )
                            ) == 1
                        )

                        allocateSupplierPaymentAmount(
                            paymentId =
                                item.id,
                            companyId =
                                company.id,
                            amount =
                                amount,
                            workspaceValue =
                                currentWorkspace
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun updateSalesReturnMeta(
        item: DealerSalesReturnEntity,
        returnedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updateSalesReturn(
                        item.copy(
                            returnedAt =
                                returnedAt,
                            note =
                                note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun updatePurchaseReturnMeta(
        item: DealerPurchaseReturnEntity,
        returnedAt: Long,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            item.id <= 0 ||
            item.workspace != workspace.value
        ) {
            onDone(false)
            return
        }

        viewModelScope.launch {
            val success =
                runCatching {
                    dao.updatePurchaseReturn(
                        item.copy(
                            returnedAt =
                                returnedAt,
                            note =
                                note.trim()
                        )
                    ) == 1
                }.getOrDefault(false)

            onDone(success)
        }
    }

    fun createPurchase(
        companyId: Long,
        invoiceNo: String,
        lines: List<DealerPurchaseLineInput>,
        paidNow: Double = 0.0,
        note: String = "",
        purchasedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanLines =
            lines.filter {
                it.productId > 0 &&
                    it.quantity > 0 &&
                    it.unitCost >= 0
            }

        if (
            companyId <= 0 ||
            cleanLines.isEmpty() ||
            paidNow < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value
        val currentBusinessKey = businessKey.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val company =
                            requireNotNull(
                                dao.getCompanyOnce(companyId)
                            )

                        require(
                            company.workspace ==
                                currentWorkspace
                        )

                        val resolved =
                            cleanLines.map { input ->
                                val product =
                                    requireNotNull(
                                        inventoryDao.getProductOnce(
                                            input.productId
                                        )
                                    )

                                require(
                                    product.workspace ==
                                        currentWorkspace
                                )

                                require(
                                    product.businessKey ==
                                        currentBusinessKey
                                )

                                input to product
                            }

                        val total =
                            resolved.sumOf {
                                it.first.quantity *
                                    it.first.unitCost
                            }

                        val purchaseId =
                            dao.insertPurchase(
                                DealerPurchaseEntity(
                                    companyId = company.id,
                                    companyNameSnapshot =
                                        company.name,
                                    invoiceNo =
                                        invoiceNo.trim(),
                                    purchasedAt =
                                        purchasedAt,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        resolved.forEach {
                            val input = it.first
                            val product = it.second

                            val batchId =
                                inventoryDao.insertBatch(
                                    StockBatchEntity(
                                        productId =
                                            product.id,
                                        batchNo =
                                            input.batchNo.trim(),
                                        quantity =
                                            input.quantity,
                                        purchasePrice =
                                            input.unitCost,
                                        purchaseDate =
                                            purchasedAt
                                    )
                                )

                            dao.insertPurchaseLine(
                                DealerPurchaseLineEntity(
                                    purchaseId =
                                        purchaseId,
                                    productId =
                                        product.id,
                                    inventoryBatchId =
                                        batchId,
                                    productNameSnapshot =
                                        product.name,
                                    batchNoSnapshot =
                                        input.batchNo.trim(),
                                    quantity =
                                        input.quantity,
                                    unitCost =
                                        input.unitCost,
                                    lineTotal =
                                        input.quantity *
                                            input.unitCost
                                )
                            )
                        }

                        var due = total

                        val oldPayments =
                            dao.getSupplierPaymentsForCompanyOnce(
                                company.id,
                                currentWorkspace
                            )

                        for (payment in oldPayments) {
                            if (due <= 0.0001) break

                            val allocated =
                                dao.getSupplierPaymentAllocated(
                                    payment.id
                                )

                            val free =
                                (
                                    payment.amount -
                                        allocated
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    free,
                                    due
                                )

                            if (use > 0.0001) {
                                dao.insertSupplierPaymentAllocation(
                                    DealerSupplierPaymentAllocationEntity(
                                        paymentId =
                                            payment.id,
                                        purchaseId =
                                            purchaseId,
                                        amount = use
                                    )
                                )

                                due -= use
                            }
                        }

                        if (paidNow > 0.0001) {
                            val paymentId =
                                dao.insertSupplierPayment(
                                    DealerSupplierPaymentEntity(
                                        companyId =
                                            company.id,
                                        companyNameSnapshot =
                                            company.name,
                                        amount =
                                            paidNow,
                                        paidAt =
                                            purchasedAt,
                                        workspace =
                                            currentWorkspace
                                    )
                                )

                            val use =
                                minOf(
                                    paidNow,
                                    due.coerceAtLeast(0.0)
                                )

                            if (use > 0.0001) {
                                dao.insertSupplierPaymentAllocation(
                                    DealerSupplierPaymentAllocationEntity(
                                        paymentId =
                                            paymentId,
                                        purchaseId =
                                            purchaseId,
                                        amount =
                                            use
                                    )
                                )

                                due -= use
                            }
                        }

                        val status =
                            when {
                                due <= 0.0001 ->
                                    "PAID"

                                due < total - 0.0001 ->
                                    "PARTIAL"

                                else ->
                                    "OPEN"
                            }

                        dao.updatePurchaseStatus(
                            purchaseId,
                            status
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun createSale(
        customerId: Long,
        invoiceNo: String,
        lines: List<DealerSaleLineInput>,
        collectedNow: Double = 0.0,
        note: String = "",
        soldAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanLines =
            lines.filter {
                it.productId > 0 &&
                    it.quantity > 0 &&
                    it.unitPrice >= 0
            }

        if (
            customerId <= 0 ||
            cleanLines.isEmpty() ||
            collectedNow < 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value
        val currentBusinessKey = businessKey.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val customer =
                            requireNotNull(
                                dao.getCustomerOnce(customerId)
                            )

                        require(
                            customer.workspace ==
                                currentWorkspace
                        )

                        val resolved =
                            cleanLines.map { input ->
                                val product =
                                    requireNotNull(
                                        inventoryDao.getProductOnce(
                                            input.productId
                                        )
                                    )

                                require(
                                    product.workspace ==
                                        currentWorkspace
                                )

                                require(
                                    product.businessKey ==
                                        currentBusinessKey
                                )

                                val batches =
                                    inventoryDao
                                        .getBatchesOnce(product.id)
                                        .filter {
                                            it.quantity > 0
                                        }

                                require(
                                    batches.sumOf {
                                        it.quantity
                                    } >=
                                        input.quantity
                                )

                                Triple(
                                    input,
                                    product,
                                    batches
                                )
                            }

                        val total =
                            resolved.sumOf {
                                it.first.quantity *
                                    it.first.unitPrice
                            }

                        val existingSales =
                            dao.getSalesForCustomerOnce(
                                customer.id,
                                currentWorkspace
                            )

                        var existingDue = 0.0

                        for (sale in existingSales) {
                            val saleTotal =
                                dao.getSaleTotal(sale.id)

                            val returns =
                                dao.getSaleReturnTotal(
                                    sale.id
                                )

                            val collected =
                                dao.getSaleCollected(
                                    sale.id
                                )

                            existingDue +=
                                (
                                    saleTotal -
                                        returns -
                                        collected
                                ).coerceAtLeast(0.0)
                        }

                        var availableAdvance = 0.0

                        val oldCollections =
                            dao.getCollectionsForCustomerOnce(
                                customer.id,
                                currentWorkspace
                            )

                        for (collection in oldCollections) {
                            val allocated =
                                dao.getCollectionAllocated(
                                    collection.id
                                )

                            availableAdvance +=
                                (
                                    collection.amount -
                                        allocated
                                ).coerceAtLeast(0.0)
                        }

                        val newDueAfterKnownMoney =
                            (
                                total -
                                    availableAdvance -
                                    collectedNow
                            ).coerceAtLeast(0.0)

                        if (customer.creditLimit > 0.0) {
                            require(
                                existingDue +
                                    newDueAfterKnownMoney <=
                                    customer.creditLimit +
                                        0.0001
                            )
                        }

                        val saleId =
                            dao.insertSale(
                                DealerSaleEntity(
                                    customerId =
                                        customer.id,
                                    customerNameSnapshot =
                                        customer.name,
                                    invoiceNo =
                                        invoiceNo.trim(),
                                    soldAt =
                                        soldAt,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        for (
                            resolvedLine in resolved
                        ) {
                            val input =
                                resolvedLine.first

                            val product =
                                resolvedLine.second

                            val batches =
                                resolvedLine.third

                            val saleLineId =
                                dao.insertSaleLine(
                                    DealerSaleLineEntity(
                                        saleId =
                                            saleId,
                                        productId =
                                            product.id,
                                        productNameSnapshot =
                                            product.name,
                                        quantity =
                                            input.quantity,
                                        unitPrice =
                                            input.unitPrice,
                                        lineTotal =
                                            input.quantity *
                                                input.unitPrice
                                    )
                                )

                            var remaining =
                                input.quantity

                            for (batch in batches) {
                                if (remaining <= 0) break

                                val used =
                                    minOf(
                                        batch.quantity,
                                        remaining
                                    )

                                dao.insertStockAllocation(
                                    DealerStockAllocationEntity(
                                        saleLineId =
                                            saleLineId,
                                        sourceStockBatchId =
                                            batch.id,
                                        batchNoSnapshot =
                                            batch.batchNo,
                                        quantity =
                                            used,
                                        unitCost =
                                            batch.purchasePrice,
                                        totalCost =
                                            used *
                                                batch.purchasePrice
                                    )
                                )

                                inventoryDao.updateBatchQuantity(
                                    batch.id,
                                    batch.quantity -
                                        used
                                )

                                remaining -= used
                            }

                            require(remaining == 0)
                        }

                        var due = total

                        for (collection in oldCollections) {
                            if (due <= 0.0001) break

                            val allocated =
                                dao.getCollectionAllocated(
                                    collection.id
                                )

                            val free =
                                (
                                    collection.amount -
                                        allocated
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    free,
                                    due
                                )

                            if (use > 0.0001) {
                                dao.insertCollectionAllocation(
                                    DealerCollectionAllocationEntity(
                                        collectionId =
                                            collection.id,
                                        saleId =
                                            saleId,
                                        amount =
                                            use
                                    )
                                )

                                due -= use
                            }
                        }

                        if (collectedNow > 0.0001) {
                            val collectionId =
                                dao.insertCollection(
                                    DealerCollectionEntity(
                                        customerId =
                                            customer.id,
                                        customerNameSnapshot =
                                            customer.name,
                                        amount =
                                            collectedNow,
                                        collectedAt =
                                            soldAt,
                                        workspace =
                                            currentWorkspace
                                    )
                                )

                            val use =
                                minOf(
                                    collectedNow,
                                    due.coerceAtLeast(0.0)
                                )

                            if (use > 0.0001) {
                                dao.insertCollectionAllocation(
                                    DealerCollectionAllocationEntity(
                                        collectionId =
                                            collectionId,
                                        saleId =
                                            saleId,
                                        amount =
                                            use
                                    )
                                )

                                due -= use
                            }
                        }

                        val status =
                            when {
                                due <= 0.0001 ->
                                    "PAID"

                                due < total - 0.0001 ->
                                    "PARTIAL"

                                else ->
                                    "OPEN"
                            }

                        dao.updateSaleStatus(
                            saleId,
                            status
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }


    private suspend fun rebalanceSaleAfterReturn(
        saleId: Long
    ) {
        val originalTotal =
            dao.getSaleTotal(saleId)

        val returnTotal =
            dao.getSaleReturnTotal(saleId)

        val netTotal =
            (
                originalTotal -
                    returnTotal
            ).coerceAtLeast(0.0)

        var allocated =
            dao.getSaleCollected(saleId)

        var excess =
            (
                allocated -
                    netTotal
            ).coerceAtLeast(0.0)

        if (excess > 0.0001) {
            val allocations =
                dao.getCollectionAllocationsForSaleOnce(
                    saleId
                )

            for (allocation in allocations) {
                if (excess <= 0.0001) break

                val release =
                    minOf(
                        allocation.amount,
                        excess
                    )

                val remaining =
                    allocation.amount -
                        release

                if (remaining <= 0.0001) {
                    dao.deleteCollectionAllocationById(
                        allocation.id
                    )
                } else {
                    dao.updateCollectionAllocationAmount(
                        allocation.id,
                        remaining
                    )
                }

                excess -= release
            }

            allocated =
                dao.getSaleCollected(saleId)
        }

        val due =
            (
                netTotal -
                    allocated
            ).coerceAtLeast(0.0)

        val status =
            when {
                due <= 0.0001 ->
                    "PAID"

                allocated > 0.0001 ->
                    "PARTIAL"

                else ->
                    "OPEN"
            }

        dao.updateSaleStatus(
            saleId,
            status
        )
    }

    private suspend fun rebalancePurchaseAfterReturn(
        purchaseId: Long
    ) {
        val originalTotal =
            dao.getPurchaseTotal(purchaseId)

        val returnTotal =
            dao.getPurchaseReturnTotal(
                purchaseId
            )

        val netTotal =
            (
                originalTotal -
                    returnTotal
            ).coerceAtLeast(0.0)

        var allocated =
            dao.getPurchasePaid(purchaseId)

        var excess =
            (
                allocated -
                    netTotal
            ).coerceAtLeast(0.0)

        if (excess > 0.0001) {
            val allocations =
                dao.getSupplierPaymentAllocationsForPurchaseOnce(
                    purchaseId
                )

            for (allocation in allocations) {
                if (excess <= 0.0001) break

                val release =
                    minOf(
                        allocation.amount,
                        excess
                    )

                val remaining =
                    allocation.amount -
                        release

                if (remaining <= 0.0001) {
                    dao.deleteSupplierPaymentAllocationById(
                        allocation.id
                    )
                } else {
                    dao.updateSupplierPaymentAllocationAmount(
                        allocation.id,
                        remaining
                    )
                }

                excess -= release
            }

            allocated =
                dao.getPurchasePaid(purchaseId)
        }

        val due =
            (
                netTotal -
                    allocated
            ).coerceAtLeast(0.0)

        val status =
            when {
                due <= 0.0001 ->
                    "PAID"

                allocated > 0.0001 ->
                    "PARTIAL"

                else ->
                    "OPEN"
            }

        dao.updatePurchaseStatus(
            purchaseId,
            status
        )
    }

    fun recordSalesReturn(
        saleLineId: Long,
        quantity: Int,
        returnType: String,
        note: String = "",
        returnedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        val cleanType =
            returnType
                .trim()
                .uppercase()

        if (
            saleLineId <= 0 ||
            quantity <= 0 ||
            cleanType !in
                setOf(
                    "RESTOCK",
                    "DAMAGED"
                )
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val line =
                            requireNotNull(
                                dao.getSaleLineOnce(
                                    saleLineId
                                )
                            )

                        val sale =
                            requireNotNull(
                                dao.getSaleOnce(
                                    line.saleId
                                )
                            )

                        require(
                            sale.workspace ==
                                currentWorkspace
                        )

                        val alreadyReturned =
                            dao.getReturnedSaleQuantity(
                                line.id
                            )

                        require(
                            alreadyReturned +
                                quantity <=
                                line.quantity
                        )

                        val originalAllocations =
                            dao.getStockAllocationsOnce(
                                line.id
                            )

                        data class ReturnSlice(
                            val allocation:
                                DealerStockAllocationEntity,
                            val quantity: Int
                        )

                        val slices =
                            mutableListOf<ReturnSlice>()

                        var remaining =
                            quantity

                        for (
                            allocation in
                            originalAllocations
                        ) {
                            if (remaining <= 0) break

                            val usedBefore =
                                dao.getReturnedAllocationQuantity(
                                    saleLineId =
                                        line.id,
                                    saleAllocationId =
                                        allocation.id
                                )

                            val available =
                                (
                                    allocation.quantity -
                                        usedBefore
                                ).coerceAtLeast(0)

                            val use =
                                minOf(
                                    available,
                                    remaining
                                )

                            if (use > 0) {
                                slices +=
                                    ReturnSlice(
                                        allocation =
                                            allocation,
                                        quantity =
                                            use
                                    )

                                remaining -= use
                            }
                        }

                        require(remaining == 0)

                        val totalCost =
                            slices.sumOf {
                                it.quantity *
                                    it.allocation.unitCost
                            }

                        val returnId =
                            dao.insertSalesReturn(
                                DealerSalesReturnEntity(
                                    saleId =
                                        sale.id,
                                    saleLineId =
                                        line.id,
                                    productId =
                                        line.productId,
                                    productNameSnapshot =
                                        line.productNameSnapshot,
                                    quantity =
                                        quantity,
                                    unitPrice =
                                        line.unitPrice,
                                    totalRefund =
                                        quantity *
                                            line.unitPrice,
                                    totalCost =
                                        totalCost,
                                    returnType =
                                        cleanType,
                                    returnedAt =
                                        returnedAt,
                                    note =
                                        note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        for (slice in slices) {
                            val allocation =
                                slice.allocation

                            var restoredBatchId =
                                allocation
                                    .sourceStockBatchId

                            if (
                                cleanType ==
                                    "RESTOCK"
                            ) {
                                val productId =
                                    requireNotNull(
                                        line.productId
                                    )

                                val currentBatch =
                                    allocation
                                        .sourceStockBatchId
                                        ?.let {
                                            inventoryDao
                                                .getBatchOnce(it)
                                        }

                                restoredBatchId =
                                    if (
                                        currentBatch != null
                                    ) {
                                        inventoryDao
                                            .updateBatchQuantity(
                                                currentBatch.id,
                                                currentBatch.quantity +
                                                    slice.quantity
                                            )

                                        currentBatch.id
                                    } else {
                                        inventoryDao.insertBatch(
                                            StockBatchEntity(
                                                productId =
                                                    productId,
                                                batchNo =
                                                    allocation
                                                        .batchNoSnapshot,
                                                quantity =
                                                    slice.quantity,
                                                purchasePrice =
                                                    allocation
                                                        .unitCost,
                                                purchaseDate =
                                                    returnedAt
                                            )
                                        )
                                    }
                            }

                            dao.insertSalesReturnAllocation(
                                DealerSalesReturnAllocationEntity(
                                    returnId =
                                        returnId,
                                    saleAllocationId =
                                        allocation.id,
                                    sourceStockBatchId =
                                        restoredBatchId,
                                    batchNoSnapshot =
                                        allocation
                                            .batchNoSnapshot,
                                    quantity =
                                        slice.quantity,
                                    unitCost =
                                        allocation.unitCost,
                                    totalCost =
                                        slice.quantity *
                                            allocation.unitCost
                                )
                            )
                        }

                        rebalanceSaleAfterReturn(
                            sale.id
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun recordPurchaseReturn(
        purchaseLineId: Long,
        quantity: Int,
        note: String = "",
        returnedAt: Long =
            System.currentTimeMillis(),
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            purchaseLineId <= 0 ||
            quantity <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace =
            workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val line =
                            requireNotNull(
                                dao.getPurchaseLineOnce(
                                    purchaseLineId
                                )
                            )

                        val purchase =
                            requireNotNull(
                                dao.getPurchaseOnce(
                                    line.purchaseId
                                )
                            )

                        require(
                            purchase.workspace ==
                                currentWorkspace
                        )

                        val returnedBefore =
                            dao.getReturnedPurchaseQuantity(
                                line.id
                            )

                        require(
                            returnedBefore +
                                quantity <=
                                line.quantity
                        )

                        val batchId =
                            requireNotNull(
                                line.inventoryBatchId
                            )

                        val batch =
                            requireNotNull(
                                inventoryDao
                                    .getBatchOnce(batchId)
                            )

                        require(
                            batch.quantity >= quantity
                        )

                        inventoryDao.updateBatchQuantity(
                            batch.id,
                            batch.quantity -
                                quantity
                        )

                        dao.insertPurchaseReturn(
                            DealerPurchaseReturnEntity(
                                purchaseId =
                                    purchase.id,
                                purchaseLineId =
                                    line.id,
                                productId =
                                    line.productId,
                                productNameSnapshot =
                                    line.productNameSnapshot,
                                quantity =
                                    quantity,
                                unitCost =
                                    line.unitCost,
                                totalValue =
                                    quantity *
                                        line.unitCost,
                                returnedAt =
                                    returnedAt,
                                note =
                                    note.trim(),
                                workspace =
                                    currentWorkspace
                            )
                        )

                        rebalancePurchaseAfterReturn(
                            purchase.id
                        )
                    }
                }.isSuccess

            onDone(success)
        }
    }


    fun addCollection(
        customerId: Long,
        amount: Double,
        collectedAt: Long =
            System.currentTimeMillis(),
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            customerId <= 0 ||
            amount <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val customer =
                            requireNotNull(
                                dao.getCustomerOnce(customerId)
                            )

                        require(
                            customer.workspace ==
                                currentWorkspace
                        )

                        val collectionId =
                            dao.insertCollection(
                                DealerCollectionEntity(
                                    customerId =
                                        customer.id,
                                    customerNameSnapshot =
                                        customer.name,
                                    amount = amount,
                                    collectedAt =
                                        collectedAt,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        var remaining = amount

                        val customerSales =
                            dao.getSalesForCustomerOnce(
                                customer.id,
                                currentWorkspace
                            )

                        for (sale in customerSales) {
                            if (remaining <= 0.0001) break

                            val total =
                                dao.getSaleTotal(sale.id)

                            val returns =
                                dao.getSaleReturnTotal(
                                    sale.id
                                )

                            val collected =
                                dao.getSaleCollected(
                                    sale.id
                                )

                            val due =
                                (
                                    total -
                                        returns -
                                        collected
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    due,
                                    remaining
                                )

                            if (use > 0.0001) {
                                dao.insertCollectionAllocation(
                                    DealerCollectionAllocationEntity(
                                        collectionId =
                                            collectionId,
                                        saleId =
                                            sale.id,
                                        amount =
                                            use
                                    )
                                )

                                remaining -= use

                                val left =
                                    due - use

                                dao.updateSaleStatus(
                                    sale.id,
                                    if (left <= 0.0001) {
                                        "PAID"
                                    } else {
                                        "PARTIAL"
                                    }
                                )
                            }
                        }

                        // Any remaining amount stays unallocated
                        // and therefore becomes customer advance.
                    }
                }.isSuccess

            onDone(success)
        }
    }

    fun addSupplierPayment(
        companyId: Long,
        amount: Double,
        paidAt: Long =
            System.currentTimeMillis(),
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (
            companyId <= 0 ||
            amount <= 0
        ) {
            onDone(false)
            return
        }

        val currentWorkspace = workspace.value

        viewModelScope.launch {
            val success =
                runCatching {
                    database.withTransaction {
                        val company =
                            requireNotNull(
                                dao.getCompanyOnce(companyId)
                            )

                        require(
                            company.workspace ==
                                currentWorkspace
                        )

                        val paymentId =
                            dao.insertSupplierPayment(
                                DealerSupplierPaymentEntity(
                                    companyId =
                                        company.id,
                                    companyNameSnapshot =
                                        company.name,
                                    amount = amount,
                                    paidAt = paidAt,
                                    note = note.trim(),
                                    workspace =
                                        currentWorkspace
                                )
                            )

                        var remaining = amount

                        val purchases =
                            dao.getPurchasesForCompanyOnce(
                                company.id,
                                currentWorkspace
                            )

                        for (purchase in purchases) {
                            if (remaining <= 0.0001) break

                            val total =
                                dao.getPurchaseTotal(
                                    purchase.id
                                )

                            val returns =
                                dao.getPurchaseReturnTotal(
                                    purchase.id
                                )

                            val paid =
                                dao.getPurchasePaid(
                                    purchase.id
                                )

                            val due =
                                (
                                    total -
                                        returns -
                                        paid
                                ).coerceAtLeast(0.0)

                            val use =
                                minOf(
                                    due,
                                    remaining
                                )

                            if (use > 0.0001) {
                                dao.insertSupplierPaymentAllocation(
                                    DealerSupplierPaymentAllocationEntity(
                                        paymentId =
                                            paymentId,
                                        purchaseId =
                                            purchase.id,
                                        amount =
                                            use
                                    )
                                )

                                remaining -= use

                                val left =
                                    due - use

                                dao.updatePurchaseStatus(
                                    purchase.id,
                                    if (left <= 0.0001) {
                                        "PAID"
                                    } else {
                                        "PARTIAL"
                                    }
                                )
                            }
                        }

                        // Any remaining amount stays as
                        // supplier/company advance.
                    }
                }.isSuccess

            onDone(success)
        }
    }
}
