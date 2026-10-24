package com.skillsprout.arcade

import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class ProgressExportStatus(val busy:Boolean=false) {
    IDLE,CHOOSING(true),WRITING(true),SAVED,CANCELLED,FAILED
}

/** Main-thread owner. A report snapshot lives only until its picker result is consumed. */
internal class ProgressReportExport {
    private val state=MutableStateFlow(ProgressExportStatus.IDLE)
    val status=state.asStateFlow()
    private var pending:String?=null
    private var closed=false

    fun request(csv:String,launchPicker:()->Unit) {
        if(closed || state.value.busy) return
        pending=csv
        state.value=ProgressExportStatus.CHOOSING
        try {launchPicker()} catch(cancelled:CancellationException) {
            pending=null
            state.value=ProgressExportStatus.CANCELLED
            throw cancelled
        } catch(_:Exception) {
            pending=null
            state.value=ProgressExportStatus.FAILED
        }
    }

    suspend fun complete(openDestination:(()->OutputStream?)?) {
        if(closed || state.value !in listOf(ProgressExportStatus.IDLE,ProgressExportStatus.CHOOSING)) return
        val csv=pending
        pending=null
        if(openDestination==null) {state.value=ProgressExportStatus.CANCELLED;return}
        // A recreated activity has no snapshot: never create an empty/wrong report.
        if(csv==null) {state.value=ProgressExportStatus.FAILED;return}
        state.value=ProgressExportStatus.WRITING
        try {
            withContext(Dispatchers.IO) {
                currentCoroutineContext().ensureActive()
                val stream=openDestination() ?: throw IOException("No output stream")
                stream.use {
                    currentCoroutineContext().ensureActive()
                    it.write(csv.toByteArray(Charsets.UTF_8))
                    it.flush()
                    currentCoroutineContext().ensureActive()
                }
            }
            if(!closed) state.value=ProgressExportStatus.SAVED
        } catch(cancelled:CancellationException) {
            if(!closed) state.value=ProgressExportStatus.CANCELLED
            throw cancelled
        } catch(_:Exception) {
            if(!closed) state.value=ProgressExportStatus.FAILED
        }
    }

    fun close() {closed=true;pending=null;state.value=ProgressExportStatus.IDLE}
}
