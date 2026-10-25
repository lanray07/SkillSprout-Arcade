package com.skillsprout.arcade

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic in-memory vault only. Never creates ParentSecurity or accesses a stored credential. */
@RunWith(AndroidJUnit4::class)
class PinSubmissionDeviceTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun guard() {
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.voiceqa")
        check(context.packageName=="com.skillsprout.arcade.voiceqa" && !BuildConfig.PURCHASING_ALLOWED)
    }
    private fun record(name:String,value:JSONObject) {
        val dir=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(dir,name+".json").writeText(value.put("model",android.os.Build.MODEL).put("api",android.os.Build.VERSION.SDK_INT)
            .put("syntheticVaultOnly",true).put("storedCredentialOrProfileOpened",false).put("keystoreTested",false)
            .put("pinUiOrSpokenFeedbackTested",false).put("productionApproval",false).toString(2))
    }
    @Test fun syntheticSetupAndVerificationRunOffAndroidMainThread()=runBlocking {
        guard();var stored="";var offMain=true;var lockCalls=0
        val vault=object:PinVault {
            override val configured=true
            override fun setPin(pin:String) {offMain=offMain && Thread.currentThread()!=Looper.getMainLooper().thread;stored=pin}
            override fun verify(pin:String):Boolean {offMain=offMain && Thread.currentThread()!=Looper.getMainLooper().thread;return pin==stored}
            override fun secondsLocked():Long {lockCalls++;offMain=offMain && Thread.currentThread()!=Looper.getMainLooper().thread;return 60}
        }
        withContext(Dispatchers.Main) {
            assertSame(Looper.getMainLooper().thread,Thread.currentThread())
            assertEquals(PinSubmissionStatus.MATCH,submitParentPin(vault,true,"654321","654321").status)
            assertEquals(PinSubmissionStatus.MATCH,submitParentPin(vault,false,"654321").status)
            assertEquals(PinSubmissionResult(PinSubmissionStatus.LOCKED,60),submitParentPin(vault,false,"123456"))
        }
        assertTrue(offMain);assertEquals(1,lockCalls)
        stored=""
        record("pin-submission-threading",JSONObject().put("syntheticSuccessAndLockedResultsCorrect",true)
            .put("callerAndroidMainConfirmed",true).put("vaultCallsOffAndroidMain",true).put("singlePostFailureLockQuery",true))
    }
    @Test fun syntheticErrorsAndCancellationNeverReportSuccess()=runBlocking {
        guard();var cancel=false;var lockFailure=false
        val vault=object:PinVault {
            override val configured=true
            override fun setPin(pin:String)=throw IOException("synthetic write error")
            override fun verify(pin:String):Boolean {
                if(cancel) throw CancellationException("synthetic cancellation")
                if(lockFailure) return false
                throw IOException("synthetic verify error")
            }
            override fun secondsLocked():Long=throw IOException("synthetic lock error")
        }
        assertEquals(PinSubmissionStatus.UNAVAILABLE,submitParentPin(vault,true,"654321","654321").status)
        assertEquals(PinSubmissionStatus.UNAVAILABLE,submitParentPin(vault,false,"654321").status)
        lockFailure=true;assertEquals(PinSubmissionStatus.UNAVAILABLE,submitParentPin(vault,false,"654321").status)
        cancel=true
        try {submitParentPin(vault,false,"654321");fail("Cancellation must propagate")} catch(_:CancellationException) {}
        record("pin-submission-errors",JSONObject().put("syntheticWriteVerifyAndLockFailuresUnavailable",true)
            .put("cancellationPropagates",true))
    }
}
