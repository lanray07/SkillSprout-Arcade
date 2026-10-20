package com.skillsprout.arcade

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

private class MemoryDao:ArcadeDao {
    val kids=MutableStateFlow(listOf(Child("preview","Explorer",year=2)))
    val results=MutableStateFlow(emptyList<Attempt>())
    val tasks=MutableStateFlow(listOf(Assignment("mission","preview","MATHS","addition",2,3,1,"2026-10-23")))
    override fun children()=kids;override fun attempts()=results;override fun assignments()=tasks
    override suspend fun save(child:Child) {kids.value=kids.value.filterNot {it.id==child.id}+child}
    override suspend fun save(assignment:Assignment) {tasks.value=tasks.value.filterNot {it.id==assignment.id}+assignment}
    override suspend fun save(attempt:Attempt) {results.value=results.value+attempt}
    override suspend fun deleteAttempts(id:String) {results.value=results.value.filterNot {it.childId==id}}
    override suspend fun deleteAssignments(id:String) {tasks.value=tasks.value.filterNot {it.childId==id}}
    override suspend fun deleteChild(id:String) {kids.value=kids.value.filterNot {it.id==id}}
}
private class TestVault:PinVault {
    override val configured=true
    override fun secondsLocked()=0L
    override fun setPin(pin:String) {error("not used")}
    override fun verify(pin:String)=pin=="654321"
}

abstract class PlayableScreensTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun screenshot(name:String) {
        compose.waitForIdle()
        lateinit var image:Bitmap
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
        }
        val variant=if(context.resources.configuration.screenWidthDp>=800) "tablet" else "phone"
        val folder=File("../artifacts/screenshots/$variant").apply {mkdirs()}
        File(folder,"$name.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun home():MemoryDao {
        val dao=MemoryDao()
        compose.setContent {ArcadeApp(dao,{},{},{},TestVault(),accountPlayTime=false)}
        compose.waitUntil(15_000) {compose.onAllNodesWithText("Ready for a little adventure?").fetchSemanticsNodes().isNotEmpty()}
        return dao
    }
    private fun game(g:Game):Activity {
        val content=ContentPacks(context).starter()
        val a=LearningEngine.select(content.filter {it.game==g},2,emptyMap(),emptyList())!!
        compose.setContent {MaterialTheme(colorScheme=lightColorScheme(primary=Leaf,onPrimary=androidx.compose.ui.graphics.Color.White,background=Cream,surface=Cream,onSurface=Forest)) {Surface {GameScreen(Child("game","Explorer",year=2),g,content,emptyList(),null,{},{},false,{},{},{},{})}}}
        compose.waitUntil(10_000) {compose.onAllNodesWithText(a.prompt).fetchSemanticsNodes().isNotEmpty()}
        return a
    }
    @Test fun screenshot01Arcade() {home();screenshot("01-arcade")}
    @Test fun screenshot02AssignedMissions() {home();if(context.resources.configuration.screenWidthDp>=840) compose.onNodeWithText("⚑  Missions").performClick() else compose.onNodeWithText("Missions",useUnmergedTree=true).performScrollTo().performClick();compose.onNodeWithText("Start mission").assertExists();screenshot("02-homework")}
    @Test fun screenshot03TimesTablesAndGrouping() {game(Game.TABLES);compose.onNodeWithText("Add a group").performScrollTo().performClick();compose.onNodeWithText("Add a group").performClick();screenshot("03-times-tables")}
    @Test fun screenshot04SpellingAcceptsTileAssembly() {
        val a=game(Game.SPELLING);screenshot("04-spelling")
        val used=mutableMapOf<Char,Int>()
        a.answer.forEach {ch->val index=used.getOrDefault(ch,0);compose.onAllNodes(hasText(ch.toString(),substring=false) and hasClickAction())[index].performScrollTo().performClick();used[ch]=index+1}
        compose.onNodeWithText("Check my answer").performScrollTo().performClick();compose.onNodeWithText("Next challenge").assertExists()
    }
    @Test fun screenshot05Story() {game(Game.STORY);screenshot("05-reading")}
    @Test fun screenshot06AllGamesAndMathsCompletion() {
        val a=game(Game.MATHS);compose.onNodeWithText(a.answer,substring=false).performScrollTo().performClick();compose.onNodeWithText("Check my answer").performScrollTo().performClick();compose.onNodeWithText("Next challenge").assertExists();screenshot("06-maths")
    }
    @Test fun screenshot07ParentAreaRequiresPin() {
        home();compose.onNodeWithText("Parent area").performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Grown-ups only").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("A little practice. A clearer picture.").assertDoesNotExist()
        compose.onNodeWithText("Parent PIN").performTextInput("000000");compose.onNodeWithText("Unlock").performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("That PIN did not match. Try again.").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Parent PIN").performTextReplacement("654321");compose.onNodeWithText("Unlock").performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("A little practice. A clearer picture.").fetchSemanticsNodes().isNotEmpty()};screenshot("07-parent-dashboard")
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.onNodeWithText("A little practice. A clearer picture.").assertDoesNotExist()
        compose.onNodeWithText("Parent area").assertExists()
    }
    @Test fun screenshot08OfflinePacks() {
        compose.setContent {MaterialTheme(colorScheme=lightColorScheme(primary=Leaf,surface=Cream,onSurface=Forest)) {Surface {ParentDashboard(emptyList(),null,emptyList(),emptyList(),ContentPacks(context).starter(),PlayClock(context),ContentPacks(context),{},{},{},{},{},{},{})}}}
        compose.onNodeWithText("Packs",substring=false).performScrollTo().performClick();compose.onNodeWithText("Ready for offline adventures").assertExists();screenshot("08-offline-packs")
    }
    @Test fun logicUsesOrderedTiles() {val a=game(Game.LOGIC);a.answer.split(' ').forEach {compose.onNodeWithText(it,substring=false).performScrollTo().performClick()};compose.onNodeWithText("Check my answer").performScrollTo().performClick();compose.onNodeWithText("Next challenge").assertExists()}
    @Test fun wordBuilderAcceptsMeaningfulWord() {val a=game(Game.WORDS);compose.onNodeWithText(a.answer,substring=false).performScrollTo().performClick();compose.onNodeWithText("Check my answer").performScrollTo().performClick();compose.onNodeWithText("Next challenge").assertExists()}
    @Test fun completedMissionRecordsEachChallengeOnce() {
        val content=ContentPacks(context).starter();val records=mutableStateListOf<Attempt>();var completions=0
        val task=Assignment("test-assignment","complete","MATHS","addition",2,3,1,"2026-10-23")
        compose.setContent {MaterialTheme {Surface {GameScreen(Child("complete","Explorer",year=2),Game.MATHS,content,records,task,{},{},false,{},{},{records.add(it)},{completions++})}}}
        val seen=mutableListOf<String>();val pool=content.filter {it.game==Game.MATHS && it.skill=="addition"}
        repeat(3) {i->
            val a=LearningEngine.select(pool,2,emptyMap(),seen,1)!!
            compose.waitUntil(10_000) {compose.onAllNodesWithText(a.prompt).fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText(a.answer,substring=false).performScrollTo().performClick()
            compose.onNodeWithText("Check my answer").performScrollTo().performClick()
            compose.onNodeWithText(if(i==2) "Finish mission" else "Next challenge").performScrollTo().performClick();seen.add(a.id)
        }
        compose.onNodeWithText("Mission complete!").assertExists();assertEquals(3,records.size);assertEquals(3,records.map {it.id}.distinct().size);assertEquals(1,completions)
    }
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="sw800dp-w1280dp-h800dp-land-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletScreensTest:PlayableScreensTest()
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="sw400dp-w400dp-h800dp-port-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneScreensTest:PlayableScreensTest()
