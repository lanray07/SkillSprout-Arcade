package com.skillsprout.arcade

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ProgressReportExportTest {
    private class Stream(val fail:String=""):ByteArrayOutputStream() {
        var closed=false
        override fun write(bytes:ByteArray,offset:Int,length:Int) {
            if(fail=="write") {super.write(bytes,offset,1);throw IOException("write")}
            super.write(bytes,offset,length)
        }
        override fun flush() {if(fail=="flush") throw IOException("flush")}
        override fun close() {closed=true;if(fail=="close") throw IOException("close");super.close()}
    }
    @Test fun successWritesUtf8SnapshotAndClosesStream()=runBlocking {
        val export=ProgressReportExport();val stream=Stream();val csv="nickname,correct\nÉloïse,true\n"
        export.request(csv) {};assertEquals(ProgressExportStatus.CHOOSING,export.status.value)
        export.complete {stream}
        assertArrayEquals(csv.toByteArray(Charsets.UTF_8),stream.toByteArray())
        assertTrue(stream.closed);assertEquals(ProgressExportStatus.SAVED,export.status.value)
    }
    @Test fun pickerCancellationDropsOldSnapshotAndAllowsFreshReport()=runBlocking {
        val export=ProgressReportExport();export.request("old") {};export.complete(null)
        assertEquals(ProgressExportStatus.CANCELLED,export.status.value)
        val stream=Stream();export.request("fresh") {};export.complete {stream}
        assertEquals("fresh",stream.toString("UTF-8"));assertEquals(ProgressExportStatus.SAVED,export.status.value)
    }
    @Test fun nullDestinationStreamFailsWithoutCrashing()=runBlocking {
        val export=ProgressReportExport();export.request("report") {};export.complete {null}
        assertEquals(ProgressExportStatus.FAILED,export.status.value)
    }
    @Test fun permissionAndProviderExceptionsFailWithoutLeakingDetails()=runBlocking {
        for(error in listOf(SecurityException("private-provider-detail"),IOException("private-path"))) {
            val export=ProgressReportExport();export.request("report") {};export.complete {throw error}
            assertEquals(ProgressExportStatus.FAILED,export.status.value)
        }
    }
    @Test fun partialWriteFlushAndCloseErrorsAreFailuresAndCloseTheStream()=runBlocking {
        for(phase in listOf("write","flush","close")) {
            val export=ProgressReportExport();val stream=Stream(phase)
            export.request("report") {};export.complete {stream}
            assertTrue(stream.closed);assertEquals(ProgressExportStatus.FAILED,export.status.value)
        }
    }
    @Test fun secondPickerRequestCannotReplacePendingSnapshot()=runBlocking {
        val export=ProgressReportExport();var launches=0
        export.request("first") {launches++};export.request("second") {launches++}
        val stream=Stream();export.complete {stream}
        assertEquals(1,launches);assertEquals("first",stream.toString("UTF-8"))
    }
    @Test fun duplicateCompletedResultCannotWriteAgain()=runBlocking {
        val export=ProgressReportExport();var opened=0
        export.request("report") {};export.complete {opened++;Stream()};export.complete {opened++;Stream()}
        assertEquals(1,opened);assertEquals(ProgressExportStatus.SAVED,export.status.value)
    }
    @Test fun concurrentRequestAndResultDoNotInterruptCurrentWrite()=runBlocking {
        val entered=CountDownLatch(1);val release=CountDownLatch(1);val export=ProgressReportExport()
        val stream=object:ByteArrayOutputStream() {
            override fun write(bytes:ByteArray,offset:Int,length:Int) {entered.countDown();check(release.await(5,TimeUnit.SECONDS));super.write(bytes,offset,length)}
        }
        export.request("first") {}
        val writer=launch {export.complete {stream}}
        try {
            assertTrue(withContext(Dispatchers.IO) {entered.await(5,TimeUnit.SECONDS)})
            assertEquals(ProgressExportStatus.WRITING,export.status.value)
            export.request("second") {fail("Overlapping picker")}
            export.complete {fail("Duplicate destination");null}
            assertEquals(ProgressExportStatus.WRITING,export.status.value)
        } finally {release.countDown();writer.join()}
        assertEquals("first",stream.toString("UTF-8"));assertEquals(ProgressExportStatus.SAVED,export.status.value)
    }
    @Test fun pickerLaunchFailureClearsSnapshotAndCanRetry()=runBlocking {
        val export=ProgressReportExport();export.request("old") {throw IllegalStateException("No document app")}
        assertEquals(ProgressExportStatus.FAILED,export.status.value)
        export.complete {fail("Failed request must not open a destination");null}
        val stream=Stream();export.request("new") {};export.complete {stream}
        assertEquals("new",stream.toString("UTF-8"))
    }
    @Test fun resultWithoutSnapshotNeverCreatesEmptyFile()=runBlocking {
        val recreated=ProgressReportExport();recreated.complete {fail("Missing snapshot");null}
        assertEquals(ProgressExportStatus.FAILED,recreated.status.value)
    }
    @Test fun closeClearsPendingRequestAndRejectsLateResults()=runBlocking {
        val export=ProgressReportExport();export.request("report") {};export.close()
        export.complete {fail("Closed owner");null};export.request("again") {fail("Closed owner")}
        assertEquals(ProgressExportStatus.IDLE,export.status.value)
    }
    @Test fun cancellationDuringWritePropagatesAndClosesStream()=runBlocking {
        val entered=CountDownLatch(1);val release=CountDownLatch(1);val export=ProgressReportExport();var closed=false
        val stream=object:ByteArrayOutputStream() {
            override fun write(bytes:ByteArray,offset:Int,length:Int) {entered.countDown();check(release.await(5,TimeUnit.SECONDS));super.write(bytes,offset,length)}
            override fun close() {closed=true;super.close()}
        }
        export.request("report") {};val writer=launch {export.complete {stream}}
        try {assertTrue(withContext(Dispatchers.IO) {entered.await(5,TimeUnit.SECONDS)});writer.cancel()}
        finally {release.countDown();writer.join()}
        assertTrue(writer.isCancelled);assertTrue(closed);assertEquals(ProgressExportStatus.CANCELLED,export.status.value)
    }
    @Test fun closeDuringWriteCannotPublishStaleSuccess()=runBlocking {
        val entered=CountDownLatch(1);val release=CountDownLatch(1);val export=ProgressReportExport()
        val stream=object:ByteArrayOutputStream() {
            override fun write(bytes:ByteArray,offset:Int,length:Int) {entered.countDown();check(release.await(5,TimeUnit.SECONDS));super.write(bytes,offset,length)}
        }
        export.request("report") {};val writer=launch {export.complete {stream}}
        try {assertTrue(withContext(Dispatchers.IO) {entered.await(5,TimeUnit.SECONDS)});export.close()}
        finally {release.countDown();writer.join()}
        assertEquals(ProgressExportStatus.IDLE,export.status.value)
    }
}
