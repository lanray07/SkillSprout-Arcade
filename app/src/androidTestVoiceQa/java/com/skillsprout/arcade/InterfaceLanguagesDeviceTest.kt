package com.skillsprout.arcade

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Fire resource selection only; does not open profiles, PIN storage or change device language. */
@RunWith(AndroidJUnit4::class)
class InterfaceLanguagesDeviceTest {
    @Test fun compiledCataloguesPluralsAndEnglishDisclosureOnFire() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(BuildConfig.DEBUG && context.packageName=="com.skillsprout.arcade.voiceqa" && !BuildConfig.PURCHASING_ALLOWED)
        val original=context.resources.configuration.locales.toLanguageTags()
        val tags=AppLanguages.available(context)
        assertEquals(19,tags.size)
        val english=context.createConfigurationContext(Configuration(context.resources.configuration).apply {setLocales(LocaleList(Locale.UK))})
        val labels=mutableSetOf<String>()
        for(tag in tags) {
            val locale=Locale.forLanguageTag(tag)
            val translated=context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocales(LocaleList(locale,Locale.UK));setLayoutDirection(locale)
            })
            val label=translated.getString(R.string.parent_area)
            assertTrue("Parent label must exist for $tag",label.isNotBlank())
            if(tag !in setOf("en-GB","en-US")) assertNotEquals(tag,english.getString(R.string.parent_area),label)
            labels.add(label)
            assertTrue(translated.getString(R.string.privacy_language_note).isNotBlank())
            assertEquals("English learning narration must remain English for $tag",
                english.getString(R.string.voice_preview),AppLanguages.englishContext(translated).getString(R.string.voice_preview))
            assertEquals(if(tag=="ar") android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR,
                translated.resources.configuration.layoutDirection)
            if(tag=="ar") {
                assertTrue(translated.resources.getQuantityString(R.plurals.stars,0,0).contains("لا نجوم"))
                assertTrue(translated.resources.getQuantityString(R.plurals.stars,2,2).contains("نجمتان"))
            }
            if(tag=="pl") {
                assertTrue(translated.resources.getQuantityString(R.plurals.stars,2,2).contains("gwiazdki"))
                assertTrue(translated.resources.getQuantityString(R.plurals.stars,5,5).contains("gwiazdek"))
            }
        }
        assertTrue(labels.size>=17)
        assertEquals(original,context.resources.configuration.locales.toLanguageTags())
        val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(folder,"interface-languages.json").writeText(JSONObject()
            .put("model",android.os.Build.MODEL).put("api",android.os.Build.VERSION.SDK_INT)
            .put("localeVariants",tags.size).put("compiledResourcesSelected",true)
            .put("arabicRtlAndPolishArabicPlurals",true).put("englishNarrationResourcesPreserved",true)
            .put("profilesOrPinOpened",false).put("deviceLanguageChanged",false)
            .put("audibleVoiceViewTested",false).put("nativeLanguageReview",false)
            .put("productionApproval",false).toString(2))
    }
}
