package com.skillsprout.arcade
import androidx.compose.ui.res.stringResource

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import java.time.*
import java.util.UUID

@Composable fun Choice(label:String,choices:List<String>,value:String,onChange:(String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column { Text(label,fontWeight=FontWeight.Bold); Box { SoftButton("$value  ▾") {expanded=true}; DropdownMenu(expanded,{expanded=false}) {choices.forEach { c->DropdownMenuItem(text={Text(c)},onClick={expanded=false;onChange(c)})}} } }
}

@Composable fun ParentDashboard(children:List<Child>,selected:Child?,attempts:List<Attempt>,assignments:List<Assignment>,content:List<Activity>,clock:PlayClock,packs:ContentPacks,
    onChild:(Child)->Unit,onSave:(Child)->Unit,onAssign:(Assignment)->Unit,onDelete:(String)->Unit,onExport:()->Unit,onPacks:()->Unit,onChangePin:()->Unit) {
    var tab by remember { mutableStateOf("Progress") }
    var message by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<String?>(null) }
    val ctx=LocalContext.current
    val scope=rememberCoroutineScope()
    var pendingPack by remember { mutableStateOf<ByteArray?>(null) }
    var digest by remember { mutableStateOf("") }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) scope.launch {
            val result=withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openInputStream(uri)?.use { stream->stream.readBytesLimited(5_000_000) } } }
            pendingPack=result.getOrNull(); message=result.exceptionOrNull()?.message.orEmpty()
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Heading(stringResource(R.string.ui_f13d11cfef))
        Text(stringResource(R.string.ui_89867bc9f3),color=Forest)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf("Progress","Practice","Explorers","Controls","Packs","Premium").forEach { name->if(tab==name) Action(name) {tab=name} else SoftButton(name) {tab=name} }
        }
        if(children.isNotEmpty()) Choice("Explorer",children.map { it.nickname },selected?.nickname.orEmpty()) { n->children.find {it.nickname==n}?.let(onChild) }
        if(message.isNotEmpty()) Text(message,fontWeight=FontWeight.Bold)
        when(tab) {
            "Progress" -> {
                val mine=attempts.filter {it.childId==selected?.id}
                val independent=mine.count {it.correct && it.hints==0}
                Heading(selected?.let { "${it.nickname}’s discoveries" } ?: "Create your first explorer",true)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Metric("Challenges",mine.size.toString()); Metric("Active today", "${selected?.let {clock.used(it.id)/60_000} ?: 0} min")
                    Metric("First-try accuracy",if(mine.isEmpty()) "—" else "${mine.count {it.correct}*100/mine.size}%")
                    Metric("Independent",independent.toString())
                    Metric("Total active practice", "${selected?.let {clock.total(it.id)/60_000} ?: 0} min")
                }
                Text(stringResource(R.string.ui_4090e48ce1))
                Heading(stringResource(R.string.ui_8c4eef5ab2),true)
                val today=LocalDate.now()
                Row(Modifier.fillMaxWidth().height(130.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    (6 downTo 0).forEach { ago-> val day=today.minusDays(ago.toLong()); val count=mine.count { Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate()==day }
                        Column(Modifier.weight(1f)) { Spacer(Modifier.weight(1f)); Text(count.toString()); Box(Modifier.fillMaxWidth().height((count*8+4).coerceAtMost(90).dp).background(Leaf,RoundedCornerShape(8.dp))); Text(day.dayOfWeek.name.take(3),fontSize=11.sp) }
                    }
                }
                Heading(stringResource(R.string.ui_eec91566a6),true)
                if(mine.isEmpty()) Text(stringResource(R.string.ui_2c5b14e58c))
                mine.groupBy {it.skill}.forEach { (skill,evidence)->
                    val m=LearningEngine.mastery(evidence.map {Evidence(it.correct,it.hints,it.difficulty)})
                    Row(Modifier.fillMaxWidth().background(Color(0xFFEAF0DE),RoundedCornerShape(14.dp)).padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text(skill.replace('-',' ').replaceFirstChar {it.uppercase()},fontWeight=FontWeight.Bold); Text(m.label) }; Text("${m.accuracy}%")
                    }
                }
                Action(stringResource(R.string.ui_c5b4cd9303),enabled=attempts.isNotEmpty()) {onExport()}
                Text(stringResource(R.string.ui_0bdd03b627),fontSize=12.sp)
            }
            "Practice" -> if(selected==null) Text(stringResource(R.string.ui_0f76020e56)) else key(selected.id) {
                var game by remember {mutableStateOf(Game.MATHS)}; var year by remember {mutableIntStateOf(selected.year)}
                var skill by remember {mutableStateOf("")}; var count by remember {mutableStateOf("5")}
                var difficulty by remember {mutableStateOf("1")}; var due by remember {mutableStateOf(LocalDate.now().plusDays(3).toString())}
                var repeat by remember {mutableStateOf("0")}; var target by remember {mutableStateOf("0")}
                Heading(stringResource(R.string.ui_65e0d4a7ba),true)
                Choice("Subject / game",Game.entries.filter {it.name in selected.subjects.split(',')}.map {it.title},game.title) { value->game=Game.entries.first {it.title==value};skill="" }
                Choice("Skill",listOf("Mixed skills")+content.filter {it.game==game && it.year==year}.map {it.skill}.distinct(),skill.ifBlank {"Mixed skills"}) {skill=if(it=="Mixed skills") "" else it}
                Choice("Year level",(1..6).map {it.toString()},year.toString()) {year=it.toInt();skill=""}
                Choice("Difficulty",listOf("1","2","3"),difficulty) {difficulty=it}
                Choice("Number of questions",listOf("3","5","10","15","20"),count) {count=it}
                OutlinedTextField(due,{due=it},label={Text(stringResource(R.string.ui_50b406fc25))},singleLine=true)
                Choice("Repeat every (days; 0 = once)",listOf("0","1","3","7"),repeat) {repeat=it}
                Choice("Optional time target (minutes; no pressure)",listOf("0","2","3","5","10"),target) {target=it}
                Action(stringResource(R.string.ui_36f68334cb)) {
                    if(runCatching {LocalDate.parse(due)}.isFailure) message="Enter a valid date, for example 2026-10-23."
                    else if(game.name !in selected.subjects.split(',')) message="Enable this subject first."
                    else {onAssign(Assignment(UUID.randomUUID().toString(),selected.id,game.name,skill,year,count.toInt(),difficulty.toInt(),due,repeat.toInt(),target.toInt()));message="Mission assigned to ${selected.nickname}."}
                }
                assignments.filter {it.childId==selected.id}.forEach { a->Text("${Game.valueOf(a.game).title} · ${a.count} challenges · due ${a.due} · ${if(a.completed==0) "Waiting" else "Completed ${a.completed} time(s)"}") }
            }
            "Explorers" -> {
                Heading(stringResource(R.string.ui_48c15eed99),true)
                Text(stringResource(R.string.ui_b76ba4436f))
                children.forEach { c->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { SoftButton("${c.nickname} · Year ${c.year}") {onChild(c)};SoftButton("Delete ${c.nickname}") {deleteId=c.id} } }
                var nickname by remember {mutableStateOf("")}; var year by remember {mutableStateOf("1")}; var avatar by remember {mutableStateOf("0")}
                OutlinedTextField(nickname,{nickname=it.take(20)},label={Text(stringResource(R.string.ui_35aca3e089))},singleLine=true)
                Choice("Starting year",(1..6).map {it.toString()},year) {year=it}
                Choice("Avatar",listOf("0","1","2","3"),avatar) {avatar=it}
                Action(stringResource(R.string.ui_c4df01117c),enabled=children.size<4) {
                    val name=nickname.trim()
                    if(name.length<2) message="Use a nickname with at least two characters."
                    else if(children.any {it.nickname.equals(name,true)}) message="Please choose a different nickname."
                    else { val c=Child(UUID.randomUUID().toString(),name,avatar.toInt(),year.toInt());onSave(c);onChild(c);nickname="";message="Explorer created." }
                }
                Text(stringResource(R.string.ui_12dc0b48c9))
            }
            "Controls" -> if(selected==null) Text(stringResource(R.string.ui_0f76020e56)) else key(selected.id,selected) {
                var draft by remember {mutableStateOf(selected)}
                Heading(stringResource(R.string.ui_25ccf390c8),true)
                Choice("Year / ability level",(1..6).map {it.toString()},draft.year.toString()) {draft=draft.copy(year=it.toInt())}
                Choice("Daily allowance (minutes)",listOf("5","10","15","20","30","45","60"),draft.allowanceMinutes.toString()) {draft=draft.copy(allowanceMinutes=it.toInt())}
                Choice("Bedtime starts (hour)",(0..23).map {it.toString()},draft.bedtimeHour.toString()) {draft=draft.copy(bedtimeHour=it.toInt())}
                Choice("Play available from (hour)",(0..23).map {it.toString()},draft.wakeHour.toString()) {draft=draft.copy(wakeHour=it.toInt())}
                Choice("Suggest a break every (minutes; 0 = off)",listOf("0","5","10","15","20"),draft.breakMinutes.toString()) {draft=draft.copy(breakMinutes=it.toInt())}
                Toggle("Spoken guidance (offline voice, when installed)",draft.voice) {draft=draft.copy(voice=it)}
                Toggle("Reduced motion",draft.reducedMotion) {draft=draft.copy(reducedMotion=it)}
                Toggle("High contrast",draft.highContrast) {draft=draft.copy(highContrast=it)}
                Toggle("Allow switching explorers without a parent PIN",draft.switchingAllowed) {draft=draft.copy(switchingAllowed=it)}
                Text(stringResource(R.string.ui_22e02cedda),fontWeight=FontWeight.Bold)
                Game.entries.forEach {g->Toggle(g.title,g.name in draft.subjects.split(',')) {on-> val set=draft.subjects.split(',').filter {it.isNotBlank()}.toMutableSet();if(on) set.add(g.name) else set.remove(g.name);draft=draft.copy(subjects=set.joinToString(","))}}
                Action(stringResource(R.string.ui_640583cc0c)) {if(draft.subjects.isBlank()) message="Enable at least one subject." else if(draft.bedtimeHour==draft.wakeHour) message="Choose different bedtime and wake hours." else {onSave(draft);message="Controls saved."}}
                SoftButton(stringResource(R.string.ui_fb217d72ec)) {clock.bonus(selected.id);message="Five minutes added. Bedtime still applies."}
                SoftButton(stringResource(R.string.ui_1873a4ea29),onClick=onChangePin)
                Text(stringResource(R.string.ui_8ae41ee339),fontSize=12.sp)
                Heading(stringResource(R.string.ui_186352a881),true); Text(stringResource(R.string.ui_2919dde6e0))
            }
            "Packs" -> {
                Heading(stringResource(R.string.ui_c476b07dee),true)
                Text("English (UK) starter pack · ${content.count {it.id.startsWith("dev-")}} activities · version 1\nBundled and integrity checked. Awaiting teacher review.")
                Text("Optional installed activities: ${packs.installed().size}")
                Text(stringResource(R.string.ui_ef8b86ab1d))
                Action(stringResource(R.string.ui_e4f4c04d74)) {importer.launch(arrayOf("application/json"))}
                SoftButton(stringResource(R.string.ui_853c2678a2)) {packs.deleteOptional();onPacks();message="Optional packs removed. Learning progress is preserved."}
                if(pendingPack!=null) {
                    OutlinedTextField(digest,{digest=it.trim().lowercase()},label={Text(stringResource(R.string.ui_b038fb0d0e))},modifier=Modifier.fillMaxWidth())
                    Action(stringResource(R.string.ui_27d773b934)) {
                        runCatching {packs.import(pendingPack!!,digest)}.onSuccess {message="$it activities installed.";pendingPack=null;digest="";onPacks()}.onFailure {message="Pack rejected: ${it.message}"}
                    }
                    SoftButton(stringResource(R.string.ui_6dd6cafb0d)) {pendingPack=null;digest=""}
                }
            }
            "Premium" -> {
                Heading(stringResource(R.string.ui_3117341b2b),true)
                Text(stringResource(R.string.ui_f8d19e2007))
                Text(stringResource(R.string.ui_a1f105476d),fontWeight=FontWeight.Bold)
                Text(stringResource(R.string.ui_42b498ede2))
                Text(stringResource(R.string.ui_2d3af979de))
            }
        }
        Text(stringResource(R.string.ui_14a3518780),fontSize=12.sp)
    }
    if(deleteId!=null) AlertDialog(onDismissRequest={deleteId=null},title={Text(stringResource(R.string.ui_7a30e2bc21))},text={Text(stringResource(R.string.ui_d36e13a461))},confirmButton={Action(stringResource(R.string.ui_e39fd420a5)) {deleteId?.let(onDelete);deleteId=null}},dismissButton={SoftButton(stringResource(R.string.ui_987316a350)) {deleteId=null}})
}
@Composable fun Metric(label:String,value:String) { Column(Modifier.widthIn(min=140.dp).background(Color(0xFFE6EDD7),RoundedCornerShape(18.dp)).padding(18.dp)) {Text(value,fontSize=30.sp,fontWeight=FontWeight.ExtraBold);Text(label)} }
@Composable fun Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(label,Modifier.weight(1f).padding(vertical=14.dp));Switch(value,onChange)} }
private fun java.io.InputStream.readBytesLimited(limit:Int):ByteArray {
    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
    while(true) {val n=read(buffer);if(n<0) break;require(out.size()+n<=limit) {"Pack is too large"};out.write(buffer,0,n)}
    return out.toByteArray()
}
