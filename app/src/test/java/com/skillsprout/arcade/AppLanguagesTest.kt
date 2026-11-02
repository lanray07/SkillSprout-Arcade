package com.skillsprout.arcade

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class AppLanguagesTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun explicitChoiceWinsAndUnsupportedChoiceFallsBackToDevice() {
        val supported=listOf("fr","de","en-US")
        assertEquals("de",AppLanguages.resolve("de",listOf(Locale.FRENCH),supported).toLanguageTag())
        assertEquals("fr",AppLanguages.resolve("xx",listOf(Locale.FRENCH),supported).toLanguageTag())
    }
    @Test fun deviceOrderAndEnglishFallbackAreDeterministic() {
        assertEquals("de",AppLanguages.resolve("system",listOf(Locale.forLanguageTag("cy"),Locale.GERMAN),listOf("de")).toLanguageTag())
        assertEquals(Locale.UK,AppLanguages.resolve("system",listOf(Locale.forLanguageTag("cy")),listOf("de")))
        assertEquals("en-US",AppLanguages.resolve("system",listOf(Locale.US),listOf("en-US")).toLanguageTag())
    }
    @Test fun chineseScriptAndRegionChooseTheCorrectResourceVariant() {
        val supported=listOf("zh-Hans","zh-Hant")
        for(tag in listOf("zh-TW","zh-HK","zh-Hant")) assertEquals("zh-Hant",AppLanguages.resolve("system",listOf(Locale.forLanguageTag(tag)),supported).toLanguageTag())
        assertEquals("zh-Hans",AppLanguages.resolve("system",listOf(Locale.CHINA),supported).toLanguageTag())
    }
    @Test fun saveRejectsUnknownTagsWithoutOverwritingTheCurrentChoice() {
        assertTrue(AppLanguages.save(context,"en-GB"))
        assertFalse(AppLanguages.save(context,"../../unknown"))
        assertEquals("en-GB",AppLanguages.selection(context))
        assertTrue(AppLanguages.save(context,"system"));assertEquals("system",AppLanguages.selection(context))
    }
    @Test fun corruptStoredSelectionUsesSystemAndOtherPreferencesSurvive() {
        context.getSharedPreferences("interface_language",0).edit().putString("selection","not-a-language").commit()
        assertEquals("system",AppLanguages.selection(context))
        val unrelated=context.getSharedPreferences("unrelated-test-preferences",0)
        unrelated.edit().putString("fixture","keep").commit()
        assertTrue(AppLanguages.save(context,"en-GB"));assertEquals("keep",unrelated.getString("fixture",null))
    }
    @Test fun wrappedResourcesUseFallbackWithoutMutatingTheBaseConfiguration() {
        val config=Configuration(context.resources.configuration).apply {setLocales(LocaleList(Locale.forLanguageTag("cy-GB")))}
        val base=context.createConfigurationContext(config)
        val wrapped=AppLanguages.wrap(base)
        assertEquals("en-GB",wrapped.resources.configuration.locales[0].toLanguageTag())
        assertEquals("cy-GB",base.resources.configuration.locales[0].toLanguageTag())
    }
    @Test fun compiledResourcesSelectRegionalAndScriptVariants() {
        val samples=mapOf("fr" to "Espace parents","pt-BR" to "Área dos responsáveis","pt-PT" to "Área dos pais",
            "ar" to "منطقة الوالدين","hi" to "अभिभावक क्षेत्र","ja" to "保護者エリア","ko" to "보호자 영역",
            "zh-Hans" to "家长专区","zh-Hant" to "家長專區","id" to "Area orang tua","sv" to "Föräldraområde")
        for((tag,expected) in samples) {
            assertTrue(AppLanguages.save(context,tag))
            val wrapped=AppLanguages.wrap(context)
            assertEquals(tag,wrapped.resources.configuration.locales[0].toLanguageTag())
            assertEquals(tag,expected,wrapped.getString(R.string.parent_area))
        }
    }
    @Test fun arabicLayoutDoesNotChangeEnglishLearningNarration() {
        assertTrue(AppLanguages.save(context,"ar"));val arabic=AppLanguages.wrap(context)
        assertEquals(android.view.View.LAYOUT_DIRECTION_RTL,arabic.resources.configuration.layoutDirection)
        val speech=AppLanguages.englishContext(arabic)
        assertEquals(Locale.UK,speech.resources.configuration.locales[0])
        assertEquals(context.getString(R.string.voice_preview),speech.getString(R.string.voice_preview))
        assertEquals("ar",arabic.resources.configuration.locales[0].toLanguageTag())
    }
    @Test fun compiledArabicAndPolishPluralsUseMoreThanOneAndOther() {
        fun resources(tag:String)=context.createConfigurationContext(Configuration(context.resources.configuration).apply {setLocale(Locale.forLanguageTag(tag))}).resources
        val ar=resources("ar");assertTrue(ar.getQuantityString(R.plurals.stars,0,0).contains("لا نجوم"))
        assertTrue(ar.getQuantityString(R.plurals.stars,2,2).contains("نجمتان"))
        val pl=resources("pl");assertTrue(pl.getQuantityString(R.plurals.stars,2,2).contains("gwiazdki"))
        assertTrue(pl.getQuantityString(R.plurals.stars,5,5).contains("gwiazdek"))
    }
}
