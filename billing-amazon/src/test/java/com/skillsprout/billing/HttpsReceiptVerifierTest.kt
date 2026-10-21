package com.skillsprout.billing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLStreamHandler
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

/** Production HTTP client and signed cache, with a controlled transport and no store transactions. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35], manifest=Config.NONE)
class HttpsReceiptVerifierTest {
    private class Connection(url: URL, val code: Int, val body: ByteArray, val unavailable: Boolean): HttpURLConnection(url) {
        val request = ByteArrayOutputStream()
        var disconnected = false
        var bodyOpened = false
        override fun connect() {}
        override fun usingProxy() = false
        override fun disconnect() { disconnected = true }
        override fun getOutputStream() = request.apply { if (unavailable) throw IOException("Test transport outage") }
        override fun getResponseCode() = code
        override fun getInputStream() = ByteArrayInputStream(body).also { bodyOpened = true }
    }
    private val connections = mutableListOf<Connection>()
    private var responseCode = 200
    private var responseBody = "".toByteArray()
    private var unavailable = false
    private val endpoint = URL(null, "https://receipt-test.invalid/verify", object: URLStreamHandler() {
        override fun openConnection(url: URL) = Connection(url, responseCode, responseBody, unavailable).also { connections += it }
    })
    private val verifier = HttpsReceiptVerifier(endpoint)
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val key = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    private val account = AmazonBilling.accountHash("test-user")
    private val start = 1700000000000L
    private var now = start
    @Before fun resetCache() { context.getSharedPreferences("verified_amazon_access",0).edit().clear().commit() }
    private fun request() = runBlocking { verifier.verify("test-user", "test-receipt", "test.parent") }
    private fun respondWithLease() {
        val bytes = JSONObject().put("version",1).put("appId","com.skillsprout.arcade")
            .put("environment","production").put("accountHash",account).put("sku","test.parent")
            .put("receiptId","test-receipt").put("issuedAt",now).put("expiresAt",now+60000)
            .put("refreshBy",now+60000).toString().toByteArray()
        val signature = Signature.getInstance("SHA256withECDSA").run { initSign(key.private); update(bytes); sign() }
        responseBody = JSONObject().put("payloadBase64",Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
            .put("signatureBase64",Base64.getUrlEncoder().withoutPadding().encodeToString(signature)).toString().toByteArray()
    }
    @Test fun requestHasOnlyReceiptFieldsAndBoundedTransport() {
        respondWithLease()
        assertNotNull(request())
        val connection = connections.single()
        val sent = JSONObject(connection.request.toString("UTF-8"))
        assertEquals(setOf("userId","receiptId","sku"),sent.keys().asSequence().toSet())
        assertEquals("test-user",sent.getString("userId"))
        assertEquals("test-receipt",sent.getString("receiptId"))
        assertEquals("test.parent",sent.getString("sku"))
        assertEquals("POST",connection.requestMethod)
        assertEquals("application/json",connection.getRequestProperty("Content-Type"))
        assertEquals(8000,connection.connectTimeout)
        assertEquals(10000,connection.readTimeout)
        assertFalse(connection.instanceFollowRedirects)
        assertTrue(connection.disconnected)
    }
    @Test fun inactiveReceiptIsDistinctFromOutagesAndRedirects() {
        for (status in listOf(301,302,307,403,429,500,503)) {
            responseCode = status
            val failure = runCatching { request() }.exceptionOrNull()
            assertNotNull("HTTP $status must not grant access",failure)
            assertEquals(status==403,failure is ReceiptInactiveException)
            assertFalse(connections.last().bodyOpened)
            assertFalse(connections.last().instanceFollowRedirects)
            assertTrue(connections.last().disconnected)
        }
    }
    @Test fun malformedAndOversizedResponsesFailClosedAndDisconnect() {
        for (body in listOf("{}", "not json", "x".repeat(16385))) {
            responseBody = body.toByteArray()
            assertNotNull(runCatching { request() }.exceptionOrNull())
            assertTrue(connections.last().disconnected)
        }
    }
    @Test fun outageCannotExtendLeaseAndRecoveryRequiresNewSignedResponse() {
        val cache = VerifiedAccessCache(context,key.public,wallTime={now})
        respondWithLease()
        cache.save(request(),account,"test.parent","test-receipt")
        val originalDeadline = cache.read(account)!!.refreshBy
        unavailable = true
        now += 1000
        assertTrue(runCatching { request() }.exceptionOrNull() is IOException)
        assertTrue(connections.last().disconnected)
        assertEquals(originalDeadline,cache.read(account)!!.refreshBy)
        now = originalDeadline
        assertNull("An unavailable server cannot extend offline access",cache.read(account))
        assertTrue(runCatching { request() }.exceptionOrNull() is IOException)
        assertNull(cache.read(account))
        unavailable = false
        now += 1000
        respondWithLease()
        cache.save(request(),account,"test.parent","test-receipt")
        assertTrue("Recovery must use the new server-signed deadline",cache.read(account)!!.refreshBy>originalDeadline)
        assertNull(cache.read(AmazonBilling.accountHash("another-user")))
    }
}
