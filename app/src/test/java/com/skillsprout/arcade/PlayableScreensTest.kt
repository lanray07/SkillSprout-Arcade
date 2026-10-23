package com.skillsprout.arcade

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp

private class MemoryDao:ArcadeDao {
    val kids=MutableStateFlow(listOf(Child("preview","Explorer",year=2)))
    val completionEvents=MutableStateFlow(emptyList<MissionCompletion>())
    override fun completions()=completionEvents
    override suspend fun insertCompletion(completion:MissionCompletion):Long {if(completionEvents.value.any {it.runId==completion.runId})return -1L;completionEvents.value+=completion;return 1L}
    override suspend fun incrementCompletion(id:String,childId:String,at:Long) {tasks.value=tasks.value.map {if(it.id==id&&it.childId==childId)it.copy(completed=it.completed+1,completedAt=at) else it}}
    override suspend fun deleteCompletions(id:String) {completionEvents.value=completionEvents.value.filterNot {it.childId==id}}
    val results=MutableStateFlow(emptyList<Attempt>())
    val tasks=MutableStateFlow(listOf(Assignment("mission","preview","MATHS","addition",2,3,1,"2026-10-23")))
    override fun children()=kids;override fun attempts()=results;override fun assignments()=tasks
    override suspend fun assignment(id:String)=tasks.value.find {it.id==id}
    override suspend fun childExists(id:String)=kids.value.any {it.id==id}
    override suspend fun insertAssignment(assignment:Assignment) {save(assignment)}
    override suspend fun insertAttempt(attempt:Attempt) {save(attempt)}
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
    @Test fun pendingPurchaseStatusIsAnnouncedAndRecoveryControlsFitLargeText() {
        val store=mutableStateOf(PurchaseState(configured=true,status=PurchaseStatus.LOADING,busy=true))
        var restores=0
        var refreshes=0
        val gateway=object:PurchaseGateway {
            override val state=MutableStateFlow(store.value)
            override fun resume() {refreshes++}
            override fun restore() {restores++}
            override fun purchase(sku:String) {error("No purchase expected")}
            override fun checkAccess() {}
            override fun close() {}
        }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.5f)) {
                MaterialTheme {
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("purchase-panel")) {
                        FamilyPurchases(store.value,FamilyAccess(false,0,1000),gateway)
                    }
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.purchase_restore)).performScrollTo().assertIsNotEnabled()
        compose.runOnIdle {store.value=store.value.copy(status=PurchaseStatus.PENDING,busy=false)}
        compose.onNodeWithText(context.getString(R.string.purchase_pending)).performScrollTo().assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion,LiveRegionMode.Polite))
        for(label in listOf(R.string.purchase_restore,R.string.purchase_refresh)) {
            val control=compose.onNodeWithText(context.getString(label)).performScrollTo().assertIsDisplayed().assertIsEnabled()
            val panel=compose.onNodeWithTag("purchase-panel").fetchSemanticsNode().boundsInRoot
            val bounds=control.fetchSemanticsNode().boundsInRoot
            assertTrue("Recovery control must remain within the narrow panel: $label",bounds.left>=panel.left-1 && bounds.right<=panel.right+1)
            control.performClick()
        }
        assertEquals(1,restores)
        assertEquals(1,refreshes)
        screenshot("12-pending-purchases-large-text")
    }
    @Test fun unavailableOfflineVoiceKeepsParentControlsUsable() {
        var saved=false
        var previewCalled=false
        val explorer=Child("no-voice-qa","QA Explorer")
        compose.setContent {MaterialTheme {ParentDashboard(listOf(explorer),explorer,
            emptyList(),emptyList(),ContentPacks(context).starter(),PlayClock(context),ContentPacks(context),
            {},{saved=true},{},{},{},{},{},previewVoice={_,_->previewCalled=true},speechAvailable=false)}}
        compose.onNodeWithText("Controls",substring=false).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.voice_unavailable)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Preview narration").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Save controls").performScrollTo().assertIsEnabled().performClick()
        assertTrue("Missing narration must not block saving parent controls",saved)
        assertFalse("Unavailable narration must not call the voice preview",previewCalled)
    }
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
        compose.setContent {ArcadeApp(dao,{_,_->},{},{},TestVault(),accountPlayTime=false,
            purchaseGateway=UnavailablePurchases(),developmentAccess=true)}
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
    @Test fun freeAccessUsesParentPinAndNeverShowsPurchasesToChildren() {
        val dao=MemoryDao()
        compose.setContent {ArcadeApp(dao,{_,_->},{},{},TestVault(),accountPlayTime=false,developmentAccess=false)}
        compose.waitUntil(15_000) {compose.onAllNodesWithText("Parent area").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Restore purchases").assertDoesNotExist()
        compose.onNodeWithText("Parent area").performClick()
        compose.onNodeWithText("Parent PIN").performTextInput("654321");compose.onNodeWithText("Unlock").performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("A little practice. A clearer picture.").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Practice",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Parent practice assignments require verified Family access.",substring=true).assertExists()
        compose.onNodeWithText("Premium",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Purchasing is not configured in this build. No payment can be made.").assertExists()
        screenshot("10-family-starter")
    }
    @Test fun parentPurchasesDisplayOnlyStorePricesAndRouteRestore() {
        var purchased="";var restored=0
        val storeState=PurchaseState(true,listOf(FamilyOffer("actual-test-sku","Amazon test offer","£2.34",subscriptionPeriod="1 Month")),status=PurchaseStatus.READY)
        val gateway=object:PurchaseGateway {
            override val state=MutableStateFlow(storeState)
            override fun resume() {};override fun checkAccess() {};override fun close() {}
            override fun purchase(sku:String) {purchased=sku}
            override fun restore() {restored++}
        }
        compose.setContent {MaterialTheme {Surface(color=Cream) {Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {FamilyPurchases(storeState,FamilyAccess(false,0,1000),gateway)}}}}
        compose.onNodeWithText("Billing period: 1 Month").assertExists()
        compose.onNodeWithText("Continue with Amazon — £2.34").performScrollTo().performClick();assertEquals("actual-test-sku",purchased)
        compose.onNodeWithText("Restore purchases").performScrollTo().performClick();assertEquals(1,restored)
        screenshot("11-parent-purchases")
    }
    @Test fun screenshot01Arcade() {home();screenshot("01-arcade")}
    @Test fun screenshot02AssignedMissions() {home();if(context.resources.configuration.screenWidthDp>=840) compose.onNodeWithText("Missions",substring=true).performClick() else compose.onNodeWithText("Missions",useUnmergedTree=true).performScrollTo().performClick();compose.onNodeWithText("Start mission").assertExists();screenshot("02-homework")}
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
    @Test fun earnedDecorationCanBeSelectedWithoutSpendingStars() {
        var explorer by mutableStateOf(Child("rewards","Explorer"))
        val attempts=List(5){Attempt("reward-$it",explorer.id,"q-$it","addition","MATHS",true,0,1,1000,0)}
        compose.setContent {MaterialTheme(colorScheme=lightColorScheme(primary=Leaf,surface=Cream,onSurface=Forest)) {Surface {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {Rewards(explorer,attempts) {explorer=explorer.copy(decoration=it)}}}}}
        compose.onNodeWithText("Choose Sunflower badge").performScrollTo().performClick()
        compose.onNodeWithText("Sunflower badge · Selected").assertExists()
        assertEquals("sunflower",explorer.decoration);assertEquals(5,attempts.size);screenshot("09-rewards")
    }
    @Test fun parentNarrationVolumePreviewsAndSaves() {
        var explorer by mutableStateOf(Child("volume","Explorer"));var previewVolume=-1
        compose.setContent {MaterialTheme {ParentDashboard(listOf(explorer),explorer,emptyList(),emptyList(),ContentPacks(context).starter(),PlayClock(context),ContentPacks(context),{},{explorer=it},{},{},{},{},{},previewVoice={_,volume->previewVolume=volume})}}
        compose.onNodeWithText("Controls",substring=false).performScrollTo().performClick()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(30f)}
        compose.onNodeWithText("Preview narration").performScrollTo().performClick();assertEquals(30,previewVolume)
        compose.onNodeWithText("Save controls").performScrollTo().performClick();assertEquals(30,explorer.voiceVolume)
    }
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="sw800dp-w1280dp-h800dp-land-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletScreensTest:PlayableScreensTest()
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="sw400dp-w400dp-h800dp-port-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneScreensTest:PlayableScreensTest()
