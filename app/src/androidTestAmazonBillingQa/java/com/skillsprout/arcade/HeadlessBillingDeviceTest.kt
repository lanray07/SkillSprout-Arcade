package com.skillsprout.arcade

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazon.device.drm.LicensingService
import com.skillsprout.billing.*
import java.io.File
import java.net.URL
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real SDK/HTTPS checks in disposable app storage. No UI, private PIN, profiles or purchase. */
@RunWith(AndroidJUnit4::class)
class HeadlessBillingDeviceTest {
    @Test fun sandboxCatalogueParentGateAndReceiptRestore() {
        val instrument=InstrumentationRegistry.getInstrumentation()
        val context=instrument.targetContext
        check(BuildConfig.DEBUG && context.packageName=="com.skillsprout.arcade.billingqa")
        check(BuildConfig.RVS_ENVIRONMENT=="sandbox" && URL(BuildConfig.VERIFIER_URL).host=="skillsprout-receipts-sandbox.lanraybanks.workers.dev")
        instrument.runOnMainSync {LicensingService.verifyLicense(context) { }}
        fun await(timeout:Long,condition:()->Boolean) {
            val until=android.os.SystemClock.elapsedRealtime()+timeout
            while(!condition() && android.os.SystemClock.elapsedRealtime()<until) Thread.sleep(100)
            assertTrue("Timed out waiting for sandbox SDK response",condition())
        }
        await(10000) {LicensingService.getAppstoreSDKMode()!="UNKNOWN"}
        check(LicensingService.getAppstoreSDKMode()=="SANDBOX") {"Never exercise a live billing session"}
        val catalog=AmazonProductCatalog.parse(BuildConfig.AMAZON_PRODUCT_MAPPINGS)
        val key=KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(context.assets.open("entitlement-public.der").use {it.readBytes()}))
        val cache=VerifiedAccessCache(context,key,"sandbox")
        val billing=AmazonBilling(context,catalog,HttpsReceiptVerifier(URL(BuildConfig.VERIFIER_URL)),cache) {false}
        try {
            cache.clear() // Only this disposable QA app's lease; require a fresh verification response.
            instrument.runOnMainSync {billing.initialise();billing.refresh()}
            await(60000) {billing.products.value.size==2}
            assertTrue(billing.products.value.all {it.price.isNotBlank() && catalog.mayPurchase(it.sku)})
            var purchaseBlocked=false;var restoreBlocked=false
            instrument.runOnMainSync {
                purchaseBlocked=runCatching {billing.purchase(billing.products.value.first().sku)}.exceptionOrNull() is IllegalStateException
                restoreBlocked=runCatching {billing.restore()}.exceptionOrNull() is IllegalStateException
            }
            assertTrue("Purchase must reject a closed parent session",purchaseBlocked)
            assertTrue("Manual restore must reject a closed parent session",restoreBlocked)
            // Foreground receipt updates are automatic; observe their actual asynchronous completion.
            await(60000) {billing.message.value=="Restore complete" || billing.message.value.startsWith("Some receipts") || billing.message.value.startsWith("Could not restore")}
            assertEquals("Initial sandbox receipt update must complete", "Restore complete",billing.message.value)
            var verified=false
            instrument.runOnMainSync {verified=billing.currentAccess()!=null}
            val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
            File(folder,"headless-billing.json").writeText(JSONObject()
                .put("model",android.os.Build.MODEL).put("api",android.os.Build.VERSION.SDK_INT)
                .put("sdkMode","SANDBOX").put("storeOffers",billing.products.value.size)
                .put("purchaseBlockedWithoutParentSession",purchaseBlocked)
                .put("restoreBlockedWithoutParentSession",restoreBlocked)
                .put("foregroundReceiptUpdateCompleted",true).put("validSignedReceiptAccess",verified)
                .put("manualRestoreUiAndNewRoundTripTested",false)
                .put("emptyReceiptRestoreIsNotPurchaseProof",!verified)
                .put("purchaseInitiated",false).put("privatePinOrProfilesOpened",false)
                .put("uiOrAudibleAccessibilityTested",false).put("productionApproval",false).toString(2))
        } finally {instrument.runOnMainSync {billing.close()}}
    }
}
