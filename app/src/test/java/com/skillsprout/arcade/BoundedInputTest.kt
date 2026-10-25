package com.skillsprout.arcade

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class BoundedInputTest {
    @Test fun exactLimitPreservesEveryByte() {val bytes=byteArrayOf(0,1,127,-1);assertArrayEquals(bytes,ByteArrayInputStream(bytes).readBytesBounded(4))}
    @Test fun emptyAndZeroLimitAreSupported() {assertArrayEquals(byteArrayOf(),ByteArrayInputStream(byteArrayOf()).readBytesBounded(0));assertThrows(IOException::class.java) {ByteArrayInputStream(byteArrayOf(1)).readBytesBounded(0)}}
    @Test fun overLimitStopsAtOneDetectionByteWithoutDrainingStream() {
        var bytesRead=0
        val stream=object:InputStream() {
            override fun available()=0
            override fun read():Int {bytesRead++;return 1}
            override fun read(b:ByteArray,off:Int,len:Int):Int {b.fill(1,off,off+len);bytesRead+=len;return len}
        }
        assertThrows(IOException::class.java) {stream.readBytesBounded(10_000)};assertEquals(10_001,bytesRead)
    }
    @Test fun shortReadsAndMisleadingAvailableDoNotTruncateData() {
        val bytes=ByteArray(17){it.toByte()}
        val stream=object:ByteArrayInputStream(bytes) {override fun available()=0;override fun read(b:ByteArray,off:Int,len:Int)=super.read(b,off,minOf(len,2))}
        assertArrayEquals(bytes,stream.readBytesBounded(17))
    }
    @Test fun zeroBulkReadUsesSingleByteFallback() {
        val stream=object:ByteArrayInputStream(byteArrayOf(1,2,3)) {override fun read(b:ByteArray,off:Int,len:Int)=0}
        assertArrayEquals(byteArrayOf(1,2,3),stream.readBytesBounded(3))
    }
    @Test fun fallbackEnforcesTheSameLimit() {
        val stream=object:ByteArrayInputStream(byteArrayOf(1,2,3)) {override fun read(b:ByteArray,off:Int,len:Int)=0}
        assertThrows(IOException::class.java) {stream.readBytesBounded(2)}
    }
    @Test fun invalidLimitsAreRejectedBeforeReading() {
        var read=false;val stream=object:InputStream() {override fun read():Int {read=true;return -1}}
        for(limit in listOf(-1,Int.MAX_VALUE)) assertThrows(IllegalArgumentException::class.java) {stream.readBytesBounded(limit)}
        assertFalse(read)
    }
    @Test fun cancellationCheckStopsBeforeInputAndPropagates() {
        var read=false;val stream=object:InputStream() {override fun read():Int {read=true;return 1}}
        assertThrows(CancellationException::class.java) {stream.readBytesBounded(1) {throw CancellationException("fixture")}}
        assertFalse(read)
    }
    @Test fun callerUseClosesStreamOnLimitFailure() {
        var closed=false;val stream=object:ByteArrayInputStream(byteArrayOf(1,2)) {override fun close() {closed=true;super.close()}}
        assertThrows(IOException::class.java) {stream.use {it.readBytesBounded(1)}};assertTrue(closed)
    }
    @Test fun readerDoesNotTakeOwnershipOrSwallowReadErrors() {
        var closed=false;val stream=object:InputStream() {override fun read():Int=throw IOException("fixture");override fun close() {closed=true}}
        assertThrows(IOException::class.java) {stream.readBytesBounded(1)};assertFalse(closed)
    }
}
