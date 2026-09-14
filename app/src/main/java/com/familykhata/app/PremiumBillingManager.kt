package com.familykhata.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PremiumPlan(
    val productId: String,
    val productType: String,
    val basePlanId: String?,
    val fallbackPrice: String
) {
    MONTHLY(
        productId = "hisabi_premium",
        productType = BillingClient.ProductType.SUBS,
        basePlanId = "monthly-prepaid",
        fallbackPrice = "৳99"
    ),
    YEARLY(
        productId = "hisabi_premium",
        productType = BillingClient.ProductType.SUBS,
        basePlanId = "yearly-prepaid",
        fallbackPrice = "৳899"
    ),
    LIFETIME(
        productId = "hisabi_premium_lifetime",
        productType = BillingClient.ProductType.INAPP,
        basePlanId = null,
        fallbackPrice = "৳2,999"
    )
}

data class PremiumBillingState(
    val ready: Boolean = false,
    val loading: Boolean = false,
    val restoring: Boolean = false,
    val active: Boolean = false,
    val subscriptionActive: Boolean = false,
    val lifetimeActive: Boolean = false,
    val monthlyPrice: String = PremiumPlan.MONTHLY.fallbackPrice,
    val yearlyPrice: String = PremiumPlan.YEARLY.fallbackPrice,
    val lifetimePrice: String = PremiumPlan.LIFETIME.fallbackPrice,
    val message: String? = null
)

class PremiumBillingManager private constructor(
    context: Context
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext

    private val preferences =
        appContext.getSharedPreferences(
            "hisabi_khata_preferences",
            Context.MODE_PRIVATE
        )

    private val _state =
        MutableStateFlow(
            PremiumBillingState(
                active = preferences.getBoolean(
                    KEY_PLAY_PREMIUM_UNLOCKED,
                    false
                )
            )
        )

    val state: StateFlow<PremiumBillingState> =
        _state.asStateFlow()

    private val productDetails =
        mutableMapOf<String, ProductDetails>()

    private val pendingPurchasesParams =
        PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()

    private val billingClient =
        BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(pendingPurchasesParams)
            .enableAutoServiceReconnection()
            .build()

    private var startingConnection = false
    private var restoreAfterConnect = false

    fun start() {
        if (billingClient.isReady) {
            _state.value =
                _state.value.copy(
                    ready = true,
                    message = null
                )
            loadProducts()

            val restoring = restoreAfterConnect
            restoreAfterConnect = false
            queryOwnedPurchases(restoring)

            return
        }

        if (startingConnection) return

        startingConnection = true
        _state.value =
            _state.value.copy(
                loading = true,
                message = null
            )

        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(
                    billingResult: BillingResult
                ) {
                    startingConnection = false

                    if (
                        billingResult.responseCode ==
                        BillingClient.BillingResponseCode.OK
                    ) {
                        _state.value =
                            _state.value.copy(
                                ready = true,
                                loading = false,
                                message = null
                            )

                        loadProducts()

                        val restoring = restoreAfterConnect
                        restoreAfterConnect = false
                        queryOwnedPurchases(restoring)
                    } else {
                        _state.value =
                            _state.value.copy(
                                ready = false,
                                loading = false,
                                restoring = false,
                                message =
                                    billingResult.debugMessage
                                        .ifBlank {
                                            "Google Play Billing unavailable"
                                        }
                            )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    startingConnection = false
                    restoreAfterConnect = false
                    _state.value =
                        _state.value.copy(
                            ready = false,
                            loading = false,
                            restoring = false
                        )
                }
            }
        )
    }

    fun launchPurchase(
        activity: Activity,
        plan: PremiumPlan
    ): BillingResult {
        if (!billingClient.isReady) {
            start()
            return BillingResult.newBuilder()
                .setResponseCode(
                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
                )
                .setDebugMessage(
                    "Google Play Billing is not ready"
                )
                .build()
        }

        val details =
            productDetails[productKey(plan)]
                ?: return BillingResult.newBuilder()
                    .setResponseCode(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE
                    )
                    .setDebugMessage(
                        "Premium product is not available yet"
                    )
                    .build()

        val productParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(details)

        if (plan.productType == BillingClient.ProductType.SUBS) {
            val offer =
                details.subscriptionOfferDetails
                    ?.firstOrNull {
                        it.basePlanId == plan.basePlanId
                    }
                    ?: return BillingResult.newBuilder()
                        .setResponseCode(
                            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE
                        )
                        .setDebugMessage(
                            "Selected subscription plan is unavailable"
                        )
                        .build()

            productParams.setOfferToken(offer.offerToken)
        } else {
            val offer =
                details.oneTimePurchaseOfferDetailsList
                    ?.firstOrNull()
                    ?: details.oneTimePurchaseOfferDetails

            if (offer != null) {
                productParams.setOfferToken(
                    offer.offerToken
                )
            }
        }

        val flowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(productParams.build())
                )
                .build()

        return billingClient.launchBillingFlow(
            activity,
            flowParams
        )
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            _state.value =
                _state.value.copy(
                    restoring = true,
                    message = null
                )

            restoreAfterConnect = true
            start()
            return
        }

        _state.value =
            _state.value.copy(
                restoring = true,
                message = null
            )

        queryOwnedPurchases(
            restoring = true
        )
    }

    fun refreshPurchases() {
        if (!billingClient.isReady) {
            start()
            return
        }

        queryOwnedPurchases(
            restoring = false
        )
    }

    private fun loadProducts() {
        querySubscriptionProduct()
        queryLifetimeProduct()
    }

    private fun querySubscriptionProduct() {
        val params =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductId(
                                PremiumPlan.MONTHLY.productId
                            )
                            .setProductType(
                                BillingClient.ProductType.SUBS
                            )
                            .build()
                    )
                )
                .build()

        billingClient.queryProductDetailsAsync(params) {
                billingResult,
                result ->

            if (
                billingResult.responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                return@queryProductDetailsAsync
            }

            val details =
                result.productDetailsList
                    .firstOrNull {
                        it.productId ==
                            PremiumPlan.MONTHLY.productId
                    }
                    ?: return@queryProductDetailsAsync

            productDetails[
                productKey(PremiumPlan.MONTHLY)
            ] = details

            productDetails[
                productKey(PremiumPlan.YEARLY)
            ] = details

            val monthlyOffer =
                details.subscriptionOfferDetails
                    ?.firstOrNull {
                        it.basePlanId ==
                            PremiumPlan.MONTHLY.basePlanId
                    }

            val yearlyOffer =
                details.subscriptionOfferDetails
                    ?.firstOrNull {
                        it.basePlanId ==
                            PremiumPlan.YEARLY.basePlanId
                    }

            val monthlyPrice =
                monthlyOffer
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice
                    ?: PremiumPlan.MONTHLY.fallbackPrice

            val yearlyPrice =
                yearlyOffer
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice
                    ?: PremiumPlan.YEARLY.fallbackPrice

            _state.value =
                _state.value.copy(
                    monthlyPrice = monthlyPrice,
                    yearlyPrice = yearlyPrice
                )
        }
    }

    private fun queryLifetimeProduct() {
        val params =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductId(
                                PremiumPlan.LIFETIME.productId
                            )
                            .setProductType(
                                BillingClient.ProductType.INAPP
                            )
                            .build()
                    )
                )
                .build()

        billingClient.queryProductDetailsAsync(params) {
                billingResult,
                result ->

            if (
                billingResult.responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                return@queryProductDetailsAsync
            }

            val details =
                result.productDetailsList
                    .firstOrNull {
                        it.productId ==
                            PremiumPlan.LIFETIME.productId
                    }
                    ?: return@queryProductDetailsAsync

            productDetails[
                productKey(PremiumPlan.LIFETIME)
            ] = details

            val offer =
                details.oneTimePurchaseOfferDetailsList
                    ?.firstOrNull()
                    ?: details.oneTimePurchaseOfferDetails

            _state.value =
                _state.value.copy(
                    lifetimePrice =
                        offer?.formattedPrice
                            ?: PremiumPlan.LIFETIME.fallbackPrice
                )
        }
    }

    private fun queryOwnedPurchases(
        restoring: Boolean
    ) {
        var subscriptionsFinished = false
        var inAppFinished = false
        var subscriptionsOk = false
        var inAppOk = false

        var subscriptionPurchase = false
        var lifetimePurchase = false

        fun finishIfReady() {
            if (
                !subscriptionsFinished ||
                !inAppFinished
            ) {
                return
            }

            if (!subscriptionsOk || !inAppOk) {
                _state.value =
                    _state.value.copy(
                        restoring = false,
                        message =
                            if (restoring) {
                                "Google Play purchase check could not be completed"
                            } else {
                                _state.value.message
                            }
                    )
                return
            }

            val active =
                subscriptionPurchase ||
                    lifetimePurchase

            setPlayEntitlement(active)

            _state.value =
                _state.value.copy(
                    active = active,
                    subscriptionActive = subscriptionPurchase,
                    lifetimeActive = lifetimePurchase,
                    restoring = false,
                    message =
                        if (restoring) {
                            if (active) {
                                "Premium purchase restored"
                            } else {
                                "No active Premium purchase found"
                            }
                        } else {
                            null
                        }
                )
        }

        val subscriptionParams =
            QueryPurchasesParams.newBuilder()
                .setProductType(
                    BillingClient.ProductType.SUBS
                )
                .build()

        billingClient.queryPurchasesAsync(
            subscriptionParams
        ) { billingResult, purchases ->

            subscriptionsFinished = true
            subscriptionsOk =
                billingResult.responseCode ==
                    BillingClient.BillingResponseCode.OK

            if (subscriptionsOk) {
                purchases.forEach { purchase ->
                    if (
                        purchase.purchaseState ==
                        Purchase.PurchaseState.PURCHASED &&
                        PremiumPlan.MONTHLY.productId in
                        purchase.products
                    ) {
                        subscriptionPurchase = true
                        acknowledgeIfNeeded(purchase)
                    }
                }
            }

            finishIfReady()
        }

        val inAppParams =
            QueryPurchasesParams.newBuilder()
                .setProductType(
                    BillingClient.ProductType.INAPP
                )
                .build()

        billingClient.queryPurchasesAsync(
            inAppParams
        ) { billingResult, purchases ->

            inAppFinished = true
            inAppOk =
                billingResult.responseCode ==
                    BillingClient.BillingResponseCode.OK

            if (inAppOk) {
                purchases.forEach { purchase ->
                    if (
                        purchase.purchaseState ==
                        Purchase.PurchaseState.PURCHASED &&
                        PremiumPlan.LIFETIME.productId in
                        purchase.products
                    ) {
                        lifetimePurchase = true
                        acknowledgeIfNeeded(purchase)
                    }
                }
            }

            finishIfReady()
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty()
                    .filter {
                        it.purchaseState ==
                            Purchase.PurchaseState.PURCHASED
                    }
                    .forEach {
                        acknowledgeIfNeeded(it)
                    }

                refreshPurchases()
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.value =
                    _state.value.copy(
                        message = null
                    )
            }

            else -> {
                _state.value =
                    _state.value.copy(
                        message =
                            billingResult.debugMessage
                                .ifBlank {
                                    "Google Play purchase failed"
                                }
                    )
            }
        }
    }

    private fun acknowledgeIfNeeded(
        purchase: Purchase
    ) {
        if (purchase.isAcknowledged) return

        val params =
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(
                    purchase.purchaseToken
                )
                .build()

        billingClient.acknowledgePurchase(params) {
            billingResult ->

            if (
                billingResult.responseCode ==
                BillingClient.BillingResponseCode.OK
            ) {
                refreshPurchases()
            }
        }
    }

    private fun setPlayEntitlement(
        active: Boolean
    ) {
        preferences.edit()
            .putBoolean(
                KEY_PLAY_PREMIUM_UNLOCKED,
                active
            )
            .putLong(
                KEY_PLAY_PREMIUM_CHECKED_AT,
                System.currentTimeMillis()
            )
            .apply()
    }

    private fun productKey(
        plan: PremiumPlan
    ): String =
        "${plan.productId}:${plan.basePlanId.orEmpty()}"

    companion object {
        const val KEY_PLAY_PREMIUM_UNLOCKED =
            "play_premium_unlocked"

        const val KEY_PLAY_PREMIUM_CHECKED_AT =
            "play_premium_checked_at"

        @Volatile
        private var instance:
            PremiumBillingManager? = null

        fun get(
            context: Context
        ): PremiumBillingManager =
            instance ?: synchronized(this) {
                instance
                    ?: PremiumBillingManager(context)
                        .also {
                            instance = it
                        }
            }
    }
}
