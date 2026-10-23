package com.skillsprout.arcade

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class NarrationVoice(val name:String,val language:String,val country:String,
    val networkRequired:Boolean=false,val installed:Boolean=true,val quality:Int=0)

internal interface NarrationEngine {
    fun initialize(onResult:(Boolean)->Unit)
    fun voices():List<NarrationVoice>
    fun selectVoice(name:String):Boolean
    fun speak(text:String,volume:Float):Boolean
    fun stop()
    fun shutdown()
}

/** Main-thread owner. Foreground entry retries; stale engine callbacks grant no readiness. */
internal class OfflineNarration(private val createEngine:()->NarrationEngine) {
    private val ready=MutableStateFlow(false)
    val available:StateFlow<Boolean> = ready.asStateFlow()
    private var engine:NarrationEngine?=null
    private var generation=0L
    private var closed=false

    fun start() {
        if(closed) return
        release()
        val current=runCatching(createEngine).getOrNull() ?: return
        engine=current
        val token=generation
        runCatching {
            current.initialize { success->
                if(!closed && token==generation && engine===current) {
                    if(!success) release() else {
                        val candidates=runCatching {current.voices()}.getOrDefault(emptyList())
                            .filter {it.language.equals("en",true) && !it.networkRequired && it.installed}
                            .sortedWith(compareByDescending<NarrationVoice> {it.country.equals("GB",true)}
                                .thenByDescending {it.quality}.thenBy {it.name})
                        ready.value=candidates.any {runCatching {current.selectVoice(it.name)}.getOrDefault(false)}
                        if(!ready.value) release()
                    }
                }
            }
        }.onFailure {if(token==generation && engine===current) release()}
    }

    fun speak(text:String,volume:Float) {
        val current=engine ?: return
        if(!ready.value || text.isBlank() || !volume.isFinite() || volume<=0f) return
        if(!runCatching {current.speak(text,volume.coerceIn(0f,1f))}.getOrDefault(false)) release()
    }

    fun stop() {
        val current=engine ?: return
        if(runCatching {current.stop()}.isFailure) release()
    }

    fun background() {release()}
    fun close() {closed=true;release()}

    private fun release() {
        generation++
        ready.value=false
        val old=engine
        engine=null
        if(old!=null) {
            runCatching {old.stop()}
            runCatching {old.shutdown()}
        }
    }
}
