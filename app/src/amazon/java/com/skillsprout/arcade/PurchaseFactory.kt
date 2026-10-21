package com.skillsprout.arcade

import android.content.Context
import com.skillsprout.billing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

fun createPurchases(context:Context,parentSessionValid:()->Boolean):PurchaseGateway {
    if(!BuildConfig.PURCHASING_ALLOWED) return UnavailablePurchases()
    if(BuildConfig.AMAZON_PRODUCT_MAPPINGS.isBlank() || BuildConfig.VERIFIER_URL.isBlank()) return UnavailablePurchases()
    if(BuildConfig.RVS_ENVIRONMENT !in setOf("sandbox","production") || (BuildConfig.DEBUG && BuildConfig.RVS_ENVIRONMENT!="sandbox")) return UnavailablePurchases()
    return runCatching {
        context.assets.open("AppstoreAuthenticationKey.pem").use {require(it.read()!=-1)}
        val key=KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(context.assets.open("entitlement-public.der").use {it.readBytes()}))
        val catalog=AmazonProductCatalog.parse(BuildConfig.AMAZON_PRODUCT_MAPPINGS)
        AmazonPurchases(AmazonBilling(context.applicationContext,catalog,HttpsReceiptVerifier(java.net.URL(BuildConfig.VERIFIER_URL)),VerifiedAccessCache(context,key,BuildConfig.RVS_ENVIRONMENT),parentSessionValid))
    }.getOrElse {UnavailablePurchases()}
}

private class AmazonPurchases(private val billing:AmazonBilling):PurchaseGateway {
    private var refreshAfter=0L
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val value=MutableStateFlow(PurchaseState(configured=true,status=PurchaseStatus.LOADING))
    override val state=value.asStateFlow()
    init {
        billing.initialise()
        scope.launch {combine(billing.products,billing.access,billing.message) {products,access,message->
            PurchaseState(true,products.map {FamilyOffer(it.sku,it.title,it.price,it.description)},access?.let {minOf(it.expiresAt,it.refreshBy)} ?: 0,
                message=="Waiting for Amazon",when {
                    access!=null->PurchaseStatus.VERIFIED
                    message=="Waiting for Amazon" || message.contains("pending",true) || message.contains("could not",true) || message.contains("unavailable",true) || message.contains("not completed",true) || message.contains("changed",true)->PurchaseStatus.PENDING
                    products.isNotEmpty()->PurchaseStatus.READY
                    else->PurchaseStatus.LOADING
                })
        }.collect {value.value=it}}
    }
    override fun resume() {refreshAfter=android.os.SystemClock.elapsedRealtime()+60_000;runCatching {billing.refresh()}.onFailure {value.value=value.value.copy(offers=emptyList(),busy=false,status=PurchaseStatus.PENDING)}}
    override fun purchase(sku:String) {runCatching {billing.purchase(sku)}.onFailure {value.value=value.value.copy(busy=false,status=PurchaseStatus.PENDING)}}
    override fun restore() {runCatching {billing.restore()}.onFailure {value.value=value.value.copy(busy=false,status=PurchaseStatus.PENDING)}}
    override fun checkAccess() {val access=billing.currentAccess();if(access!=null && access.refreshBy-System.currentTimeMillis()<120_000 && android.os.SystemClock.elapsedRealtime()>=refreshAfter) resume()}
    override fun close() {scope.cancel();billing.close()}
}
