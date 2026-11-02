package com.skillsprout.arcade

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Real Compose screens with synthetic explorer data; no claims of physical Fire captures. */
abstract class LocalizedStoreScreens {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun captureLocalizedLearningScreens() {
        val base=ApplicationProvider.getApplicationContext<Context>()
        val content=ContentPacks(base).starter()
        val tags=AppLanguages.available(base)
        var tag by mutableStateOf("en-GB")
        var game by mutableStateOf(Game.MATHS)
        compose.setContent {
            val ctx=remember(tag) {base.createConfigurationContext(Configuration(base.resources.configuration).apply {
                val locale=Locale.forLanguageTag(tag)
                setLocales(LocaleList(locale,Locale.UK));setLayoutDirection(locale)
            })}
            CompositionLocalProvider(LocalContext provides ctx,LocalConfiguration provides ctx.resources.configuration,
                LocalLayoutDirection provides if(tag=="ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                key(tag,game) {MaterialTheme(colorScheme=androidx.compose.material3.lightColorScheme(primary=Leaf,onPrimary=androidx.compose.ui.graphics.Color.White,background=Cream,surface=Cream,onSurface=Forest)) {
                    Surface(Modifier.fillMaxSize()) {GameScreen(Child("store-fixture","Explorer",year=2),game,content,emptyList(),null,{},{},false,{},{},{},{})}
                }}
            }
        }
        val variant=if(base.resources.configuration.screenWidthDp>=800) "tablet" else "phone"
        val root=File("../marketing/candidate/captures/$variant").apply {mkdirs()}
        val records=JSONArray()
        for(language in tags) for(activity in listOf(Game.MATHS,Game.SPELLING,Game.STORY)) {
            compose.runOnIdle {tag=language;game=activity}
            compose.waitForIdle()
            val config=Configuration(base.resources.configuration).apply {setLocales(LocaleList(Locale.forLanguageTag(language),Locale.UK))}
            val expected=base.createConfigurationContext(config).getString(R.string.hint)
            // The controls must resolve to this interface locale; learning text remains English.
            compose.onAllNodesWithText(expected,substring=true).fetchSemanticsNodes().also {Assert.assertTrue("Missing localised hint for $language/$activity",it.isNotEmpty())}
            val textNodes=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes()
            Assert.assertTrue(textNodes.size>=5)
            compose.runOnIdle {
                textNodes.forEach {node->
                    val layouts=mutableListOf<TextLayoutResult>()
                    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                    // BasicText's semantics paragraph may retain the parent's maximum width
                    // although the view uses intrinsic width. Check actual line extents rather
                    // than that paragraph-width/size comparison (which flags even 'Maths Dash').
                    layouts.forEach {layout->
                        for(line in 0 until layout.lineCount) {
                            val width=layout.getLineRight(line)-layout.getLineLeft(line)
                            Assert.assertTrue("Text line exceeds view $language/$activity: ${layout.layoutInput.text}",width<=layout.size.width+1f)
                            Assert.assertFalse("Ellipsised text $language/$activity",layout.isLineEllipsized(line))
                        }
                        if(layout.lineCount>0) Assert.assertTrue("Text height exceeds view $language/$activity",layout.getLineBottom(layout.lineCount-1)<=layout.size.height+1f)
                    }
                }
            }
            lateinit var image:Bitmap
            compose.runOnIdle {
                val view=compose.activity.window.decorView
                image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(image))
            }
            val directory=File(root,language).apply {mkdirs()}
            val file=File(directory,activity.name.lowercase()+".png")
            file.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
            image.recycle()
            records.put(JSONObject().put("locale",language).put("game",activity.name).put("file",file.name)
                .put("source","Real Compose screen, synthetic explorer, JVM native Android renderer")
                .put("interfaceLocaleVerified",true).put("textLineBoundOverflowCount",0).put("learningContentLanguage","en-GB"))
        }
        File(root,"capture-records.json").writeText(records.toString(2))
    }
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="sw800dp-w1280dp-h800dp-land-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalizedTabletStoreScreensTest:LocalizedStoreScreens()
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="sw400dp-w400dp-h800dp-port-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalizedPhoneStoreScreensTest:LocalizedStoreScreens()
