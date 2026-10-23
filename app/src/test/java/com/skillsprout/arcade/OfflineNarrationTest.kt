package com.skillsprout.arcade

import org.junit.Assert.*
import org.junit.Test

class OfflineNarrationTest {
    private val uk=NarrationVoice("uk","en","GB")
    private val us=NarrationVoice("us","en","US")
    private class Engine(var result:Boolean?=true,var options:List<NarrationVoice> = emptyList()):NarrationEngine {
        var callback:((Boolean)->Unit)?=null
        val selections=mutableListOf<String>()
        val rejected=mutableSetOf<String>()
        val spoken=mutableListOf<Pair<String,Float>>()
        var stops=0;var shutdowns=0;var voiceQueries=0
        var throwInit=false;var throwVoices=false;var throwSelect=false
        var throwSpeak=false;var throwStop=false;var throwShutdown=false;var speakAccepted=true
        override fun initialize(onResult:(Boolean)->Unit) {callback=onResult;if(throwInit) error("init");result?.let(onResult)}
        override fun voices():List<NarrationVoice> {voiceQueries++;if(throwVoices) error("voices");return options}
        override fun selectVoice(name:String):Boolean {selections+=name;if(throwSelect) error("select");return name !in rejected}
        override fun speak(text:String,volume:Float):Boolean {if(throwSpeak) error("speak");spoken+=text to volume;return speakAccepted}
        override fun stop() {stops++;if(throwStop) error("stop")}
        override fun shutdown() {shutdowns++;if(throwShutdown) error("shutdown")}
    }
    @Test fun readyRequiresSuccessfulInitializationAndVoiceSelection() {
        val engine=Engine(null,listOf(uk));val narrator=OfflineNarration {engine}
        narrator.start();assertFalse(narrator.available.value);narrator.speak("hello",1f);assertTrue(engine.spoken.isEmpty())
        engine.callback!!(true);assertTrue(narrator.available.value);assertEquals(listOf("uk"),engine.selections)
    }
    @Test fun synchronousInitializationUsesAlreadyAssignedEngine() {
        val narrator=OfflineNarration {Engine(true,listOf(uk))};narrator.start();assertTrue(narrator.available.value)
    }
    @Test fun onlyInstalledOfflineEnglishVoicesAreEligible() {
        val engine=Engine(true,listOf(uk.copy(name="network",networkRequired=true),
            uk.copy(name="download",installed=false),uk.copy(name="french",language="fr"),us))
        val narrator=OfflineNarration {engine};narrator.start()
        assertEquals(listOf("us"),engine.selections);assertTrue(narrator.available.value)
    }
    @Test fun ukEnglishIsPreferredBeforeQuality() {
        val engine=Engine(true,listOf(us.copy(quality=500),uk.copy(quality=100)))
        OfflineNarration {engine}.start();assertEquals(listOf("uk"),engine.selections)
    }
    @Test fun rejectedUkVoiceFallsBackToUsEnglish() {
        val engine=Engine(true,listOf(us,uk));engine.rejected+="uk"
        val narrator=OfflineNarration {engine};narrator.start()
        assertEquals(listOf("uk","us"),engine.selections);assertTrue(narrator.available.value)
    }
    @Test fun rejectedOrThrowingSelectionsNeverReportReady() {
        for(throwing in listOf(false,true)) {
            val engine=Engine(true,listOf(uk));engine.rejected+="uk";engine.throwSelect=throwing
            val narrator=OfflineNarration {engine};narrator.start()
            assertFalse(narrator.available.value);assertEquals(1,engine.shutdowns)
        }
    }
    @Test fun emptyOrFailingVoiceInventoryStaysUnavailable() {
        for(throwing in listOf(false,true)) {
            val engine=Engine(true);engine.throwVoices=throwing
            val narrator=OfflineNarration {engine};narrator.start()
            assertFalse(narrator.available.value);assertEquals(1,engine.shutdowns)
        }
    }
    @Test fun failedInitializationDoesNotQueryVoices() {
        val engine=Engine(false,listOf(uk));val narrator=OfflineNarration {engine};narrator.start()
        assertFalse(narrator.available.value);assertEquals(0,engine.voiceQueries);assertEquals(1,engine.shutdowns)
    }
    @Test fun foregroundRetryRecoversAfterMissingEngine() {
        val failed=Engine(false);val recovered=Engine(true,listOf(uk));var calls=0
        val narrator=OfflineNarration {if(calls++==0) failed else recovered}
        narrator.start();assertFalse(narrator.available.value);narrator.background();narrator.start()
        assertTrue(narrator.available.value);assertEquals(1,failed.shutdowns)
    }
    @Test fun backgroundStopsAndReleasesEngineAndRejectsLateCallback() {
        val engine=Engine(null,listOf(uk));val narrator=OfflineNarration {engine};narrator.start();narrator.background()
        engine.callback!!(true);narrator.speak("must stay silent",1f)
        assertFalse(narrator.available.value);assertEquals(1,engine.stops);assertEquals(1,engine.shutdowns);assertTrue(engine.spoken.isEmpty())
    }
    @Test fun supersededCallbacksCannotChangeCurrentEngineReadiness() {
        val old=Engine(null,listOf(uk));val current=Engine(true,listOf(us));var calls=0
        val narrator=OfflineNarration {if(calls++==0) old else current}
        narrator.start();narrator.start();old.callback!!(false);old.callback!!(true)
        assertTrue(narrator.available.value);assertTrue(old.selections.isEmpty());assertEquals(0,current.shutdowns)
    }
    @Test fun closeIsFinalEvenWhenOldInitializationCompletes() {
        val engine=Engine(null,listOf(uk));var created=0
        val narrator=OfflineNarration {created++;engine};narrator.start();narrator.close();engine.callback!!(true);narrator.start()
        assertEquals(1,created);assertFalse(narrator.available.value);assertEquals(1,engine.shutdowns)
    }
    @Test fun mutedBlankAndNonFiniteRequestsDoNotReachEngine() {
        val engine=Engine(true,listOf(uk));val narrator=OfflineNarration {engine};narrator.start()
        for(volume in listOf(0f,-1f,Float.NaN,Float.POSITIVE_INFINITY)) narrator.speak("hello",volume)
        narrator.speak("  ",1f);narrator.speak("bounded",2f)
        assertEquals(listOf("bounded" to 1f),engine.spoken)
    }
    @Test fun rejectedOrThrowingSpeechDisablesAndReleasesEngine() {
        for(throwing in listOf(false,true)) {
            val engine=Engine(true,listOf(uk));engine.speakAccepted=false;engine.throwSpeak=throwing
            val narrator=OfflineNarration {engine};narrator.start();narrator.speak("hello",0.3f)
            assertFalse(narrator.available.value);assertEquals(1,engine.shutdowns)
        }
    }
    @Test fun ordinarySilenceKeepsAvailableEngineForNextQuestion() {
        val engine=Engine(true,listOf(uk));val narrator=OfflineNarration {engine};narrator.start();narrator.stop();narrator.speak("next",0.3f)
        assertTrue(narrator.available.value);assertEquals(0,engine.shutdowns);assertEquals(listOf("next" to 0.3f),engine.spoken)
    }
    @Test fun failingStopFallsBackToShutdown() {
        val engine=Engine(true,listOf(uk));val narrator=OfflineNarration {engine};narrator.start();engine.throwStop=true;narrator.stop()
        assertFalse(narrator.available.value);assertEquals(1,engine.shutdowns)
    }
    @Test fun constructionInitializationAndCleanupExceptionsStayUnavailable() {
        val unavailable=OfflineNarration {error("factory")};unavailable.start();assertFalse(unavailable.available.value)
        val engine=Engine(null,listOf(uk));engine.throwInit=true;engine.throwStop=true;engine.throwShutdown=true
        val narrator=OfflineNarration {engine};narrator.start();engine.callback!!(true)
        assertFalse(narrator.available.value);assertEquals(1,engine.shutdowns)
    }
    @Test fun asynchronousEngineErrorClearsReadiness() {
        val engine=Engine(true,listOf(uk));val narrator=OfflineNarration {engine};narrator.start();engine.callback!!(false)
        assertFalse(narrator.available.value);assertEquals(1,engine.shutdowns)
    }
}
