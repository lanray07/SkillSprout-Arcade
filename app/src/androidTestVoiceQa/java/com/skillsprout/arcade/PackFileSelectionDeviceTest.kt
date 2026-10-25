package com.skillsprout.arcade

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FilterInputStream
import java.io.RandomAccessFile
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Private generated files only. Does not install a pack or open the document-picker UI. */
@RunWith(AndroidJUnit4::class)
class PackFileSelectionDeviceTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun guard() {
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.voiceqa")
        check(context.packageName=="com.skillsprout.arcade.voiceqa" && !BuildConfig.PURCHASING_ALLOWED)
    }
    private fun record(name:String,value:JSONObject) {
        val dir=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(dir,name+".json").writeText(value.put("model",android.os.Build.MODEL).put("api",android.os.Build.VERSION.SDK_INT)
            .put("profilesOrPinOpened",false).put("documentPickerUiTested",false).put("packInstalled",false)
            .put("productionApproval",false).toString(2))
    }
    @Test fun actualContentResolverReadsExactBytesAndClosesStream()=runBlocking {
        guard();val file=File.createTempFile("pack-read-qa-",".json",context.cacheDir)
        val owner=PackFileSelection();var closed=false
        try {
            val bytes="{\"fixture\":\"é\"}".toByteArray(Charsets.UTF_8);file.writeBytes(bytes);owner.request {}
            owner.complete {object:FilterInputStream(requireNotNull(context.contentResolver.openInputStream(Uri.fromFile(file)))) {
                override fun close() {closed=true;super.close()}
            }}
            assertEquals(PackSelectionStatus.READY,owner.selection.value.status);assertArrayEquals(bytes,owner.selection.value.bytes)
            assertTrue(closed);owner.clear();assertNull(owner.selection.value.bytes)
            record("pack-file-read",JSONObject().put("exactUtf8BytesMatch",true).put("streamClosed",true).put("clearReleasesSelection",true))
        } finally {owner.close();check(file.delete())}
    }
    @Test fun missingEmptyAndOversizedFilesFailAndFreshRetrySucceeds()=runBlocking {
        guard();val file=File.createTempFile("pack-size-qa-",".json",context.cacheDir)
        val missing=File(context.cacheDir,"absent-pack-"+java.util.UUID.randomUUID()+".json")
        val owner=PackFileSelection()
        try {
            owner.request {};owner.complete {context.contentResolver.openInputStream(Uri.fromFile(missing))}
            assertEquals(PackSelectionStatus.FAILED,owner.selection.value.status)
            owner.request {};owner.complete {context.contentResolver.openInputStream(Uri.fromFile(file))}
            assertEquals(PackSelectionStatus.FAILED,owner.selection.value.status)
            RandomAccessFile(file,"rw").use {it.setLength(7_000_001)}
            owner.request {};owner.complete {context.contentResolver.openInputStream(Uri.fromFile(file))}
            assertEquals(PackSelectionStatus.FAILED,owner.selection.value.status);assertNull(owner.selection.value.bytes)
            file.writeBytes(byteArrayOf(1,2));owner.request {};owner.complete {context.contentResolver.openInputStream(Uri.fromFile(file))}
            assertEquals(PackSelectionStatus.READY,owner.selection.value.status);assertArrayEquals(byteArrayOf(1,2),owner.selection.value.bytes)
            record("pack-file-errors",JSONObject().put("actualMissingFileRejected",true).put("emptyFileRejected",true)
                .put("overDefaultSevenMillionByteLimitRejected",true).put("freshRetrySucceeds",true))
        } finally {owner.close();check(file.delete())}
    }
}
