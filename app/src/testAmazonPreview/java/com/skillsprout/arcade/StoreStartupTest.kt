package com.skillsprout.arcade

import android.content.Context
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import com.amazon.device.iap.internal.model.*
import com.skillsprout.billing.*
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import java.security.KeyPairGenerator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation

@Implements(PurchasingService::class)
class StartupStoreShadow {
    companion object {
        var registered=false
        var purchases=0
        @JvmStatic @Implementation fun getUserData():RequestId = RequestId.fromString("fixture-user")
        @JvmStatic @Implementation fun getProductData(skus:Set<String>):RequestId = RequestId.fromString("fixture-product")
        @JvmStatic @Implementation fun getPurchaseUpdates(reset:Boolean):RequestId = RequestId.fromString("fixture-updates")
        @JvmStatic @Implementation fun purchase(sku:String):RequestId {purchases++;return RequestId.fromString("fixture-purchase")}
        @JvmStatic @Implementation fun registerListener(context:Context,listener:PurchasingListener) {registered=true}
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],shadows=[StartupStoreShadow::class])
class StoreStartupTest {
    @Test fun pricesCannotExposePurchaseButtonsBeforeTheAmazonAccountIsReady() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val key=KeyPairGenerator.getInstance("EC").apply {initialize(256)}.generateKeyPair()
        val billing=AmazonBilling(context,AmazonProductCatalog.parse("family.monthly=family"),
            ReceiptVerifier {_,_,_->error("No receipt expected")},VerifiedAccessCache(context,key.public),{true})
        val product=ProductBuilder().setSku("family.monthly").setProductType(ProductType.SUBSCRIPTION)
            .setTitle("Monthly").setDescription("Fixture").setPrice("£4.99").setSmallIconUrl("https://example.com/icon.png").build()
        fun prices()=billing.onProductDataResponse(ProductDataResponseBuilder().setRequestId(RequestId.fromString("fixture-product"))
            .setRequestStatus(ProductDataResponse.RequestStatus.SUCCESSFUL).setProductData(mapOf(product.sku to product)).setUnavailableSkus(emptySet()).build())
        val user=UserDataBuilder().setUserId("synthetic-user").setMarketplace("GB").build()
        try {
            billing.initialise();billing.refresh();prices()
            assertTrue("Offers arriving first must not enable checkout without an Amazon account",billing.products.value.isEmpty())
            billing.onUserDataResponse(UserDataResponseBuilder().setRequestId(RequestId.fromString("fixture-user"))
                .setRequestStatus(UserDataResponse.RequestStatus.SUCCESSFUL).setUserData(user).build())
            assertEquals(listOf("family.monthly"),billing.products.value.map {it.sku})
            billing.refresh()
            assertTrue("Refresh must withdraw stale account prices",billing.products.value.isEmpty())
            billing.onUserDataResponse(UserDataResponseBuilder().setRequestId(RequestId.fromString("fixture-user"))
                .setRequestStatus(UserDataResponse.RequestStatus.FAILED).setUserData(user).build())
            prices()
            assertTrue(billing.products.value.isEmpty())
            assertEquals("Amazon account unavailable",billing.message.value)
        } finally {billing.close()}
    }

    @Test fun missingPurchaseCallbackCannotLeaveCheckoutWaitingForever() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val key=KeyPairGenerator.getInstance("EC").apply {initialize(256)}.generateKeyPair()
        val billing=AmazonBilling(context,AmazonProductCatalog.parse("family.monthly=family"),
            ReceiptVerifier {_,_,_->error("No receipt expected")},VerifiedAccessCache(context,key.public),{true})
        try {
            billing.initialise();billing.refresh()
            val user=UserDataBuilder().setUserId("synthetic-user").setMarketplace("GB").build()
            billing.onUserDataResponse(UserDataResponseBuilder().setRequestId(RequestId.fromString("fixture-user"))
                .setRequestStatus(UserDataResponse.RequestStatus.SUCCESSFUL).setUserData(user).build())
            val product=ProductBuilder().setSku("family.monthly").setProductType(ProductType.SUBSCRIPTION)
                .setTitle("Monthly").setDescription("Fixture").setPrice("£4.99").setSmallIconUrl("https://example.com/icon.png").build()
            billing.onProductDataResponse(ProductDataResponseBuilder().setRequestId(RequestId.fromString("fixture-product"))
                .setRequestStatus(ProductDataResponse.RequestStatus.SUCCESSFUL).setProductData(mapOf(product.sku to product)).setUnavailableSkus(emptySet()).build())
            val before=StartupStoreShadow.purchases
            billing.purchase("family.monthly")
            assertEquals(before+1,StartupStoreShadow.purchases)
            assertEquals("Waiting for Amazon",billing.message.value)
            assertTrue(billing.purchaseInProgress.value)
            billing.purchase("family.monthly");assertEquals(before+1,StartupStoreShadow.purchases)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(46))
            assertNotEquals("A missing SDK callback must release checkout and offer a retry", "Waiting for Amazon",billing.message.value)
            assertFalse(billing.purchaseInProgress.value)
            billing.purchase("family.monthly");assertEquals(before+2,StartupStoreShadow.purchases)
        } finally {billing.close()}
    }

    @Test fun amazonListenerIsRegisteredDuringActivityCreateBeforeFirstStart() {
        StartupStoreShadow.registered=false
        val activity=Robolectric.buildActivity(MainActivity::class.java).create()
        try {
            assertTrue("Amazon SDK must see Activity lifecycle from onCreate, before onStart and Compose",StartupStoreShadow.registered)
        } finally {activity.destroy()}
    }
}
