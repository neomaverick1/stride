package com.neomaverick.stride.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Stride Pro (monthly subscription) through Google Play Billing.
 * The demo build is not listed on Play, so when Play has no product it offers a free demo unlock instead,
 * which runs the exact same "is Pro" path through the app.
 */
class Premium(context: Context) {
    private val prefs = context.getSharedPreferences("premium", Context.MODE_PRIVATE)
    private val _isPro = MutableStateFlow(prefs.getBoolean("pro", false))
    val isPro: StateFlow<Boolean> = _isPro

    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases -> if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases?.let { handle(it) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect(onReady: () -> Unit = {}) {
        if (client.isReady) return onReady()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) { if (r.responseCode == BillingClient.BillingResponseCode.OK) onReady() }
            override fun onBillingServiceDisconnected() {}
        })
    }

    /** Re-checks owned subscriptions (after reinstall or on another phone). */
    suspend fun restore() {
        if (!client.isReady) return
        val owned = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        handle(owned.purchasesList)
    }

    /** Starts the Play purchase sheet. Returns false when the product isn't on Play (demo build). */
    suspend fun buy(activity: Activity): Boolean {
        if (!client.isReady) return false
        val details = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT).setProductType(BillingClient.ProductType.SUBS).build(),
            )).build()
        ).productDetailsList?.firstOrNull() ?: return false
        val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return false
        client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build(),
        )).build())
        return true
    }

    fun demoUnlock(on: Boolean) = set(on)

    private fun handle(purchases: List<Purchase>) {
        val active = purchases.any { PRODUCT in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (active) set(true)
        purchases.filter { !it.isAcknowledged && it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { p ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
            }
        }
    }

    private fun set(on: Boolean) { prefs.edit().putBoolean("pro", on).apply(); _isPro.value = on }

    companion object { const val PRODUCT = "stride_pro_monthly" }
}
