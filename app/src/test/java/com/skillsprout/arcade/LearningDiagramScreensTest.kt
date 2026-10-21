package com.skillsprout.arcade

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

abstract class LearningDiagramScreensTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun clearSession() {context.getSharedPreferences("sessions",0).edit().clear().commit()}
    private fun show(id:String):Activity {
        val item=ContentPacks(context).starter().single {it.id==id}
        compose.setContent {MaterialTheme(colorScheme=lightColorScheme(primary=Leaf,onPrimary=androidx.compose.ui.graphics.Color.White,background=Cream,surface=Cream,onSurface=Forest)) {
            Surface {GameScreen(Child("diagram-qa","Explorer",year=1),Game.MATHS,listOf(item),emptyList(),null,{},{},false,{},{},{},{})}
        }}
        compose.waitUntil(10000) {compose.onAllNodesWithText(item.prompt).fetchSemanticsNodes().isNotEmpty()}
        return item
    }
    private fun capture(name:String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val variant=if(context.resources.configuration.screenWidthDp>=800) "tablet" else "phone"
            val file=File("../artifacts/verification/learning-diagrams/$variant-$name.png");file.parentFile.mkdirs()
            file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
    @Test fun shapeLabelsStayAttachedAndCorrectAnswerStillWorks() {
        show("dev-maths-y1-0397")
        for(label in listOf("Triangle","Circle","Square")) compose.onNodeWithTag("diagram-choice-$label").performScrollTo().assertTextContains(label)
        compose.onNodeWithTag("diagram-choice-Triangle").performScrollTo();capture("shapes")
        compose.onNodeWithTag("diagram-choice-Square").performScrollTo().performClick()
        compose.onNodeWithText("Check my answer").performScrollTo().performClick()
        compose.onNodeWithTag("diagram-choice-Triangle").performScrollTo().performClick()
        compose.onNodeWithText("Check my answer").performScrollTo().performClick()
        compose.onNodeWithText("A triangle has three straight sides and three corners.",substring=true).assertExists()
        compose.onNodeWithText("Next challenge").assertExists()
    }
    @Test fun clockShowsTheQuestionTimeWithAnAccessibleDescription() {
        show("dev-maths-y1-0399")
        compose.onNodeWithTag("learning-clock").performScrollTo().assertContentDescriptionEquals("Analogue clock showing 3:00.")
        capture("clock")
        compose.onNodeWithText("4 o’clock",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Check my answer").performScrollTo().performClick()
        compose.onNodeWithText("Next challenge").assertExists()
    }
    @Test fun equalPiecesAreAvailableWithoutDivisionFactMastery() {
        val item=show("dev-maths-y1-0401")
        Assert.assertEquals(listOf("counting"),item.prerequisites)
        compose.onNodeWithTag("learning-sandwich").performScrollTo().assertContentDescriptionEquals("A square sandwich cut diagonally into two equal pieces.")
        capture("sandwich")
        compose.onNodeWithText("half",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Check my answer").performScrollTo().performClick()
        compose.onNodeWithText("Next challenge").assertExists()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="sw800dp-w1280dp-h800dp-land-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletLearningDiagramScreensTest:LearningDiagramScreensTest()
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="sw400dp-w400dp-h800dp-port-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneLearningDiagramScreensTest:LearningDiagramScreensTest()
