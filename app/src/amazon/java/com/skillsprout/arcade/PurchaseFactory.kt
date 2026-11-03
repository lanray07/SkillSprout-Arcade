package com.skillsprout.arcade

import android.content.Context
import com.skillsprout.billing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

fun createPurchases(context:Context,parentSessionValid:()->Boolean):PurchaseGateway {
    if(BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.billingqa") return SandboxOnlyPurchases(context,parentSessionValid)
    return configuredPurchases(context,parentSessionValid)
}

private fun configuredPurchases(context:Context,parentSessionValid:()->Boolean):PurchaseGateway {
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

/** Appstore SDK 3 mode is available only after verifyLicense; UNKNOWN and PRODUCTION fail closed. */
private class SandboxOnlyPurchases(context:Context,parentSessionValid:()->Boolean):PurchaseGateway {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val value=MutableStateFlow(PurchaseState(status=PurchaseStatus.LOADING))
    override val state=value.asStateFlow()
    private var delegate:PurchaseGateway?=null
    init {scope.launch {
        if(!BuildConfig.DEBUG || BuildConfig.RVS_ENVIRONMENT!="sandbox") {value.value=PurchaseState();return@launch}
        try {
            com.amazon.device.drm.LicensingService.verifyLicense(context.applicationContext) { }
            repeat(50) {
                when(com.amazon.device.drm.LicensingService.getAppstoreSDKMode()) {
                    "SANDBOX" -> {
                        val gateway=configuredPurchases(context,parentSessionValid);delegate=gateway
                        launch {gateway.state.collect {value.value=it}}
                        gateway.resume();return@launch
                    }
                    "PRODUCTION" -> {value.value=PurchaseState();return@launch}
                }
                delay(100)
            }
            value.value=PurchaseState()
        } catch(cancelled:CancellationException) {throw cancelled}
        catch(_:Exception) {value.value=PurchaseState()}
    }}
    private fun sandbox():Boolean = runCatching {com.amazon.device.drm.LicensingService.getAppstoreSDKMode()=="SANDBOX"}.getOrDefault(false)
    override fun resume() {if(sandbox()) delegate?.resume()}
    override fun purchase(sku:String) {if(sandbox()) delegate?.purchase(sku)}
    override fun restore() {if(sandbox()) delegate?.restore()}
    override fun checkAccess() {if(sandbox()) delegate?.checkAccess() else value.value=PurchaseState()}
    override fun close() {scope.cancel();delegate?.close()}
}

private class AmazonPurchases(private val billing:AmazonBilling):PurchaseGateway {
    private var refreshAfter=0L
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val value=MutableStateFlow(PurchaseState(configured=true,status=PurchaseStatus.LOADING))
    override val state=value.asStateFlow()
    init {
        billing.initialise()
        scope.launch {combine(billing.products,billing.access,billing.message,billing.purchaseInProgress) {products,access,message,busy->
            PurchaseState(true,products.map {FamilyOffer(it.sku,it.title,it.price,it.description,it.subscriptionPeriod)},access?.let {minOf(it.expiresAt,it.refreshBy)} ?: 0,
                busy,when {
                    access!=null->PurchaseStatus.VERIFIED
                    message.contains("not completed",true) || message=="Amazon account unavailable"->PurchaseStatus.RETRY
                    message=="Waiting for Amazon" || message.contains("pending",true) || message.contains("could not",true) || message.contains("unavailable",true) || message.contains("not completed",true) || message.contains("changed",true)->PurchaseStatus.PENDING
                    products.isNotEmpty()->PurchaseStatus.READY
                    else->PurchaseStatus.LOADING
                })
        }.collect {value.value=it}}
    }
    override fun resume() {refreshAfter=android.os.SystemClock.elapsedRealtime()+60_000;runCatching {billing.refresh()}.onFailure {value.value=value.value.copy(offers=emptyList(),busy=false,status=PurchaseStatus.PENDING)}}
    override fun purchase(sku:String) {runCatching {billing.purchase(sku)}.onFailure {value.value=value.value.copy(busy=false,status=PurchaseStatus.RETRY)}}
    override fun restore() {runCatching {billing.restore()}.onFailure {value.value=value.value.copy(busy=false,status=PurchaseStatus.PENDING)}}
    override fun checkAccess() {val access=billing.currentAccess();if(access!=null && access.refreshBy-System.currentTimeMillis()<120_000 && android.os.SystemClock.elapsedRealtime()>=refreshAfter) resume()}
    override fun close() {scope.cancel();billing.close()}
}
