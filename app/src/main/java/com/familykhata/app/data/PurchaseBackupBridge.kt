package com.familykhata.app.data

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

data class PurchaseBackupData(
    val suppliers: List<PurchaseSupplierEntity> = emptyList(),
    val bills: List<PurchaseBillEntity> = emptyList(),
    val lines: List<PurchaseBillLineEntity> = emptyList(),
    val payments: List<PurchasePaymentEntity> = emptyList(),
    val returns: List<PurchaseReturnEntity> = emptyList()
)

object PurchaseBackupBridge {

    suspend fun export(
        context: Context
    ): PurchaseBackupData {
        val dao =
            InventoryDatabase.get(context).dao()

        return PurchaseBackupData(
            suppliers =
                dao.getAllPurchaseSuppliers(),
            bills =
                dao.getAllPurchaseBills(),
            lines =
                dao.getAllPurchaseBillLines(),
            payments =
                dao.getAllPurchasePayments(),
            returns =
                dao.getAllPurchaseReturns()
        )
    }

    fun toJson(
        data: PurchaseBackupData
    ): JSONObject =
        JSONObject().apply {
            put(
                "suppliers",
                JSONArray().apply {
                    data.suppliers.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("id", item.id)
                                put("name", item.name)
                                put("phone", item.phone)
                                put("address", item.address)
                                put("note", item.note)
                                put("workspace", item.workspace)
                                put("businessKey", item.businessKey)
                                put("isActive", item.isActive)
                                put("createdAt", item.createdAt)
                            }
                        )
                    }
                }
            )

            put(
                "bills",
                JSONArray().apply {
                    data.bills.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("id", item.id)
                                put("purchaseNo", item.purchaseNo)
                                put("supplierId", item.supplierId)
                                put("subtotal", item.subtotal)
                                put("discount", item.discount)
                                put("total", item.total)
                                put("status", item.status)
                                put("note", item.note)
                                put("workspace", item.workspace)
                                put("businessKey", item.businessKey)
                                put("purchasedAt", item.purchasedAt)
                                put("createdAt", item.createdAt)
                            }
                        )
                    }
                }
            )

            put(
                "lines",
                JSONArray().apply {
                    data.lines.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("id", item.id)
                                put("billId", item.billId)
                                put("productId", item.productId)

                                item.stockBatchId?.let {
                                    put("stockBatchId", it)
                                }

                                put(
                                    "productNameSnapshot",
                                    item.productNameSnapshot
                                )
                                put("skuSnapshot", item.skuSnapshot)
                                put("unitSnapshot", item.unitSnapshot)
                                put("unitFactor", item.unitFactor)
                                put("quantity", item.quantity)
                                put("baseQuantity", item.baseQuantity)
                                put("unitCost", item.unitCost)
                                put("lineTotal", item.lineTotal)
                                put("batchNo", item.batchNo)

                                item.expiryDate?.let {
                                    put("expiryDate", it)
                                }

                                put("createdAt", item.createdAt)
                            }
                        )
                    }
                }
            )

            put(
                "payments",
                JSONArray().apply {
                    data.payments.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("id", item.id)
                                put("eventKey", item.eventKey)
                                put("billId", item.billId)
                                put(
                                    "financialAccountId",
                                    item.financialAccountId
                                )
                                put("amount", item.amount)
                                put(
                                    "paymentMethod",
                                    item.paymentMethod
                                )
                                put("note", item.note)
                                put("paidAt", item.paidAt)
                                put("createdAt", item.createdAt)
                            }
                        )
                    }
                }
            )

            put(
                "returns",
                JSONArray().apply {
                    data.returns.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("id", item.id)
                                put("eventKey", item.eventKey)
                                put("billId", item.billId)
                                put(
                                    "purchaseLineId",
                                    item.purchaseLineId
                                )
                                put("productId", item.productId)

                                item.stockBatchId?.let {
                                    put("stockBatchId", it)
                                }

                                put(
                                    "baseQuantity",
                                    item.baseQuantity
                                )
                                put("amount", item.amount)
                                put("note", item.note)
                                put(
                                    "returnedAt",
                                    item.returnedAt
                                )
                                put("createdAt", item.createdAt)
                            }
                        )
                    }
                }
            )
        }

    fun fromJson(
        root: JSONObject?
    ): PurchaseBackupData {
        if (root == null) {
            return PurchaseBackupData()
        }

        val suppliers =
            buildList {
                val array =
                    root.optJSONArray("suppliers")
                        ?: JSONArray()

                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(index)

                    add(
                        PurchaseSupplierEntity(
                            id = item.getLong("id"),
                            name =
                                item.getString("name"),
                            phone =
                                item.optString(
                                    "phone",
                                    ""
                                ),
                            address =
                                item.optString(
                                    "address",
                                    ""
                                ),
                            note =
                                item.optString(
                                    "note",
                                    ""
                                ),
                            workspace =
                                item.optString(
                                    "workspace",
                                    "SHOP"
                                ),
                            businessKey =
                                item.optString(
                                    "businessKey",
                                    "legacy"
                                ),
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
                    )
                }
            }

        val bills =
            buildList {
                val array =
                    root.optJSONArray("bills")
                        ?: JSONArray()

                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(index)

                    add(
                        PurchaseBillEntity(
                            id = item.getLong("id"),
                            purchaseNo =
                                item.getString(
                                    "purchaseNo"
                                ),
                            supplierId =
                                item.getLong(
                                    "supplierId"
                                ),
                            subtotal =
                                item.getDouble(
                                    "subtotal"
                                ),
                            discount =
                                item.optDouble(
                                    "discount",
                                    0.0
                                ),
                            total =
                                item.getDouble(
                                    "total"
                                ),
                            status =
                                item.optString(
                                    "status",
                                    "ACTIVE"
                                ),
                            note =
                                item.optString(
                                    "note",
                                    ""
                                ),
                            workspace =
                                item.optString(
                                    "workspace",
                                    "SHOP"
                                ),
                            businessKey =
                                item.optString(
                                    "businessKey",
                                    "legacy"
                                ),
                            purchasedAt =
                                item.optLong(
                                    "purchasedAt",
                                    System.currentTimeMillis()
                                ),
                            createdAt =
                                item.optLong(
                                    "createdAt",
                                    System.currentTimeMillis()
                                )
                        )
                    )
                }
            }

        val lines =
            buildList {
                val array =
                    root.optJSONArray("lines")
                        ?: JSONArray()

                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(index)

                    add(
                        PurchaseBillLineEntity(
                            id = item.getLong("id"),
                            billId =
                                item.getLong(
                                    "billId"
                                ),
                            productId =
                                item.getLong(
                                    "productId"
                                ),
                            stockBatchId =
                                if (
                                    item.has(
                                        "stockBatchId"
                                    ) &&
                                    !item.isNull(
                                        "stockBatchId"
                                    )
                                ) {
                                    item.getLong(
                                        "stockBatchId"
                                    )
                                } else {
                                    null
                                },
                            productNameSnapshot =
                                item.getString(
                                    "productNameSnapshot"
                                ),
                            skuSnapshot =
                                item.optString(
                                    "skuSnapshot",
                                    ""
                                ),
                            unitSnapshot =
                                item.optString(
                                    "unitSnapshot",
                                    "pcs"
                                ),
                            unitFactor =
                                item.optInt(
                                    "unitFactor",
                                    1
                                ),
                            quantity =
                                item.getInt(
                                    "quantity"
                                ),
                            baseQuantity =
                                item.getInt(
                                    "baseQuantity"
                                ),
                            unitCost =
                                item.getDouble(
                                    "unitCost"
                                ),
                            lineTotal =
                                item.getDouble(
                                    "lineTotal"
                                ),
                            batchNo =
                                item.optString(
                                    "batchNo",
                                    ""
                                ),
                            expiryDate =
                                item.optLong(
                                    "expiryDate",
                                    0L
                                ).takeIf {
                                    it > 0L
                                },
                            createdAt =
                                item.optLong(
                                    "createdAt",
                                    System.currentTimeMillis()
                                )
                        )
                    )
                }
            }

        val payments =
            buildList {
                val array =
                    root.optJSONArray("payments")
                        ?: JSONArray()

                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(index)

                    add(
                        PurchasePaymentEntity(
                            id = item.getLong("id"),
                            eventKey =
                                item.getString(
                                    "eventKey"
                                ),
                            billId =
                                item.getLong(
                                    "billId"
                                ),
                            financialAccountId =
                                item.getLong(
                                    "financialAccountId"
                                ),
                            amount =
                                item.getDouble(
                                    "amount"
                                ),
                            paymentMethod =
                                item.getString(
                                    "paymentMethod"
                                ),
                            note =
                                item.optString(
                                    "note",
                                    ""
                                ),
                            paidAt =
                                item.optLong(
                                    "paidAt",
                                    System.currentTimeMillis()
                                ),
                            createdAt =
                                item.optLong(
                                    "createdAt",
                                    System.currentTimeMillis()
                                )
                        )
                    )
                }
            }

        val returns =
            buildList {
                val array =
                    root.optJSONArray("returns")
                        ?: JSONArray()

                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(index)

                    add(
                        PurchaseReturnEntity(
                            id = item.getLong("id"),
                            eventKey =
                                item.getString(
                                    "eventKey"
                                ),
                            billId =
                                item.getLong(
                                    "billId"
                                ),
                            purchaseLineId =
                                item.getLong(
                                    "purchaseLineId"
                                ),
                            productId =
                                item.getLong(
                                    "productId"
                                ),
                            stockBatchId =
                                if (
                                    item.has(
                                        "stockBatchId"
                                    ) &&
                                    !item.isNull(
                                        "stockBatchId"
                                    )
                                ) {
                                    item.getLong(
                                        "stockBatchId"
                                    )
                                } else {
                                    null
                                },
                            baseQuantity =
                                item.getInt(
                                    "baseQuantity"
                                ),
                            amount =
                                item.getDouble(
                                    "amount"
                                ),
                            note =
                                item.optString(
                                    "note",
                                    ""
                                ),
                            returnedAt =
                                item.optLong(
                                    "returnedAt",
                                    System.currentTimeMillis()
                                ),
                            createdAt =
                                item.optLong(
                                    "createdAt",
                                    System.currentTimeMillis()
                                )
                        )
                    )
                }
            }

        return PurchaseBackupData(
            suppliers = suppliers,
            bills = bills,
            lines = lines,
            payments = payments,
            returns = returns
        )
    }

    fun validate(
        data: PurchaseBackupData,
        products: List<ProductEntity>,
        batches: List<StockBatchEntity>,
        accounts: List<FinancialAccountEntity>,
        allowedWorkspaces: Set<String>,
        validBusinessIds: Set<String>
    ) {
        val supplierIds =
            mutableSetOf<Long>()

        data.suppliers.forEach { supplier ->
            require(
                supplier.id > 0L &&
                    supplierIds.add(
                        supplier.id
                    )
            ) {
                "Purchase supplier ID সঠিক নয়"
            }

            require(
                supplier.name.trim()
                    .isNotBlank()
            ) {
                "Purchase supplier name খালি"
            }

            require(
                supplier.workspace in
                    allowedWorkspaces
            ) {
                "Purchase supplier workspace সঠিক নয়"
            }

            if (
                supplier.workspace ==
                "SHOP"
            ) {
                require(
                    supplier.businessKey
                        .contains("::") &&
                        supplier.businessKey
                            .substringBefore(
                                "::"
                            )
                            .trim() in
                        validBusinessIds
                ) {
                    "Purchase supplier business সঠিক নয়"
                }
            }
        }

        val supplierById =
            data.suppliers.associateBy {
                it.id
            }

        val billIds =
            mutableSetOf<Long>()

        val billNumbers =
            mutableSetOf<
                Triple<
                    String,
                    String,
                    String
                >
            >()

        data.bills.forEach { bill ->
            val supplier =
                requireNotNull(
                    supplierById[
                        bill.supplierId
                    ]
                ) {
                    "Purchase supplier পাওয়া যায়নি"
                }

            require(
                bill.id > 0L &&
                    billIds.add(bill.id)
            ) {
                "Purchase bill ID সঠিক নয়"
            }

            require(
                bill.purchaseNo.trim()
                    .isNotBlank()
            ) {
                "Purchase number খালি"
            }

            require(
                billNumbers.add(
                    Triple(
                        bill.workspace,
                        bill.businessKey,
                        bill.purchaseNo
                    )
                )
            ) {
                "একই Purchase number একাধিকবার আছে"
            }

            require(
                bill.workspace ==
                    supplier.workspace &&
                    bill.businessKey ==
                        supplier.businessKey
            ) {
                "Purchase bill supplier scope সঠিক নয়"
            }

            require(
                bill.subtotal.isFinite() &&
                    bill.discount.isFinite() &&
                    bill.total.isFinite() &&
                    bill.subtotal >= 0.0 &&
                    bill.discount >= 0.0 &&
                    bill.discount <=
                        bill.subtotal +
                            0.0001 &&
                    bill.total >= 0.0
            ) {
                "Purchase bill amount সঠিক নয়"
            }

            require(
                abs(
                    bill.total -
                        (
                            bill.subtotal -
                                bill.discount
                            ).coerceAtLeast(
                                0.0
                            )
                ) < 0.01
            ) {
                "Purchase bill total সঠিক নয়"
            }
        }

        val billById =
            data.bills.associateBy {
                it.id
            }

        val productById =
            products.associateBy {
                it.id
            }

        val batchById =
            batches.associateBy {
                it.id
            }

        val lineIds =
            mutableSetOf<Long>()

        data.lines.forEach { line ->
            val bill =
                requireNotNull(
                    billById[
                        line.billId
                    ]
                ) {
                    "Purchase line-এর bill পাওয়া যায়নি"
                }

            val product =
                requireNotNull(
                    productById[
                        line.productId
                    ]
                ) {
                    "Purchase line-এর product পাওয়া যায়নি"
                }

            require(
                line.id > 0L &&
                    lineIds.add(
                        line.id
                    )
            ) {
                "Purchase line ID সঠিক নয়"
            }

            require(
                product.workspace ==
                    bill.workspace &&
                    product.businessKey ==
                        bill.businessKey
            ) {
                "Purchase product scope সঠিক নয়"
            }

            require(
                line.quantity > 0 &&
                    line.unitFactor > 0 &&
                    line.baseQuantity > 0 &&
                    line.unitCost.isFinite() &&
                    line.unitCost >= 0.0 &&
                    line.lineTotal.isFinite() &&
                    line.lineTotal >= 0.0
            ) {
                "Purchase line amount সঠিক নয়"
            }

            require(
                line.quantity.toLong() *
                    line.unitFactor.toLong() ==
                    line.baseQuantity.toLong()
            ) {
                "Purchase line quantity সঠিক নয়"
            }

            require(
                abs(
                    line.lineTotal -
                        line.quantity.toDouble() *
                        line.unitCost
                ) < 0.01
            ) {
                "Purchase line total সঠিক নয়"
            }

            line.stockBatchId?.let {
                    batchId ->

                val batch =
                    requireNotNull(
                        batchById[
                            batchId
                        ]
                    ) {
                        "Purchase stock batch পাওয়া যায়নি"
                    }

                require(
                    batch.productId ==
                        line.productId
                ) {
                    "Purchase batch product সঠিক নয়"
                }
            }
        }

        val lineById =
            data.lines.associateBy {
                it.id
            }

        val accountById =
            accounts.associateBy {
                it.id
            }

        val paymentIds =
            mutableSetOf<Long>()

        val paymentKeys =
            mutableSetOf<String>()

        val paidByBill =
            mutableMapOf<Long, Double>()

        data.payments.forEach {
                payment ->

            val bill =
                requireNotNull(
                    billById[
                        payment.billId
                    ]
                ) {
                    "Purchase payment bill পাওয়া যায়নি"
                }

            val account =
                requireNotNull(
                    accountById[
                        payment.financialAccountId
                    ]
                ) {
                    "Purchase payment account পাওয়া যায়নি"
                }

            require(
                payment.id > 0L &&
                    paymentIds.add(
                        payment.id
                    )
            ) {
                "Purchase payment ID সঠিক নয়"
            }

            require(
                payment.eventKey
                    .isNotBlank() &&
                    paymentKeys.add(
                        payment.eventKey
                    )
            ) {
                "Purchase payment eventKey সঠিক নয়"
            }

            require(
                payment.amount.isFinite() &&
                    payment.amount >
                        0.0001
            ) {
                "Purchase payment amount সঠিক নয়"
            }

            require(
                account.workspace ==
                    bill.workspace
            ) {
                "Purchase payment workspace সঠিক নয়"
            }

            if (
                bill.workspace ==
                "SHOP"
            ) {
                require(
                    account.businessId ==
                        bill.businessKey
                            .substringBefore(
                                "::"
                            )
                            .trim()
                ) {
                    "Purchase payment business সঠিক নয়"
                }
            }

            paidByBill[
                bill.id
            ] =
                (
                    paidByBill[
                        bill.id
                    ] ?: 0.0
                ) +
                payment.amount
        }

        paidByBill.forEach {
                (billId, amount) ->

            val bill =
                requireNotNull(
                    billById[billId]
                )

            require(
                amount <=
                    bill.total +
                        0.01
            ) {
                "Purchase payment total bill-এর বেশি"
            }
        }

        val returnIds =
            mutableSetOf<Long>()

        val returnKeys =
            mutableSetOf<String>()

        data.returns.forEach {
                item ->

            val bill =
                requireNotNull(
                    billById[
                        item.billId
                    ]
                ) {
                    "Purchase return bill পাওয়া যায়নি"
                }

            val line =
                requireNotNull(
                    lineById[
                        item.purchaseLineId
                    ]
                ) {
                    "Purchase return line পাওয়া যায়নি"
                }

            require(
                item.id > 0L &&
                    returnIds.add(
                        item.id
                    )
            ) {
                "Purchase return ID সঠিক নয়"
            }

            require(
                item.eventKey
                    .isNotBlank() &&
                    returnKeys.add(
                        item.eventKey
                    )
            ) {
                "Purchase return eventKey সঠিক নয়"
            }

            require(
                line.billId ==
                    bill.id &&
                    line.productId ==
                        item.productId &&
                    item.baseQuantity >
                        0 &&
                    item.amount.isFinite() &&
                    item.amount >= 0.0
            ) {
                "Purchase return data সঠিক নয়"
            }
        }
    }

    suspend fun restore(
        context: Context,
        data: PurchaseBackupData
    ) {
        val db =
            InventoryDatabase.get(
                context
            )

        val dao =
            db.dao()

        db.withTransaction {
            dao.clearPurchaseReturns()
            dao.clearPurchasePayments()
            dao.clearPurchaseBillLines()
            dao.clearPurchaseBills()
            dao.clearPurchaseSuppliers()

            data.suppliers.forEach {
                dao.insertPurchaseSupplier(
                    it
                )
            }

            data.bills.forEach {
                dao.insertPurchaseBill(
                    it
                )
            }

            data.lines.forEach {
                dao.insertPurchaseBillLine(
                    it
                )
            }

            data.payments.forEach {
                dao.insertPurchasePayment(
                    it
                )
            }

            data.returns.forEach {
                dao.insertPurchaseReturn(
                    it
                )
            }
        }
    }
}