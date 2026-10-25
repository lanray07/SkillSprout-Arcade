package com.skillsprout.arcade

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import java.util.*

val Forest=Color(0xFF173F32)
val Leaf=Color(0xFF427C39)
val Cream=Color(0xFFFFF9EE)
val Peach=Color(0xFFF4C567)
val GameColors=listOf(0xFFD9EABD,0xFFFFD786,0xFFBEE4F1,0xFFF9C6B6,0xFFE0CEF9,0xFFFFE5A8).map { Color(it) }

class MainActivity: ComponentActivity() {
    private lateinit var db:ArcadeDatabase
    private lateinit var narration:OfflineNarration
    private val packFiles=PackFileSelection()
    private val importFile=registerForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        lifecycleScope.launch {packFiles.complete(if(uri==null) null else ({contentResolver.openInputStream(uri)}))}
    }
    private val progressExport=ProgressReportExport()
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        lifecycleScope.launch {
            progressExport.complete(if(uri==null) null else ({contentResolver.openOutputStream(uri,"wt")}))
        }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        db=ArcadeDatabase.open(this)
        val speechContext=applicationContext
        narration=OfflineNarration {AndroidNarrationEngine(speechContext)}
        setContent {
            val speechAvailable by narration.available.collectAsState()
            val exportStatus by progressExport.status.collectAsState()
            val packSelection by packFiles.selection.collectAsState()
            ArcadeApp(db.dao(),speak=narration::speak,speechAvailable=speechAvailable,
                silence=narration::stop,export={csv->progressExport.request(csv) {export.launch("skillsprout-progress.csv")}},
                progressExportStatus=exportStatus,packSelection=packSelection,
                choosePack={packFiles.request {importFile.launch(arrayOf("application/json"))}},clearPack=packFiles::clear)
        }
    }
    override fun onStart() {super.onStart();narration.start()}
    override fun onStop() {narration.background();super.onStop()}
    override fun onDestroy() {packFiles.close();progressExport.close();narration.close();db.close();super.onDestroy()}

}

@Composable fun Action(label:String, modifier:Modifier=Modifier, enabled:Boolean=true, onClick:()->Unit) {
    Button(onClick,modifier.heightIn(min=52.dp),enabled=enabled,shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Leaf)) { Text(label,fontSize=16.sp,fontWeight=FontWeight.Bold) }
}
@Composable fun SoftButton(label:String, modifier:Modifier=Modifier, onClick:()->Unit) {
    OutlinedButton(onClick,modifier.heightIn(min=48.dp),shape=RoundedCornerShape(16.dp)) { Text(label,color=Forest) }
}
@Composable fun Heading(text:String,small:Boolean=false) { Text(text,fontSize=if(small) 23.sp else 32.sp,fontWeight=FontWeight.ExtraBold,color=Forest,lineHeight=if(small) 29.sp else 38.sp) }
@Composable fun Pip(modifier:Modifier=Modifier) { Image(painterResource(R.drawable.pip),stringResource(R.string.pip_description),modifier,contentScale=ContentScale.Fit) }
@Composable fun GameIcon(game:Game,modifier:Modifier=Modifier) {
    val id=when(game) {Game.MATHS->R.drawable.game_maths;Game.SPELLING->R.drawable.game_spelling;Game.STORY->R.drawable.game_story;Game.TABLES->R.drawable.game_tables;Game.LOGIC->R.drawable.game_logic;Game.WORDS->R.drawable.game_words}
    Image(painterResource(id),null,modifier)
}

@Composable fun ArcadeApp(dao:ArcadeDao,speak:(String,Float)->Unit,silence:()->Unit,export:(String)->Unit,pinVault:PinVault?=null,accountPlayTime:Boolean=true,speechAvailable:Boolean=true,purchaseGateway:PurchaseGateway?=null,developmentAccess:Boolean=BuildConfig.DEVELOPMENT_ACCESS,progressExportStatus:ProgressExportStatus=ProgressExportStatus.IDLE,playClock:PlayClock?=null,packSelection:PackSelection=PackSelection(),choosePack:()->Unit={},clearPack:()->Unit={}) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val children by dao.children().collectAsState(initial=emptyList())
    val attempts by dao.attempts().collectAsState(initial=emptyList())
    val completions by dao.completions().collectAsState(initial=emptyList())
    val assignments by dao.assignments().collectAsState(initial=emptyList())
    val security=remember { pinVault ?: ParentSecurity(context) }
    val clock=remember { playClock ?: PlayClock(context) }
    val packs=remember { ContentPacks(context) }
    var content by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var bundledIds by remember {mutableStateOf(emptySet<String>())}
    var contentError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {runCatching {withContext(Dispatchers.IO) {val base=packs.starter();base to (base+packs.installed())}}.onSuccess {(base,all)->bundledIds=base.map {it.id}.toSet();content=all}.onFailure {contentError=it.message}}
    val prefs=remember { context.getSharedPreferences("arcade_ui",0) }
    var selectedId by rememberSaveable { mutableStateOf(prefs.getString("child",null)) }
    val chosenChild=children.find { it.id==selectedId } ?: children.firstOrNull()
    var screen by rememberSaveable { mutableStateOf("home") }
    var parentOpen by remember { mutableStateOf(false) }
    var gate by remember { mutableStateOf(!security.configured) }
    var gateDestination by remember { mutableStateOf("parent") }
    var setup by remember { mutableStateOf(!security.configured) }
    var activeGame by rememberSaveable { mutableStateOf<String?>(null) }
    var gameChildId by rememberSaveable {mutableStateOf<String?>(null)}
    var activeAssignment by rememberSaveable { mutableStateOf<String?>(null) }
    var paused by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(true) }
    var gateSession by remember {mutableLongStateOf(0)}
    val profileWrites=remember {Mutex()}
    val parentSession=rememberUpdatedState(parentOpen && foreground && !gate)
    val purchases=remember {purchaseGateway ?: createPurchases(context) {parentSession.value}}
    val purchaseState by purchases.state.collectAsState()
    var accessNow by remember {mutableLongStateOf(System.currentTimeMillis())}
    var primaryId by remember {mutableStateOf(prefs.getString("primaryExplorer",null))}
    LaunchedEffect(children) {if(children.isNotEmpty() && children.none {it.id==primaryId}) {primaryId=children.first().id;prefs.edit().putString("primaryExplorer",primaryId).commit()}}
    val access=FamilyAccess(developmentAccess,purchaseState.verifiedUntil,accessNow,primaryId)
    val accessibleChildren=children.filter {access.canUseProfile(it.id,children)}
    val child=chosenChild?.takeIf {access.canUseProfile(it.id,children)} ?: accessibleChildren.firstOrNull()
    var breakDue by remember(child?.id,child?.breakMinutes,accountPlayTime) {
        mutableStateOf(accountPlayTime && child?.let {clock.breakDue(it)}==true)
    }
    var blocked by remember(child,accountPlayTime) {
        mutableStateOf(accountPlayTime && child?.let {clock.blocked(it)}==true)
    }
    val playableContent=access.activities(content,bundledIds)
    DisposableEffect(purchases) {onDispose {purchases.close()}}
    LaunchedEffect(purchases,foreground,purchaseState.configured) {
        if(foreground) {purchases.resume();var ticks=0;while(purchaseState.configured) {accessNow=System.currentTimeMillis();if(ticks++%30==0) purchases.checkAccess();delay(1000)}}
    }
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner,child?.id) {
        val obs=LifecycleEventObserver { _,e ->
            if(e==Lifecycle.Event.ON_STOP) { gateSession++; foreground=false; parentOpen=false; silence(); if(screen=="parent") screen="home" }
            if(e==Lifecycle.Event.ON_START) { foreground=true; clock.resume() }
        }; owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(child,foreground,parentOpen,gate,paused,breakDue,accountPlayTime) {
        if(!accountPlayTime) return@LaunchedEffect
        clock.resume()
        while(true) {
            delay(1000)
            child?.let { c ->
                if(foreground && !parentOpen && !gate && !paused && !breakDue && !clock.blocked(c)) { clock.tick(c.id) }
                blocked=clock.blocked(c)
                val due=clock.breakDue(c)
                if(due && !breakDue) silence()
                breakDue=due
            }
        }
    }
    DisposableEffect(gate,parentOpen) {
        val window=(context as? android.app.Activity)?.window
        if(gate||parentOpen) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    fun parent(destination:String="parent") { gateSession++; silence(); gateDestination=destination; gate=true }
    fun play(g:Game,a:Assignment?=null) { if(child==null || g.name !in child.subjects.split(',') || (a!=null && !access.assignmentsAllowed)) return; gameChildId=child.id; activeGame=g.name; activeAssignment=a?.id; screen="game"; paused=false }
    BackHandler(screen!="home" || gate || parentOpen) { if(security.configured) {silence(); gate=false; parentOpen=false; screen="home"; activeGame=null} }
    val high=child?.highContrast==true
    MaterialTheme(colorScheme=lightColorScheme(primary=if(high) Color(0xFF173F32) else Leaf,onPrimary=Color.White,background=if(high) Color.White else Cream,onBackground=Forest,surface=if(high) Color.White else Cream,onSurface=Forest)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            if(!gate) Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f).padding(end=12.dp)) { Text(stringResource(R.string.app_name),fontSize=22.sp,fontWeight=FontWeight.ExtraBold,color=Forest); Text(stringResource(R.string.ui_b133d173e5),fontSize=12.sp,color=Leaf) }
                    SoftButton(if(parentOpen) context.getString(R.string.ui_5053947772) else stringResource(R.string.parent_area)) { if(parentOpen) { parentOpen=false; screen="home" } else parent() }
                }
                if(contentError!=null) { Text(stringResource(R.string.ui_15dbcd5df9),Modifier.padding(32.dp)); return@Column }
                if(content.isEmpty()) { CircularProgressIndicator(Modifier.padding(32.dp)); return@Column }
                when {
                    parentOpen && screen=="parent" -> ParentDashboard(children,child,attempts,assignments,content,clock,packs,
                        progressExportStatus=progressExportStatus,packSelection=packSelection,onChoosePack=choosePack,onClearPack=clearPack,onChild={selectedId=it.id; prefs.edit().putString("child",it.id).apply()},
                        onSave={c->scope.launch { profileWrites.withLock {val saved=dao.children().first();val liveAccess=FamilyAccess(developmentAccess,purchases.state.value.verifiedUntil,System.currentTimeMillis());if(saved.any {it.id==c.id} || saved.size<liveAccess.profileLimit) dao.save(c)} }},onAssign={if(access.assignmentsAllowed) scope.launch { dao.save(it) }},
                        onDelete={id->scope.launch { dao.eraseChild(id); clock.clear(id); val sessions=context.getSharedPreferences("sessions",0); val edit=sessions.edit(); sessions.all.keys.filter {it.startsWith("$id-")}.forEach { edit.remove(it) }; edit.commit(); selectedId=null }},
                        onExport={export(progressCsv(children,attempts))},onPacks={scope.launch {val updated=withContext(Dispatchers.IO) {packs.starter()+packs.installed()};content=updated}},completions=completions,speechAvailable=speechAvailable,previewVoice={ text,volume->speak(text,volume/100f) },onChangePin={gateSession++; setup=true; gateDestination="parent"; gate=true},access=access,purchaseState=purchaseState,purchases=purchases)
                    child==null -> Column(Modifier.padding(32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Pip(Modifier.height(200.dp)); Heading(stringResource(R.string.ui_c5a63ad934))
                        Text(stringResource(R.string.ui_6fd5a23c40))
                        Action(stringResource(R.string.ui_3a0169f840)) { parent() }
                    }
                    blocked -> Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
                        Pip(Modifier.height(210.dp)); Heading(stringResource(R.string.time_up)); Text(stringResource(R.string.ui_34753d4c25))
                        Action(stringResource(R.string.ui_dce7095cb8)) { parent() }
                    }
                    screen=="game" && activeGame!=null -> {
                        if(gameChildId!=null && gameChildId!=child.id) {LaunchedEffect(Unit) {screen="home";silence()};return@Column}
                        if(activeAssignment!=null && !access.assignmentsAllowed) {LaunchedEffect(Unit) {screen="home";silence()};return@Column}
                        val assignment=assignments.find { it.id==activeAssignment && it.childId==child.id }
                        LaunchedEffect(access.assignmentsAllowed,child.id) {if(activeAssignment!=null && !access.assignmentsAllowed) {screen="home";silence()}}
                        GameScreen(child,Game.valueOf(activeGame!!),playableContent,attempts.filter { it.childId==child.id },assignment,{text->if(child.voiceVolume>0) speak(text,child.voiceVolume.coerceIn(0,100)/100f)},silence,
                            paused||breakDue,onPause={paused=true; silence()},onHome={screen="home"; silence()},onAttempt={a -> scope.launch {
                                dao.save(a)
                            }},onComplete={runId->if(assignment!=null) dao.completeMission(assignment,runId)})
                    }
                    else -> BoxWithConstraints(Modifier.weight(1f)) {
                        val wide=maxWidth>=840.dp
                        val medium=maxWidth>540.dp
                        Row(Modifier.fillMaxSize()) {
                            if(wide) Column(Modifier.width(170.dp).fillMaxHeight().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                listOf("home" to context.getString(R.string.ui_b0215dfa7a),"missions" to context.getString(R.string.ui_cc1decf45c),"rewards" to context.getString(R.string.ui_4c77b48d0e)).forEach { (key,label)->
                                    SoftButton(label,Modifier.fillMaxWidth()) { screen=key }
                                }
                                Spacer(Modifier.weight(1f)); Text(stringResource(R.string.ui_c39d646f10),fontSize=12.sp); Text(child.nickname,fontWeight=FontWeight.Bold,fontSize=20.sp)
                            }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(if(wide) 20.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                                    ExplorerAvatar(child,Modifier.padding(end=10.dp))
                                    Column(Modifier.weight(1f)) { Heading(if(screen=="home") stringResource(R.string.welcome) else if(screen=="missions") context.getString(R.string.ui_ce914592a8) else context.getString(R.string.ui_6e1d45405f))
                                        Text(stringResource(R.string.ui_500d8b05db ,child.nickname,child.year,attempts.count { it.childId==child.id }),color=Leaf) }
                                    if(accessibleChildren.size>1) SoftButton(stringResource(R.string.ui_a2db04b2e9)) { if(child.switchingAllowed) screen="switch" else parent("switch") }
                                }
                                when(screen) {
                                    "switch" -> accessibleChildren.forEach { c->Action(c.nickname) { selectedId=c.id; prefs.edit().putString("child",c.id).apply(); screen="home" } }
                                    "missions" -> {
                                        Text(stringResource(R.string.ui_3838507ad7))
                                        val mine=assignments.filter { access.assignmentsAllowed && it.childId==child.id && (it.completed==0 || (it.repeatDays>0 && System.currentTimeMillis()-it.completedAt>=it.repeatDays*86_400_000L)) }
                                        if(mine.isEmpty()) Text(stringResource(R.string.ui_aea553a33d))
                                        mine.forEach { a->Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFE6EDD7))) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                            Heading(Game.valueOf(a.game).displayTitle(context),true); Text(stringResource(R.string.ui_6a102e6768 ,a.count,a.year,if(a.skill.isBlank()) context.getString(R.string.ui_758d3c3f5d) else skillLabel(context,a.skill),a.due))
                                            if(a.targetMinutes>0) Text(stringResource(R.string.ui_235db5ec42 ,a.targetMinutes))
                                            Action(stringResource(R.string.ui_8cbba8f239)) { play(Game.valueOf(a.game),a) }
                                        } } }
                                    }
                                    "rewards" -> Rewards(child,attempts.filter { it.childId==child.id }) { decoration->scope.launch {dao.save(child.copy(decoration=decoration))} }
                                    else -> {
                                        Box(Modifier.fillMaxWidth().height(if(wide) 285.dp else 215.dp).clip(RoundedCornerShape(26.dp))) {
                                            Image(painterResource(R.drawable.sproutland),context.getString(R.string.ui_dfbdab12a0),Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                                            ExplorerPip(child,attempts.count {it.childId==child.id},Modifier.align(Alignment.BottomStart).height(if(wide) 200.dp else 140.dp).width(170.dp))
                                            Text(stringResource(R.string.ui_7b3466f472),Modifier.align(Alignment.TopStart).padding(16.dp).background(Cream,RoundedCornerShape(12.dp)).padding(8.dp),fontWeight=FontWeight.Bold)
                                        }
                                        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                            listOf(context.getString(R.string.ui_7873f7dee5),context.getString(R.string.ui_dcf608d660),context.getString(R.string.ui_9b45914e76),context.getString(R.string.ui_171af51be8)).forEachIndexed { index,name ->
                                                val unlocked=attempts.count { it.childId==child.id }>=index*3
                                                SoftButton(if(unlocked) context.getString(R.string.ui_09db48f46c, name) else context.getString(R.string.ui_03a290a193, name,index*3)) { screen=if(unlocked) "home" else "rewards" }
                                            }
                                        }
                                        Text(stringResource(R.string.ui_12312fa867),fontSize=22.sp,fontWeight=FontWeight.Bold)
                                        val games=Game.entries.filter { it.name in child.subjects.split(',') }
                                        val columns=if(wide) 3 else if(medium) 2 else 1
                                        games.chunked(columns).forEach { group -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                            group.forEach { g->Card(onClick={play(g)},modifier=Modifier.weight(1f).heightIn(min=118.dp),colors=CardDefaults.cardColors(containerColor=GameColors[g.ordinal]),shape=RoundedCornerShape(22.dp)) {
                                                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                                                    GameIcon(g,Modifier.size(56.dp))
                                                    Column { Text(g.displayTitle(context),fontSize=19.sp,fontWeight=FontWeight.ExtraBold); Text(g.displayDistrict(context),fontSize=12.sp); Text(stringResource(R.string.ui_d276a227cd),fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp)) }
                                                }
                                            } }; repeat(columns-group.size) { Spacer(Modifier.weight(1f)) }
                                        } }
                                        Text(stringResource(R.string.ui_62b5b1691d),fontSize=13.sp)
                                    }
                                }
                                Text(stringResource(R.string.development),fontSize=12.sp,color=Forest)
                                if(!wide) FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("home","missions","rewards").forEach { s->SoftButton(stringResource(when(s) {"home"->R.string.nav_home;"missions"->R.string.nav_missions;else->R.string.nav_rewards})) { screen=s } } }
                            }
                        }
                    }
                }
            }
            if((paused||breakDue) && !parentOpen && !gate && !blocked) AlertDialog(onDismissRequest={},title={Text(if(breakDue) context.getString(R.string.ui_e15ab438e0) else context.getString(R.string.ui_fcce06bc3c))},text={Text(if(breakDue) context.getString(R.string.ui_96437fbe15) else context.getString(R.string.ui_7cf170d208))},confirmButton={Action(stringResource(R.string.ui_31fbef1625)) { paused=false; breakDue=false; child?.let {clock.resetBreak(it.id)} }},dismissButton={SoftButton(stringResource(R.string.ui_39b71afa46)) { paused=false; breakDue=false; child?.let {clock.resetBreak(it.id)}; screen="home" }})
            if(gate && foreground) {
                val session=gateSession
                PinGate(security,setup && (!security.configured || parentOpen),onSuccess={
                    if(gate && foreground && gateSession==session) {
                        setup=false; gate=false
                        if(gateDestination=="switch") screen="switch" else {parentOpen=true; screen="parent"}
                    }
                },onDismiss={if(security.configured) {gateSession++;gate=false;setup=false}})
            }
        }
    }
}

@Composable fun PinGate(security:PinVault,setup:Boolean,onSuccess:()->Unit,onDismiss:()->Unit) {
    val ctx=LocalContext.current
    var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(),color=Cream) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
          Column(Modifier.widthIn(max=480.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.app_name),fontWeight=FontWeight.Bold,color=Leaf)
            Heading(if(setup) ctx.getString(R.string.ui_4951f2174b) else ctx.getString(R.string.ui_b48dabb9c3))
            Text(if(setup) ctx.getString(R.string.ui_95e6838aa9) else ctx.getString(R.string.ui_9f6d5a63a9))
            OutlinedTextField(pin,{if(!busy && acceptsPinInput(it)) pin=it},enabled=!busy,label={Text(stringResource(R.string.ui_addd9d0383))},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true)
            if(setup) OutlinedTextField(confirm,{if(!busy && acceptsPinInput(it)) confirm=it},enabled=!busy,label={Text(stringResource(R.string.ui_179acd046e))},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true)
            if(error.isNotEmpty()) Text(error,color=Color(0xFF973C24),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
            Action(if(busy) stringResource(R.string.status_checking) else if(setup) ctx.getString(R.string.ui_9e49e152f5) else stringResource(R.string.pin_unlock),enabled=!busy && validParentPin(pin)) {
                if(busy) return@Action
                val submittedPin=pin;val submittedConfirmation=confirm
                if(setup && submittedPin!=submittedConfirmation) {error=ctx.getString(R.string.ui_8fc8b95fb9);return@Action}
                busy=true;error=""
                scope.launch {
                    try {
                        val result=submitParentPin(security,setup,submittedPin,submittedConfirmation)
                        currentCoroutineContext().ensureActive()
                        when(result.status) {
                            PinSubmissionStatus.MATCH->onSuccess()
                            PinSubmissionStatus.NO_MATCH->error=ctx.getString(R.string.ui_57a8426ec3)
                            PinSubmissionStatus.INVALID->error=ctx.getString(R.string.ui_8fc8b95fb9)
                            PinSubmissionStatus.LOCKED->error=ctx.getString(R.string.ui_cca375e476,result.secondsLocked)
                            PinSubmissionStatus.UNAVAILABLE->error=ctx.getString(R.string.pin_service_unavailable)
                        }
                    } finally {busy=false;pin="";confirm=""}
                }
            }
            if(security.configured) SoftButton(stringResource(R.string.ui_78184204de),onClick=onDismiss)
          }
        }
    }
}

fun progressCsv(children:List<Child>,attempts:List<Attempt>):String {
    fun cell(s:String)="\""+s.replace("\"","\"\"").let { if(it.firstOrNull() in listOf('=','+','-','@')) "'"+it else it }+"\""
    return "nickname,activity,skill,correct,hints,difficulty,duration_ms,utc\n"+attempts.joinToString("\n") { a->listOf(children.find { it.id==a.childId }?.nickname.orEmpty(),a.activityId,a.skill,a.correct.toString(),a.hints.toString(),a.difficulty.toString(),a.durationMs.toString(),Instant.ofEpochMilli(a.at).toString()).joinToString(",",transform=::cell) }
}

@Composable fun Rewards(child:Child,attempts:List<Attempt>,onDecoration:(String)->Unit) {
    val ctx=LocalContext.current
    val stars=attempts.size
    Row(verticalAlignment=Alignment.CenterVertically) { ExplorerPip(child,stars,Modifier.height(170.dp).width(150.dp)); Column(Modifier.weight(1f)) { Heading(pluralStringResource(R.plurals.effort_stars,stars,stars)); Text(stringResource(R.string.ui_e8a85cbb6b)) } }
    listOf(1 to ctx.getString(R.string.ui_b8d3fc5385),5 to ctx.getString(R.string.ui_bed584eb4c),15 to ctx.getString(R.string.ui_5bdb7dfe08),30 to ctx.getString(R.string.ui_b1db56a3b6)).forEach { (n,label)->
        Text(pluralStringResource(R.plurals.reward_discoveries,n,if(stars>=n) "★" else "☆",label,n),fontSize=22.sp,modifier=Modifier.padding(10.dp))
    }
    val recent=attempts.filter { it.at>=System.currentTimeMillis()-7*86_400_000L }
    Text(pluralStringResource(R.plurals.weekly_discoveries,recent.size,recent.size),fontWeight=FontWeight.Bold)
    Heading(stringResource(R.string.decoration_choose),true)
    Text(stringResource(R.string.decoration_help))
    val selected=Decoration.available(child.decoration,stars)
    Decoration.entries.forEach { decoration->
        val label=stringResource(decoration.label)
        if(stars<decoration.stars) Text(stringResource(R.string.decoration_locked,label,decoration.stars))
        else if(decoration==selected) Action(stringResource(R.string.decoration_selected,label),enabled=false) {}
        else SoftButton(stringResource(R.string.decoration_unlocked,label)) {onDecoration(decoration.id)}
    }
}
