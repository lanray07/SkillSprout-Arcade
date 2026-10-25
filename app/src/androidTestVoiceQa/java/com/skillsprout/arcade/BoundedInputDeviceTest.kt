package com.skillsprout.arcade

import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.RandomAccessFile
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Generated cache files only; never enumerates or modifies the user's installed packs. */
@RunWith(AndroidJUnit4::class)
class BoundedInputDeviceTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun guard() {
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.voiceqa")
        check(context.packageName=="com.skillsprout.arcade.voiceqa" && !BuildConfig.PURCHASING_ALLOWED)
    }
    private fun record(name:String,value:JSONObject) {
        val dir=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(dir,name+".json").writeText(value.put("model",android.os.Build.MODEL).put("api",android.os.Build.VERSION.SDK_INT)
            .put("generatedCacheFilesOnly",true).put("installedPacksOrProfilesOpened",false)
            .put("packInstalled",false).put("productionApproval",false).toString(2))
    }
    @Test fun exactDefaultLimitLoadsAndOverLimitFailsWithClosedStream() {
        guard();val file=File.createTempFile("bounded-qa-",".bin",context.cacheDir)
        try {
            RandomAccessFile(file,"rw").use {it.setLength(PackReadLimits.SIGNED.toLong())}
            val exact=file.inputStream().use {it.readBytesBounded(PackReadLimits.SIGNED)}
            assertEquals(PackReadLimits.SIGNED,exact.size)
            RandomAccessFile(file,"rw").use {it.setLength(PackReadLimits.SIGNED.toLong()+1)}
            var closed=false
            try {
                object:FilterInputStream(file.inputStream()) {override fun close() {closed=true;super.close()}}
                    .use {it.readBytesBounded(PackReadLimits.SIGNED)}
                fail("Oversized input must be rejected")
            } catch(_:IOException) {}
            assertTrue(closed)
            record("bounded-input-limit",JSONObject().put("exactSevenMillionBytesAccepted",true)
                .put("oneByteOverRejected",true).put("failureStreamClosed",true))
        } finally {check(file.delete())}
    }
    @Test fun atomicBackupRecoveryPreservesBytesUnderBoundedRead() {
        guard();val file=File.createTempFile("bounded-atomic-qa-",".sproutpack",context.cacheDir)
        val backup=File(file.path+".bak");val pending=File(file.path+".new")
        try {
            val bytes="generated é fixture".toByteArray(Charsets.UTF_8)
            file.writeBytes(bytes);assertTrue(file.renameTo(backup))
            val loaded=AtomicFile(file).openRead().use {it.readBytesBounded(64)}
            assertArrayEquals(bytes,loaded);assertTrue(file.exists());assertFalse(backup.exists())
            val replacement="new fixture".toByteArray(Charsets.UTF_8)
            val atomic=AtomicFile(file);val output=atomic.startWrite()
            try {output.write(replacement);atomic.finishWrite(output)} catch(error:Exception) {atomic.failWrite(output);throw error}
            assertArrayEquals(replacement,atomic.openRead().use {it.readBytesBounded(64)})
            record("bounded-input-atomic",JSONObject().put("actualAtomicBackupRecovered",true).put("utf8BytesMatch",true)
                .put("actualAtomicExistingReplacementWorks",true))
        } finally {
            for(owned in listOf(file,backup,pending)) if(owned.exists()) check(owned.delete())
        }
    }
}
