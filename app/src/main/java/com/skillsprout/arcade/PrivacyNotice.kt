package com.skillsprout.arcade

import android.content.Context
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal fun privacyParagraphs(context:Context,amazon:Boolean):List<String> {
    val value=context.assets.open("privacy/notice.json").use {it.readBytesBounded(65_536)}
    val document=JSONObject(value.toString(Charsets.UTF_8))
    require(document.getInt("schemaVersion")==1 && document.getString("locale")=="en-GB")
    return listOf("common",if(amazon) "amazon" else "offline","rights").flatMap {section->
        val paragraphs=document.getJSONArray(section)
        require(paragraphs.length() in 1..30)
        (0 until paragraphs.length()).map {paragraphs.getString(it).also {text->require(text.isNotBlank())}}
    }
}

/** Bundled disclosure remains available offline inside the parent gate. */
@Composable internal fun ParentPrivacyNotice() {
    val context=LocalContext.current
    var paragraphs by remember {mutableStateOf<List<String>?>(null)}
    var failed by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {
        try {paragraphs=withContext(Dispatchers.IO) {privacyParagraphs(context,BuildConfig.PURCHASING_ALLOWED)}}
        catch(cancelled:CancellationException) {throw cancelled}
        catch(_:Exception) {failed=true}
    }
    Heading(stringResource(R.string.privacy_title),true)
    Text(stringResource(R.string.privacy_language_note))
    if(BuildConfig.PURCHASING_ALLOWED) Text(stringResource(R.string.purchase_privacy))
    if(failed) Text(stringResource(R.string.privacy_load_failed))
    else if(paragraphs==null) CircularProgressIndicator()
    else paragraphs!!.forEach {Text(it)}
}
