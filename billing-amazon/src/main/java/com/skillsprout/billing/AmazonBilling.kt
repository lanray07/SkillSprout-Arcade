package com.skillsprout.billing

import android.content.Context
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import android.util.Base64
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature

/** Not linked into the offline development APK. No credentials or product prices in source. */
data class StoreProduct(val sku:String,val title:String,val price:String)
data class SignedAccess(val payloadBase64:String,val signatureBase64:String)
data class Access(val accountHash:String,val sku:String,val receiptId:String,val expiresAt:Long,val refreshBy:Long,val issuedAt:Long)
class ReceiptInactiveException:Exception("Receipt is inactive")
fun interface ReceiptVerifier {
    /** Backend must verify Amazon RVS with its private shared secret before signing access. */
    suspend fun verify(amazonUserId:String,receiptId:String,sku:String):SignedAccess
}

class VerifiedAccessCache(context:Context,private val publicKey:PublicKey,private val environment:String="production",private val wallTime:()->Long=System::currentTimeMillis) {
    private val prefs=context.getSharedPreferences("verified_amazon_access",Context.MODE_PRIVATE)
    private fun decode(s:String)=Base64.decode(s,Base64.URL_SAFE or Base64.NO_WRAP)
    fun read(accountHash:String):Access? = runCatching {
        val p=prefs.getString("payload",null) ?: return null
        val s=prefs.getString("signature",null) ?: return null
        validate(SignedAccess(p,s),accountHash)
    }.getOrNull()
    fun save(signed:SignedAccess,accountHash:String,expectedSku:String,expectedReceipt:String):Access {
        val access=validate(signed,accountHash)
        require(access.sku==expectedSku && access.receiptId==expectedReceipt) {"Receipt binding mismatch"}
        val existing=read(accountHash)
        if(existing!=null && existing.receiptId==access.receiptId) require(access.issuedAt>=existing.issuedAt) {"Stale verification response"}
        if(existing!=null && existing.receiptId!=access.receiptId && existing.expiresAt>access.expiresAt) return existing
        check(prefs.edit().putString("payload",signed.payloadBase64).putString("signature",signed.signatureBase64).commit())
        return access
    }
    private fun validate(s:SignedAccess,accountHash:String):Access {
        require(environment in setOf("production","sandbox") && s.payloadBase64.length<=8192 && s.signatureBase64.length<=1024)
        val payload=decode(s.payloadBase64)
        require(Signature.getInstance("SHA256withECDSA").run {initVerify(publicKey);update(payload);verify(decode(s.signatureBase64))}) {"Access signature invalid"}
        val j=JSONObject(payload.toString(Charsets.UTF_8))
        require(j.getInt("version")==1 && j.getString("appId")=="com.skillsprout.arcade" && j.getString("environment")==environment)
        val a=Access(j.getString("accountHash"),j.getString("sku"),j.getString("receiptId"),j.getLong("expiresAt"),j.getLong("refreshBy"),j.getLong("issuedAt"))
        val now=maxOf(wallTime(),prefs.getLong("lastSeenWall",0))
        prefs.edit().putLong("lastSeenWall",now).commit()
        require(a.accountHash==accountHash && now<a.expiresAt && now<a.refreshBy)
        require(a.issuedAt>=0 && a.issuedAt<=now+300_000 && a.refreshBy>a.issuedAt && a.refreshBy<=a.expiresAt)
        require(a.refreshBy-a.issuedAt<=7*86_400_000L) {"Offline cache exceeds seven days"}
        return a
    }
    fun clear() {prefs.edit().remove("payload").remove("signature").commit()}
}

class AmazonBilling(
    private val context:Context,private val skus:Set<String>,private val verifier:ReceiptVerifier,
    private val cache:VerifiedAccessCache,private val parentSessionValid:()->Boolean
):PurchasingListener {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val productsState=MutableStateFlow<List<StoreProduct>>(emptyList())
    private val accessState=MutableStateFlow<Access?>(null)
    private val messageState=MutableStateFlow("Purchasing not initialised")
    val products=productsState.asStateFlow();val access=accessState.asStateFlow();val message=messageState.asStateFlow()
    private var userId:String?=null
    private var restoring=false
    private var restoreHadFailure=false
    private val verifiedDuringRestore=mutableSetOf<String>()
    private fun bindUser(id:String) {
        if(userId!=id) {userId=id;accessState.value=cache.read(accountHash(id))?.takeIf {it.sku in skus}}
    }
    private fun gate() {check(parentSessionValid()) {"Parent PIN session required"}}
    fun initialise() {gate();require(skus.isNotEmpty());PurchasingService.registerListener(context.applicationContext,this);PurchasingService.getUserData();PurchasingService.getProductData(skus)}
    fun purchase(sku:String) {gate();require(productsState.value.any {it.sku==sku});PurchasingService.purchase(sku);messageState.value="Waiting for Amazon"}
    fun restore() {gate();restoring=true;restoreHadFailure=false;verifiedDuringRestore.clear();PurchasingService.getPurchaseUpdates(true)}
    override fun onUserDataResponse(r:UserDataResponse) {
        if(r.requestStatus!=UserDataResponse.RequestStatus.SUCCESSFUL) {messageState.value="Amazon account unavailable";accessState.value=null;return}
        bindUser(r.userData.userId)
        accessState.value=cache.read(accountHash(userId!!))
    }
    override fun onProductDataResponse(r:ProductDataResponse) {
        if(r.requestStatus==ProductDataResponse.RequestStatus.SUCCESSFUL) {
            productsState.value=r.productData.values.filter {it.sku in skus}.map {StoreProduct(it.sku,it.title,it.price)}
            messageState.value=if(productsState.value.isEmpty()) "No store products available" else "Store prices loaded"
        } else {productsState.value=emptyList();messageState.value="Store unavailable. Please try again later."}
    }
    override fun onPurchaseResponse(r:PurchaseResponse) {
        if(r.requestStatus==PurchaseResponse.RequestStatus.SUCCESSFUL) {
            bindUser(r.userData.userId)
            scope.launch { verifyReceipt(r.receipt,r.userData.userId) }
        } else messageState.value="Purchase was not completed: ${r.requestStatus.name}. You can retry or restore."
    }
    override fun onPurchaseUpdatesResponse(r:PurchaseUpdatesResponse) {
        if(r.requestStatus!=PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {restoring=false;messageState.value="Could not restore. Existing verified access remains valid until its refresh deadline.";return}
        bindUser(r.userData.userId)
        scope.launch {
            r.receipts.forEach {verifyReceipt(it,r.userData.userId)}
            if(r.hasMore()) PurchasingService.getPurchaseUpdates(false)
            else {if(restoring && verifiedDuringRestore.isEmpty() && !restoreHadFailure) {cache.clear();accessState.value=null};restoring=false;messageState.value=if(restoreHadFailure) "Some receipts could not be verified. Please try restore again." else "Restore complete"}
        }
    }
    private suspend fun verifyReceipt(receipt:Receipt,user:String) {
        if(receipt.sku !in skus) return
        if(receipt.isCanceled && (receipt.cancelDate?.time ?: 0)<=System.currentTimeMillis()) {if(accessState.value?.receiptId==receipt.receiptId) {cache.clear();accessState.value=null};return}
        runCatching {
            val signed=withContext(Dispatchers.IO) {verifier.verify(user,receipt.receiptId,receipt.sku)}
            if(userId!=user) return
            val verified=cache.save(signed,accountHash(user),receipt.sku,receipt.receiptId)
            verifiedDuringRestore.add(receipt.receiptId);accessState.value=verified
            PurchasingService.notifyFulfillment(receipt.receiptId,FulfillmentResult.FULFILLED)
            messageState.value="Family access verified"
        }.onFailure {
            if(it is CancellationException) throw it
            if(it is ReceiptInactiveException && accessState.value?.receiptId==receipt.receiptId && userId==user) {cache.clear();accessState.value=null}
            restoreHadFailure=true;messageState.value="Verification pending or access expired. Restore when connected."
        }
    }
    fun currentAccess():Access? = userId?.let {cache.read(accountHash(it))}?.takeIf {it.sku in skus}.also {accessState.value=it}
    fun close() {scope.cancel()}
    companion object {fun accountHash(id:String)=MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString("") {"%02x".format(it)}}
}
