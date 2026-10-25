package com.skillsprout.arcade

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PinSubmissionTest {
    private class Vault:PinVault {
        override val configured=true
        var sets=0;var verifies=0;var locks=0;var written="";var checked=""
        var match=false;var remaining=0L;var thread:Thread?=null
        var setError:Exception?=null;var verifyError:Exception?=null;var lockError:Exception?=null
        override fun setPin(pin:String) {sets++;written=pin;thread=Thread.currentThread();setError?.let {throw it}}
        override fun verify(pin:String):Boolean {verifies++;checked=pin;thread=Thread.currentThread();verifyError?.let {throw it};return match}
        override fun secondsLocked():Long {locks++;thread=Thread.currentThread();lockError?.let {throw it};return remaining}
    }
    @Test fun inputFilterUsesTheVaultsAsciiDigitContract() {
        assertTrue(acceptsPinInput(""));assertTrue(acceptsPinInput("012345678901"))
        for(value in listOf("1234567890123","１２３４５６","١٢٣٤٥٦","123 456","12345a")) assertFalse(acceptsPinInput(value))
        assertFalse(validParentPin("12345"));assertTrue(validParentPin("012345"));assertTrue(validParentPin("123456789012"))
    }
    @Test fun invalidInputNeverConsumesAVaultAttempt()=runBlocking {
        val vault=Vault()
        for(value in listOf("","12345","1234567890123","１２３４５６")) assertEquals(PinSubmissionStatus.INVALID,submitParentPin(vault,false,value).status)
        assertEquals(0,vault.verifies);assertEquals(0,vault.locks);assertEquals(0,vault.sets)
    }
    @Test fun setupMismatchNeverWritesACredential()=runBlocking {
        val vault=Vault();assertEquals(PinSubmissionStatus.INVALID,submitParentPin(vault,true,"654321","654322").status)
        assertEquals(0,vault.sets);assertEquals(0,vault.verifies)
    }
    @Test fun setupWritesTheExactSnapshotOffTheCallerThread()=runBlocking {
        val caller=Thread.currentThread();val vault=Vault()
        assertEquals(PinSubmissionStatus.MATCH,submitParentPin(vault,true,"654321","654321").status)
        assertEquals("654321",vault.written);assertEquals(1,vault.sets);assertEquals(0,vault.verifies);assertEquals(0,vault.locks)
        assertNotEquals(caller,vault.thread)
    }
    @Test fun matchingVerificationDoesNotQueryLockState()=runBlocking {
        val vault=Vault().apply {match=true}
        assertEquals(PinSubmissionStatus.MATCH,submitParentPin(vault,false,"654321").status)
        assertEquals("654321",vault.checked);assertEquals(1,vault.verifies);assertEquals(0,vault.locks)
    }
    @Test fun wrongPinQueriesLockStateOnceOffTheCallerThread()=runBlocking {
        val caller=Thread.currentThread();val vault=Vault()
        assertEquals(PinSubmissionStatus.NO_MATCH,submitParentPin(vault,false,"654321").status)
        assertEquals(1,vault.verifies);assertEquals(1,vault.locks);assertNotEquals(caller,vault.thread)
    }
    @Test fun activeLockReturnsRemainingSecondsWithoutSuccess()=runBlocking {
        val vault=Vault().apply {remaining=60}
        assertEquals(PinSubmissionResult(PinSubmissionStatus.LOCKED,60),submitParentPin(vault,false,"654321"))
    }
    @Test fun writeFailureReturnsUnavailableWithoutExceptionDetails()=runBlocking {
        val vault=Vault().apply {setError=IOException("private storage detail")}
        assertEquals(PinSubmissionResult(PinSubmissionStatus.UNAVAILABLE),submitParentPin(vault,true,"654321","654321"))
    }
    @Test fun verificationFailureDoesNotQueryLockOrReportMatch()=runBlocking {
        val vault=Vault().apply {verifyError=IOException("private credential detail")}
        assertEquals(PinSubmissionStatus.UNAVAILABLE,submitParentPin(vault,false,"654321").status);assertEquals(0,vault.locks)
    }
    @Test fun lockQueryFailureReturnsUnavailableWithoutOpeningGate()=runBlocking {
        val vault=Vault().apply {lockError=IOException("private lock detail")}
        assertEquals(PinSubmissionStatus.UNAVAILABLE,submitParentPin(vault,false,"654321").status);assertEquals(1,vault.locks)
    }
    @Test fun cancellationPropagatesFromSetupAndVerification()=runBlocking {
        for(setup in listOf(false,true)) {
            val vault=Vault().apply {if(setup) setError=CancellationException("cancel") else verifyError=CancellationException("cancel")}
            try {submitParentPin(vault,setup,"654321","654321");fail("Cancellation must propagate")}
            catch(_:CancellationException) {}
        }
    }
}
