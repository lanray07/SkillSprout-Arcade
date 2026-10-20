package com.skillsprout.billing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import java.net.URL

// Cache/HTTP configuration tests do not initialise Amazon's Android broadcast receiver.
// Live SDK callbacks still require App Tester on Fire; its obfuscated receiver is not JVM bytecode QA.
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],manifest=Config.NONE)
class VerifiedAccessTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val key=KeyPairGenerator.getInstance("EC").apply {initialize(256)}.generateKeyPair()
    private val start=1700000000000L
    private var time=start
    private val account=AmazonBilling.accountHash("test-user")
    @Before fun clear(){context.getSharedPreferences("verified_amazon_access",0).edit().clear().commit()}
    private fun token(environment:String="production",issued:Long=start,expires:Long=start+3600000,refresh:Long=expires):SignedAccess {
        val payload=JSONObject().put("version",1).put("appId","com.skillsprout.arcade").put("environment",environment).put("accountHash",account).put("sku","test.monthly").put("receiptId","test-receipt").put("issuedAt",issued).put("expiresAt",expires).put("refreshBy",refresh).toString().toByteArray()
        val signature=Signature.getInstance("SHA256withECDSA").run {initSign(key.private);update(payload);sign()}
        return SignedAccess(Base64.getUrlEncoder().withoutPadding().encodeToString(payload),Base64.getUrlEncoder().withoutPadding().encodeToString(signature))
    }
    private fun cache()=VerifiedAccessCache(context,key.public,wallTime={time})
    @Test fun verifiesPersistsAndExpiresAccountBoundAccess(){
        cache().save(token(),account,"test.monthly","test-receipt")
        assertNotNull(cache().read(account));assertNull(cache().read(AmazonBilling.accountHash("other-user")))
        time=start+3600000;assertNull(cache().read(account))
        time=start;assertNull(cache().read(account))
    }
    @Test fun rejectsTamperingEnvironmentAndReceiptMismatch(){
        val signed=token();val changed=signed.copy(payloadBase64=Base64.getUrlEncoder().encodeToString("{}".toByteArray()))
        assertThrows(IllegalArgumentException::class.java){cache().save(changed,account,"test.monthly","test-receipt")}
        assertThrows(IllegalArgumentException::class.java){cache().save(token("sandbox"),account,"test.monthly","test-receipt")}
        assertThrows(IllegalArgumentException::class.java){cache().save(signed,account,"test.monthly","another-receipt")}
    }
    @Test fun newerVerificationCanShortenTheSameReceiptsAccess(){
        cache().save(token(),account,"test.monthly","test-receipt")
        time=start+1000
        cache().save(token(issued=time,expires=start+2000),account,"test.monthly","test-receipt")
        time=start+2001;assertNull(cache().read(account))
    }
    @Test fun rejectsExcessiveOrFutureLeases(){
        assertThrows(IllegalArgumentException::class.java){cache().save(token(expires=start+8*86400000L),account,"test.monthly","test-receipt")}
        assertThrows(IllegalArgumentException::class.java){cache().save(token(issued=start+3600000,expires=start+7200000),account,"test.monthly","test-receipt")}
    }
    @Test fun httpAndCredentialBearingEndpointsAreNotAllowed(){
        assertThrows(IllegalArgumentException::class.java){HttpsReceiptVerifier(URL("http://example.com/verify"))}
        assertThrows(IllegalArgumentException::class.java){HttpsReceiptVerifier(URL("https://user:password@example.com/verify"))}
        assertNotNull(HttpsReceiptVerifier(URL("https://example.com/verify")))
    }
}
