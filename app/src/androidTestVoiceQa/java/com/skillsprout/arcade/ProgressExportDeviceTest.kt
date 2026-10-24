package com.skillsprout.arcade

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FilterOutputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Actual private-file ContentResolver checks; never opens profiles or the document picker. */
@RunWith(AndroidJUnit4::class)
class ProgressExportDeviceTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun guard() {
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.voiceqa")
        check(context.packageName=="com.skillsprout.arcade.voiceqa" && !BuildConfig.PURCHASING_ALLOWED)
    }
    private fun record(name:String,result:JSONObject) {
        val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(folder,name+".json").writeText(result.put("model",android.os.Build.MODEL)
            .put("api",android.os.Build.VERSION.SDK_INT).put("personalDataOrPinOpened",false)
            .put("documentPickerUiTested",false).put("productionApproval",false).toString(2))
    }
    @Test fun utf8ReportTruncatesOwnedFileAndClosesStream()=runBlocking {
        guard()
        val file=File.createTempFile("export-qa-",".csv",context.cacheDir)
        val export=ProgressReportExport()
        try {
            file.writeText("old content that is deliberately longer than the replacement report\n")
            val csv="name,correct\nÉloïse,true\n";var closed=false
            export.request(csv) {}
            export.complete {
                object:FilterOutputStream(requireNotNull(context.contentResolver.openOutputStream(Uri.fromFile(file),"wt"))) {
                    override fun close() {closed=true;super.close()}
                }
            }
            assertEquals(ProgressExportStatus.SAVED,export.status.value)
            assertArrayEquals(csv.toByteArray(Charsets.UTF_8),file.readBytes());assertTrue(closed)
            record("export-write",JSONObject().put("utf8BytesMatch",true).put("oldTrailingBytesRemoved",true).put("streamClosed",true))
        } finally {export.close();check(file.delete())}
    }
    @Test fun actualUnwritableDestinationFailsAndFreshRetrySucceeds()=runBlocking {
        guard()
        val file=File.createTempFile("export-retry-",".csv",context.cacheDir)
        val missing=File(context.cacheDir,"absent-"+java.util.UUID.randomUUID()+"/report.csv")
        val export=ProgressReportExport()
        try {
            export.request("old snapshot") {}
            export.complete {context.contentResolver.openOutputStream(Uri.fromFile(missing),"wt")}
            assertEquals(ProgressExportStatus.FAILED,export.status.value);assertFalse(missing.exists())
            export.request("fresh snapshot") {}
            export.complete {context.contentResolver.openOutputStream(Uri.fromFile(file),"wt")}
            assertEquals(ProgressExportStatus.SAVED,export.status.value);assertEquals("fresh snapshot",file.readText())
            record("export-failure",JSONObject().put("actualMissingDestinationHandled",true).put("freshRetrySaved",true))
        } finally {export.close();check(file.delete())}
    }
}
