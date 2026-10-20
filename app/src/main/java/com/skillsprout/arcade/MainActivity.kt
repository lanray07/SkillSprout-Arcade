package com.skillsprout.arcade

import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
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
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import java.time.*
import java.util.*

val Forest=Color(0xFF173F32)
val Leaf=Color(0xFF427C39)
val Cream=Color(0xFFFFF9EE)
val Peach=Color(0xFFF4C567)
val GameColors=listOf(0xFFD9EABD,0xFFFFD786,0xFFBEE4F1,0xFFF9C6B6,0xFFE0CEF9,0xFFFFE5A8).map { Color(it) }

class MainActivity: ComponentActivity() {
    private lateinit var db:ArcadeDatabase
    private var tts:TextToSpeech?=null
    private var speechReady=false
    private var exportText=""
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if(uri!=null) lifecycleScope.launch(Dispatchers.IO) { contentResolver.openOutputStream(uri)?.use { it.write(exportText.toByteArray()) } }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        db=ArcadeDatabase.open(this)
        tts=TextToSpeech(this) { status ->
            if(status==TextToSpeech.SUCCESS) {
                val local=tts?.voices?.firstOrNull { !it.isNetworkConnectionRequired && it.locale.language=="en" && it.locale.country=="GB" }
                    ?: tts?.voices?.firstOrNull { !it.isNetworkConnectionRequired && it.locale.language=="en" }
                if(local!=null) { tts?.voice=local; speechReady=true }
            }
        }
        setContent { ArcadeApp(db.dao(),speak={ if(speechReady) tts?.speak(it,TextToSpeech.QUEUE_FLUSH,null,"pip") },
            silence={tts?.stop()},export={ exportText=it; export.launch("skillsprout-progress.csv") }) }
    }
    override fun onStop() { tts?.stop(); super.onStop() }
    override fun onDestroy() { tts?.shutdown(); db.close(); super.onDestroy() }
}

@Composable fun Action(label:String, modifier:Modifier=Modifier, enabled:Boolean=true, onClick:()->Unit) {
    Button(onClick,modifier.heightIn(min=52.dp),enabled=enabled,shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Leaf)) { Text(label,fontSize=16.sp,fontWeight=FontWeight.Bold) }
}
@Composable fun SoftButton(label:String, modifier:Modifier=Modifier, onClick:()->Unit) {
    OutlinedButton(onClick,modifier.heightIn(min=48.dp),shape=RoundedCornerShape(16.dp)) { Text(label,color=Forest) }
}
@Composable fun Heading(text:String,small:Boolean=false) { Text(text,fontSize=if(small) 23.sp else 32.sp,fontWeight=FontWeight.ExtraBold,color=Forest,lineHeight=if(small) 29.sp else 38.sp) }
@Composable fun Pip(modifier:Modifier=Modifier) { Image(painterResource(R.drawable.pip),"Pip, your woodland guide",modifier,contentScale=ContentScale.Fit) }
@Composable fun GameIcon(game:Game,modifier:Modifier=Modifier) {
    val id=when(game) {Game.MATHS->R.drawable.game_maths;Game.SPELLING->R.drawable.game_spelling;Game.STORY->R.drawable.game_story;Game.TABLES->R.drawable.game_tables;Game.LOGIC->R.drawable.game_logic;Game.WORDS->R.drawable.game_words}
    Image(painterResource(id),null,modifier)
}

@Composable fun ArcadeApp(dao:ArcadeDao,speak:(String)->Unit,silence:()->Unit,export:(String)->Unit,pinVault:PinVault?=null,accountPlayTime:Boolean=true) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val children by dao.children().collectAsState(initial=emptyList())
    val attempts by dao.attempts().collectAsState(initial=emptyList())
    val assignments by dao.assignments().collectAsState(initial=emptyList())
    val security=remember { pinVault ?: ParentSecurity(context) }
    val clock=remember { PlayClock(context) }
    val packs=remember { ContentPacks(context) }
    var content by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var contentError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { runCatching { packs.starter()+packs.installed() }.onSuccess { content=it }.onFailure { contentError=it.message } } }
    val prefs=remember { context.getSharedPreferences("arcade_ui",0) }
    var selectedId by rememberSaveable { mutableStateOf(prefs.getString("child",null)) }
    val child=children.find { it.id==selectedId } ?: children.firstOrNull()
    var screen by rememberSaveable { mutableStateOf("home") }
    var parentOpen by remember { mutableStateOf(false) }
    var gate by remember { mutableStateOf(!security.configured) }
    var gateDestination by remember { mutableStateOf("parent") }
    var setup by remember { mutableStateOf(!security.configured) }
    var activeGame by rememberSaveable { mutableStateOf<String?>(null) }
    var activeAssignment by rememberSaveable { mutableStateOf<String?>(null) }
    var paused by remember { mutableStateOf(false) }
    var breakDue by remember { mutableStateOf(false) }
    var sessionMs by remember { mutableLongStateOf(0) }
    var blocked by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(true) }
    var clockVersion by remember { mutableIntStateOf(0) }
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner,child?.id) {
        val obs=LifecycleEventObserver { _,e ->
            if(e==Lifecycle.Event.ON_STOP) { foreground=false; parentOpen=false; silence(); if(screen=="parent") screen="home" }
            if(e==Lifecycle.Event.ON_START) { foreground=true; clock.resume() }
        }; owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(child?.id,foreground,parentOpen,gate,paused,breakDue) {
        if(!accountPlayTime) return@LaunchedEffect
        clock.resume()
        while(true) {
            delay(1000)
            child?.let { c ->
                if(foreground && !parentOpen && !gate && !paused && !breakDue && !clock.blocked(c)) { clock.tick(c.id); sessionMs+=1000; clockVersion++ }
                blocked=clock.blocked(c)
                if(c.breakMinutes>0 && sessionMs>=c.breakMinutes*60_000L) { breakDue=true; silence() }
            }
        }
    }
    DisposableEffect(gate,parentOpen) {
        val window=(context as? android.app.Activity)?.window
        if(gate||parentOpen) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    fun parent(destination:String="parent") { silence(); gateDestination=destination; gate=true }
    fun play(g:Game,a:Assignment?=null) { activeGame=g.name; activeAssignment=a?.id; screen="game"; paused=false }
    BackHandler(screen!="home" || gate || parentOpen) { if(security.configured) {silence(); gate=false; parentOpen=false; screen="home"; activeGame=null} }
    val high=child?.highContrast==true
    MaterialTheme(colorScheme=lightColorScheme(primary=if(high) Color(0xFF173F32) else Leaf,onPrimary=Color.White,background=if(high) Color.White else Cream,onBackground=Forest,surface=if(high) Color.White else Cream,onSurface=Forest)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            if(!gate) Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Column { Text(stringResource(R.string.app_name),fontSize=22.sp,fontWeight=FontWeight.ExtraBold,color=Forest); Text(stringResource(R.string.ui_b133d173e5),fontSize=12.sp,color=Leaf) }
                    SoftButton(if(parentOpen) "Lock parent area" else stringResource(R.string.parent_area)) { if(parentOpen) { parentOpen=false; screen="home" } else parent() }
                }
                if(contentError!=null) { Text(stringResource(R.string.ui_15dbcd5df9),Modifier.padding(32.dp)); return@Column }
                if(content.isEmpty()) { CircularProgressIndicator(Modifier.padding(32.dp)); return@Column }
                when {
                    parentOpen && screen=="parent" -> ParentDashboard(children,child,attempts,assignments,content,clock,packs,
                        onChild={selectedId=it.id; prefs.edit().putString("child",it.id).apply()},
                        onSave={scope.launch { dao.save(it) }},onAssign={scope.launch { dao.save(it) }},
                        onDelete={id->scope.launch { dao.eraseChild(id); clock.clear(id); val sessions=context.getSharedPreferences("sessions",0); val edit=sessions.edit(); sessions.all.keys.filter {it.startsWith("$id-")}.forEach { edit.remove(it) }; edit.commit(); selectedId=null }},
                        onExport={export(progressCsv(children,attempts))},onPacks={content=packs.starter()+packs.installed()},onChangePin={setup=true; gateDestination="parent"; gate=true})
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
                        val assignment=assignments.find { it.id==activeAssignment }
                        GameScreen(child,Game.valueOf(activeGame!!),content,attempts.filter { it.childId==child.id },assignment,speak,silence,
                            paused||breakDue,onPause={paused=true; silence()},onHome={screen="home"; silence()},onAttempt={a -> scope.launch {
                                dao.save(a)
                            }},onComplete={if(assignment!=null) scope.launch { dao.save(assignment.copy(completed=assignment.completed+1,completedAt=System.currentTimeMillis())) }})
                    }
                    else -> BoxWithConstraints(Modifier.weight(1f)) {
                        val wide=maxWidth>=840.dp
                        val medium=maxWidth>540.dp
                        Row(Modifier.fillMaxSize()) {
                            if(wide) Column(Modifier.width(170.dp).fillMaxHeight().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                listOf("home" to "⌂  Home","missions" to "⚑  Missions","rewards" to "☆  Rewards").forEach { (key,label)->
                                    SoftButton(label,Modifier.fillMaxWidth()) { screen=key }
                                }
                                Spacer(Modifier.weight(1f)); Text(stringResource(R.string.ui_c39d646f10),fontSize=12.sp); Text(child.nickname,fontWeight=FontWeight.Bold,fontSize=20.sp)
                            }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(if(wide) 20.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Heading(if(screen=="home") stringResource(R.string.welcome) else if(screen=="missions") "Your little missions" else "Look how you’ve grown!")
                                        Text("${child.nickname} · Year ${child.year} · ${attempts.count { it.childId==child.id }} effort stars",color=Leaf) }
                                    if(children.size>1) SoftButton(stringResource(R.string.ui_a2db04b2e9)) { if(child.switchingAllowed) screen="switch" else parent("switch") }
                                }
                                when(screen) {
                                    "switch" -> children.forEach { c->Action(c.nickname) { selectedId=c.id; prefs.edit().putString("child",c.id).apply(); screen="home" } }
                                    "missions" -> {
                                        Text(stringResource(R.string.ui_3838507ad7))
                                        val mine=assignments.filter { it.childId==child.id && (it.completed==0 || (it.repeatDays>0 && System.currentTimeMillis()-it.completedAt>=it.repeatDays*86_400_000L)) }
                                        if(mine.isEmpty()) Text(stringResource(R.string.ui_aea553a33d))
                                        mine.forEach { a->Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFE6EDD7))) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                            Heading(Game.valueOf(a.game).title,true); Text("${a.count} challenges · Year ${a.year} · ${a.skill.ifBlank { "Mixed skills" }} · Due ${a.due}")
                                            if(a.targetMinutes>0) Text("About ${a.targetMinutes} minutes, at your own pace")
                                            Action(stringResource(R.string.ui_8cbba8f239)) { play(Game.valueOf(a.game),a) }
                                        } } }
                                    }
                                    "rewards" -> Rewards(attempts.filter { it.childId==child.id })
                                    else -> {
                                        Box(Modifier.fillMaxWidth().height(if(wide) 285.dp else 215.dp).clip(RoundedCornerShape(26.dp))) {
                                            Image(painterResource(R.drawable.sproutland),"Sproutland: Number Valley, Wordwood Forest, Story Harbour and Puzzle Peaks",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                                            Pip(Modifier.align(Alignment.BottomStart).height(if(wide) 200.dp else 140.dp).width(170.dp))
                                            Text(stringResource(R.string.ui_7b3466f472),Modifier.align(Alignment.TopStart).padding(16.dp).background(Cream,RoundedCornerShape(12.dp)).padding(8.dp),fontWeight=FontWeight.Bold)
                                        }
                                        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                            listOf("Number Valley","Wordwood Forest","Story Harbour","Puzzle Peaks").forEachIndexed { index,name ->
                                                val unlocked=attempts.count { it.childId==child.id }>=index*3
                                                SoftButton(if(unlocked) "$name  ✓" else "$name · ${index*3} stars") { screen=if(unlocked) "home" else "rewards" }
                                            }
                                        }
                                        Text(stringResource(R.string.ui_12312fa867),fontSize=22.sp,fontWeight=FontWeight.Bold)
                                        val games=Game.entries.filter { it.name in child.subjects.split(',') }
                                        val columns=if(wide) 3 else if(medium) 2 else 1
                                        games.chunked(columns).forEach { group -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                            group.forEach { g->Card(onClick={play(g)},modifier=Modifier.weight(1f).heightIn(min=118.dp),colors=CardDefaults.cardColors(containerColor=GameColors[g.ordinal]),shape=RoundedCornerShape(22.dp)) {
                                                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                                                    GameIcon(g,Modifier.size(56.dp))
                                                    Column { Text(g.title,fontSize=19.sp,fontWeight=FontWeight.ExtraBold); Text(g.district,fontSize=12.sp); Text(stringResource(R.string.ui_d276a227cd),fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp)) }
                                                }
                                            } }; repeat(columns-group.size) { Spacer(Modifier.weight(1f)) }
                                        } }
                                        Text(stringResource(R.string.ui_62b5b1691d),fontSize=13.sp)
                                    }
                                }
                                Text(stringResource(R.string.development),fontSize=12.sp,color=Forest)
                                if(!wide) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) { listOf("home","missions","rewards").forEach { s->SoftButton(s.replaceFirstChar { it.uppercase() }) { screen=s } } }
                            }
                        }
                    }
                }
            }
            if((paused||breakDue) && !parentOpen && !gate && !blocked) AlertDialog(onDismissRequest={},title={Text(if(breakDue) "A little pause for Pip" else "Adventure paused")},text={Text(if(breakDue) "Stretch, look away from the screen, and rest your eyes. Your progress is saved." else "Take your time. Your adventure will wait.")},confirmButton={Action(stringResource(R.string.ui_31fbef1625)) { paused=false; breakDue=false; sessionMs=0; clock.resume() }},dismissButton={SoftButton(stringResource(R.string.ui_39b71afa46)) { paused=false; breakDue=false; sessionMs=0; screen="home" }})
            if(gate && foreground) PinGate(security,setup,onSuccess={
                setup=false; gate=false
                if(gateDestination=="switch") screen="switch" else {parentOpen=true; screen="parent"}
            },onDismiss={if(security.configured) { gate=false; setup=false }})
        }
    }
}

@Composable fun PinGate(security:PinVault,setup:Boolean,onSuccess:()->Unit,onDismiss:()->Unit) {
    var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(),color=Cream) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
          Column(Modifier.widthIn(max=480.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.app_name),fontWeight=FontWeight.Bold,color=Leaf)
            Heading(if(setup) "Grown-ups: create a parent PIN" else "Grown-ups only")
            Text(if(setup) "Choose 6–12 digits and keep them private. This protects settings, profiles and purchases. There is no online PIN recovery." else "Enter your parent PIN. Pip keeps these controls just for grown-ups.")
            OutlinedTextField(pin,{if(it.length<=12 && it.all(Char::isDigit)) pin=it},label={Text(stringResource(R.string.ui_addd9d0383))},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true)
            if(setup) OutlinedTextField(confirm,{if(it.length<=12 && it.all(Char::isDigit)) confirm=it},label={Text(stringResource(R.string.ui_179acd046e))},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true)
            if(error.isNotEmpty()) Text(error,color=Color(0xFF973C24),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
            Action(if(busy) "Checking…" else if(setup) "Save PIN" else "Unlock",enabled=!busy) {
        if(setup && (pin.length<6 || pin!=confirm)) {error="Use 6–12 digits and make sure both entries match."; return@Action}
        busy=true; scope.launch {
            val ok=withContext(Dispatchers.IO) { runCatching { if(setup) {security.setPin(pin);true} else security.verify(pin) }.getOrDefault(false) }
            busy=false; pin=""; confirm=""
            if(ok) onSuccess() else error=if(security.secondsLocked()>0) "Please wait ${security.secondsLocked()} seconds before trying again." else "That PIN did not match. Try again."
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

@Composable fun Rewards(attempts:List<Attempt>) {
    val stars=attempts.size
    Row(verticalAlignment=Alignment.CenterVertically) { Pip(Modifier.height(170.dp).width(150.dp)); Column { Heading("$stars effort stars"); Text(stringResource(R.string.ui_e8a85cbb6b)) } }
    listOf(1 to "First discovery",5 to "Curious explorer",15 to "Woodland helper",30 to "Sproutland adventurer").forEach { (n,label)->
        Text("${if(stars>=n) "★" else "☆"}  $label · $n discoveries",fontSize=22.sp,modifier=Modifier.padding(10.dp))
    }
    val recent=attempts.filter { it.at>=System.currentTimeMillis()-7*86_400_000L }
    Text("This week: ${recent.size} discoveries. Every little bit counts.",fontWeight=FontWeight.Bold)
    Text(if(stars>=5) "Pip earned a sunflower decoration for the valley!" else "A sunflower decoration grows after five discoveries.")
}
