package com.skillsprout.arcade

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class PrivacyNoticeTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun offlineNoticeDoesNotClaimThatThisBuildVerifiesPurchases() {
        val paragraphs=privacyParagraphs(context,false)
        assertTrue(paragraphs.any {it.contains("Purchasing is disabled")})
        assertFalse(paragraphs.any {it.contains("signed, account-bound")})
        assertTrue(paragraphs.any {it.contains("within 90 days")})
    }
    @Test fun amazonNoticeDisclosesAutomaticVerificationAndPseudonymousIdentifiers() {
        val paragraphs=privacyParagraphs(context,true)
        assertTrue(paragraphs.any {it.contains("automatically")})
        assertTrue(paragraphs.any {it.contains("signed, account-bound")})
        assertFalse(paragraphs.any {it.contains("Purchasing is disabled")})
        assertTrue(paragraphs.any {it.contains("banksmi@mail.com")})
    }
}
