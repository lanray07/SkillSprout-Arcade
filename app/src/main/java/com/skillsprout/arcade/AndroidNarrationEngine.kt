package com.skillsprout.arcade

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice

/** Platform adapter: all callbacks are posted after construction onto the main thread. */
internal class AndroidNarrationEngine(context:Context):NarrationEngine {
    private val context=context.applicationContext
    private val main=Handler(Looper.getMainLooper())
    private var tts:TextToSpeech?=null
    private var knownVoices=emptyMap<String,Voice>()
    private var closed=false

    override fun initialize(onResult:(Boolean)->Unit) {
        check(tts==null && !closed)
        tts=TextToSpeech(context) {status->
            main.post {if(!closed) onResult(status==TextToSpeech.SUCCESS)}
        }
        val listenerStatus=tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener() {
            override fun onStart(utteranceId:String?) {}
            override fun onDone(utteranceId:String?) {}
            override fun onError(utteranceId:String?) {failed()}
            override fun onError(utteranceId:String?,errorCode:Int) {failed()}
            private fun failed() {main.post {if(!closed) onResult(false)}}
        })
        if(listenerStatus!=TextToSpeech.SUCCESS) main.post {if(!closed) onResult(false)}
    }

    override fun voices():List<NarrationVoice> {
        knownVoices=tts?.voices.orEmpty().associateBy {it.name}
        return knownVoices.values.map {NarrationVoice(it.name,it.locale.language,it.locale.country,
            it.isNetworkConnectionRequired,TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty(),it.quality)}
    }

    override fun selectVoice(name:String):Boolean {
        val voice=knownVoices[name] ?: return false
        return tts?.setVoice(voice)==TextToSpeech.SUCCESS
    }

    override fun speak(text:String,volume:Float):Boolean = tts?.speak(text,TextToSpeech.QUEUE_FLUSH,
        Bundle().apply {putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,volume)},"pip")==TextToSpeech.SUCCESS

    override fun stop() {tts?.stop()}
    override fun shutdown() {
        closed=true
        val old=tts
        tts=null
        knownVoices=emptyMap()
        old?.shutdown()
    }
}
