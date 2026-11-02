package com.skillsprout.arcade

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/** Interface language only. Learning packs and their English narration keep their own language. */
internal object AppLanguages {
    const val SYSTEM="system"
    private const val FILE="interface_language"
    private const val KEY="selection"
    fun available(context:Context):List<String> = context.resources.getStringArray(R.array.interface_locales)
        .filter {it.matches(Regex("[a-z]{2}(?:-[A-Za-z]{2,4})?"))}.distinct().let {listOf("en-GB")+it.filterNot {tag->tag=="en-GB"}}
    fun selection(context:Context):String = context.getSharedPreferences(FILE,Context.MODE_PRIVATE)
        .getString(KEY,SYSTEM).let {if(it==SYSTEM || it in available(context)) it!! else SYSTEM}
    fun save(context:Context,value:String):Boolean {
        if(value!=SYSTEM && value !in available(context)) return false
        return context.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().putString(KEY,value).commit()
    }
    fun resolve(selection:String,device:List<Locale>,available:List<String>):Locale {
        val supported=(listOf("en-GB")+available).distinct()
        if(selection!=SYSTEM && selection in supported) return Locale.forLanguageTag(selection)
        for(locale in device) {
            val tag=locale.toLanguageTag()
            if(tag in supported) return Locale.forLanguageTag(tag)
            if(locale.language=="zh") {
                val script=locale.script.ifEmpty {if(locale.country in setOf("TW","HK","MO")) "Hant" else "Hans"}
                supported.firstOrNull {it=="zh-$script"}?.let {return Locale.forLanguageTag(it)}
                continue
            }
            supported.firstOrNull {Locale.forLanguageTag(it).language==locale.language}?.let {return Locale.forLanguageTag(it)}
        }
        return Locale.UK
    }
    fun wrap(base:Context):Context {
        val config=Configuration(base.resources.configuration)
        val locale=resolve(selection(base),(0 until config.locales.size()).map {config.locales[it]},available(base))
        config.setLocales(if(locale==Locale.UK) LocaleList(locale) else LocaleList(locale,Locale.UK))
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
    fun nativeLabel(tag:String):String = Locale.forLanguageTag(tag).let {it.getDisplayName(it)}
    fun englishContext(base:Context):Context = base.createConfigurationContext(Configuration(base.resources.configuration).apply {
        setLocales(LocaleList(Locale.UK));setLayoutDirection(Locale.UK)
    })
}
