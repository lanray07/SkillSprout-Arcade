package com.skillsprout.arcade

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PackFileSelectionTest {
    private fun requested(limit:Int=8)=PackFileSelection(limit).apply {request {}}
    @Test fun selectedBytesAreReadExactlyAndStreamCloses()=runBlocking {
        val owner=requested();var closed=false
        owner.complete {object:ByteArrayInputStream(byteArrayOf(1,2,3)) {override fun close() {closed=true;super.close()}}}
        assertEquals(PackSelectionStatus.READY,owner.selection.value.status)
        assertArrayEquals(byteArrayOf(1,2,3),owner.selection.value.bytes);assertTrue(closed)
    }
    @Test fun exactLimitAcceptedAndOneByteOverRejected()=runBlocking {
        val exact=requested();exact.complete {ByteArrayInputStream(ByteArray(8))};assertEquals(PackSelectionStatus.READY,exact.selection.value.status)
        val over=requested();over.complete {ByteArrayInputStream(ByteArray(9))};assertEquals(PackSelectionStatus.FAILED,over.selection.value.status);assertNull(over.selection.value.bytes)
    }
    @Test fun emptyNullAndUnavailableStreamsFailWithoutSelectedBytes()=runBlocking {
        for(open in listOf<()->InputStream?>({ByteArrayInputStream(byteArrayOf())},{null},{throw SecurityException("private provider detail")})) {
            val owner=requested();owner.complete(open);assertEquals(PackSelectionStatus.FAILED,owner.selection.value.status);assertNull(owner.selection.value.bytes)
        }
    }
    @Test fun readAndCloseFailuresDiscardPartialBytes()=runBlocking {
        val read=requested();var closed=false
        read.complete {object:InputStream() {override fun read():Int=throw IOException("private detail");override fun close() {closed=true}}}
        assertTrue(closed);assertEquals(PackSelectionStatus.FAILED,read.selection.value.status);assertNull(read.selection.value.bytes)
        val close=requested();close.complete {object:ByteArrayInputStream(byteArrayOf(1)) {override fun close() {throw IOException("close")}}}
        assertEquals(PackSelectionStatus.FAILED,close.selection.value.status);assertNull(close.selection.value.bytes)
    }
    @Test fun zeroLengthBulkReadFallsBackAndDoesNotSpin()=runBlocking {
        val owner=requested();var first=true
        owner.complete {object:ByteArrayInputStream(byteArrayOf(1,2)) {
            override fun read(b:ByteArray,off:Int,len:Int):Int {if(first) {first=false;return 0};return super.read(b,off,len)}
        }}
        assertArrayEquals(byteArrayOf(1,2),owner.selection.value.bytes)
    }
    @Test fun zeroReadFallbackStillEnforcesLimit()=runBlocking {
        val owner=requested(1)
        owner.complete {object:ByteArrayInputStream(byteArrayOf(1,2)) {override fun read(b:ByteArray,off:Int,len:Int)=0}}
        assertEquals(PackSelectionStatus.FAILED,owner.selection.value.status)
    }
    @Test fun cancellationClearsPreviousSelectionAndRetryIsFresh()=runBlocking {
        val owner=requested();owner.complete {ByteArrayInputStream(byteArrayOf(1))}
        owner.request {};assertNull(owner.selection.value.bytes);owner.complete(null)
        assertEquals(PackSelectionStatus.CANCELLED,owner.selection.value.status)
        owner.request {};owner.complete {ByteArrayInputStream(byteArrayOf(2))};assertArrayEquals(byteArrayOf(2),owner.selection.value.bytes)
    }
    @Test fun launchFailureHasGenericFailureState() {
        val owner=PackFileSelection();owner.request {throw IllegalStateException("private detail")}
        assertEquals(PackSelectionStatus.FAILED,owner.selection.value.status);assertNull(owner.selection.value.bytes)
    }
    @Test fun repeatedRequestsAndCallbacksDoNotOverlapOrReplaceReadyBytes()=runBlocking {
        val owner=PackFileSelection();var launches=0;var opens=0
        owner.request {launches++};owner.request {launches++}
        owner.complete {opens++;ByteArrayInputStream(byteArrayOf(1))}
        owner.complete {opens++;ByteArrayInputStream(byteArrayOf(2))}
        assertEquals(1,launches);assertEquals(1,opens);assertArrayEquals(byteArrayOf(1),owner.selection.value.bytes)
    }
    @Test fun recreatedOwnerRejectsUnrequestedResultWithoutOpeningProvider()=runBlocking {
        val owner=PackFileSelection();var opened=false
        owner.complete {opened=true;ByteArrayInputStream(byteArrayOf(1))}
        assertFalse(opened);assertEquals(PackSelectionStatus.FAILED,owner.selection.value.status)
    }
    @Test fun closedOwnerCannotRequestOrPublishLateResult()=runBlocking {
        val owner=requested();var closed=false
        owner.complete {object:ByteArrayInputStream(byteArrayOf(1)) {override fun close() {closed=true;owner.close();super.close()}}}
        assertTrue(closed);assertEquals(PackSelectionStatus.IDLE,owner.selection.value.status);assertNull(owner.selection.value.bytes)
        var launched=false;owner.request {launched=true};assertFalse(launched)
    }
    @Test fun clearInvalidatesInFlightReadAndAllowsNewSelection()=runBlocking {
        val owner=requested()
        owner.complete {object:ByteArrayInputStream(byteArrayOf(1)) {override fun close() {owner.clear();super.close()}}}
        assertEquals(PackSelectionStatus.IDLE,owner.selection.value.status);assertNull(owner.selection.value.bytes)
        owner.request {};owner.complete {ByteArrayInputStream(byteArrayOf(2))};assertArrayEquals(byteArrayOf(2),owner.selection.value.bytes)
    }
    @Test fun coroutineCancellationPropagatesAndClosesStream()=runBlocking {
        val owner=requested();var closed=false
        try {
            owner.complete {object:InputStream() {override fun read():Int=throw CancellationException("cancelled");override fun close() {closed=true}}}
            fail("Cancellation must propagate")
        } catch(_:CancellationException) {}
        assertTrue(closed);assertEquals(PackSelectionStatus.CANCELLED,owner.selection.value.status);assertNull(owner.selection.value.bytes)
    }
}
