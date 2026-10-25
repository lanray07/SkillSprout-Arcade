package com.skillsprout.arcade

import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class PackSelectionStatus(val busy:Boolean=false) { IDLE,CHOOSING(true),READING(true),READY,CANCELLED,FAILED }
data class PackSelection(val status:PackSelectionStatus=PackSelectionStatus.IDLE,val bytes:ByteArray?=null)

/** Activity-owned memory only. Selecting a file never installs it. Main-thread entry points. */
internal class PackFileSelection(private val limit:Int=PackReadLimits.SIGNED) {
    init {require(limit>0 && limit<Int.MAX_VALUE)}
    private val state=MutableStateFlow(PackSelection())
    val selection=state.asStateFlow()
    private var closed=false
    private var generation=0L
    fun request(launch:()->Unit) {
        if(closed || state.value.status.busy) return
        generation++;state.value=PackSelection(PackSelectionStatus.CHOOSING)
        try {launch()} catch(cancelled:CancellationException) {
            state.value=PackSelection(PackSelectionStatus.CANCELLED);throw cancelled
        } catch(_:Exception) {state.value=PackSelection(PackSelectionStatus.FAILED)}
    }
    suspend fun complete(open:(()->InputStream?)?) {
        if(closed || state.value.status==PackSelectionStatus.READING) return
        if(state.value.status!=PackSelectionStatus.CHOOSING) {
            if(state.value.status==PackSelectionStatus.IDLE && open!=null) state.value=PackSelection(PackSelectionStatus.FAILED)
            return
        }
        if(open==null) {state.value=PackSelection(PackSelectionStatus.CANCELLED);return}
        val owner=generation;state.value=PackSelection(PackSelectionStatus.READING)
        try {
            val bytes=withContext(Dispatchers.IO) {
                val readContext=currentCoroutineContext();readContext.ensureActive()
                (open() ?: throw IOException("No input stream")).use {stream->
                    stream.readBytesBounded(limit) {readContext.ensureActive()}
                        .also {if(it.isEmpty()) throw IOException("Empty pack")}
                }
            }
            if(!closed && owner==generation) state.value=PackSelection(PackSelectionStatus.READY,bytes)
        } catch(cancelled:CancellationException) {
            if(!closed && owner==generation) state.value=PackSelection(PackSelectionStatus.CANCELLED)
            throw cancelled
        } catch(_:Exception) {if(!closed && owner==generation) state.value=PackSelection(PackSelectionStatus.FAILED)}
    }
    fun clear() {generation++;state.value=PackSelection()}
    fun close() {closed=true;clear()}
}
