package com.skillsprout.arcade
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import java.time.*
import java.util.UUID

@Composable fun Choice(label:String,choices:List<String>,value:String,optionLabel:(String)->String={it},onChange:(String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column { Text(label,fontWeight=FontWeight.Bold); Box { SoftButton(stringResource(R.string.ui_557962bb2b ,optionLabel(value))) {expanded=true}; DropdownMenu(expanded,{expanded=false}) {choices.forEach { c->DropdownMenuItem(text={Text(optionLabel(c))},onClick={expanded=false;onChange(c)})}} } }
}

@Composable fun ParentDashboard(children:List<Child>,selected:Child?,attempts:List<Attempt>,assignments:List<Assignment>,content:List<Activity>,clock:PlayClock,packs:ContentPacks,
    onChild:(Child)->Unit,onSave:(Child)->Unit,onAssign:(Assignment)->Unit,onDelete:(String)->Unit,onExport:()->Unit,onPacks:()->Unit,onChangePin:()->Unit,previewVoice:(String,Int)->Unit={_,_->},completions:List<MissionCompletion> = emptyList(),speechAvailable:Boolean=true,access:FamilyAccess=FamilyAccess(true,0,0),purchaseState:PurchaseState=PurchaseState(),purchases:PurchaseGateway=UnavailablePurchases(),progressExportStatus:ProgressExportStatus=ProgressExportStatus.IDLE,packSelection:PackSelection=PackSelection(),onChoosePack:()->Unit={},onClearPack:()->Unit={}) {
    var tab by remember { mutableStateOf("Progress") }
    var message by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<String?>(null) }
    val avatarNames=listOf(R.string.avatar_leaf,R.string.avatar_sun,R.string.avatar_star,R.string.avatar_wave).map {stringResource(it)}
    val ctx=LocalContext.current
    val displayLocale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val scope=rememberCoroutineScope()
    val pendingPack=packSelection.bytes
    var digest by remember(pendingPack) { mutableStateOf("") }
    var packBusy by remember { mutableStateOf(false) }
    val packControlsBusy=packBusy || packSelection.status.busy
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Heading(stringResource(R.string.ui_f13d11cfef))
        Text(stringResource(R.string.ui_89867bc9f3),color=Forest)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf("Progress" to R.string.tab_progress,"Practice" to R.string.tab_practice,"Explorers" to R.string.tab_explorers,"Controls" to R.string.tab_controls,"Packs" to R.string.tab_packs,"Premium" to R.string.tab_premium).forEach { (name,label)->if(tab==name) Action(stringResource(label)) {tab=name} else SoftButton(stringResource(label)) {tab=name} }
        }
        if(children.isNotEmpty()) Choice(stringResource(R.string.ui_61ff351c30),children.map { it.nickname },selected?.nickname.orEmpty()) { n->children.find {it.nickname==n}?.let(onChild) }
        if(message.isNotEmpty()) Text(message,fontWeight=FontWeight.Bold)
        PackSelectionFeedback(packSelection.status)
        when(tab) {
            "Progress" -> {
                Heading(stringResource(R.string.family_week),true)
                Text(stringResource(R.string.family_week_help))
                val weekStart=LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                children.forEach { explorer->
                    val weekly=attempts.filter {it.childId==explorer.id && it.at>=weekStart && it.at<=System.currentTimeMillis()}
                    val finished=completions.count {it.childId==explorer.id && it.at>=weekStart && it.at<=System.currentTimeMillis()}
                    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFEAF0DE))) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {ExplorerAvatar(explorer,Modifier.padding(end=12.dp));Text(stringResource(R.string.family_summary,explorer.nickname,weekly.size,weekly.count {it.correct&&it.hints==0},finished),fontWeight=FontWeight.Bold)}
                            if(weekly.isEmpty()) Text(stringResource(R.string.family_no_practice))
                            else weekly.groupBy {it.skill}.minByOrNull {(_,items)->items.count {it.correct&&it.hints==0}.toDouble()/items.size}?.key?.let {Text(stringResource(R.string.next_practice,skillLabel(ctx,it)))}
                        }
                    }
                }
                val mine=attempts.filter {it.childId==selected?.id}
                val independent=mine.count {it.correct && it.hints==0}
                Heading(selected?.let { ctx.getString(R.string.ui_a4f7b9afd3, it.nickname) } ?: ctx.getString(R.string.ui_3c9c9f3f5a),true)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Metric(stringResource(R.string.ui_93aa0d4791),mine.size.toString()); Metric(stringResource(R.string.ui_4598e27117), ctx.getString(R.string.ui_6ddee0eb12, selected?.let {clock.used(it.id)/60_000} ?: 0))
                    Metric(stringResource(R.string.ui_3503a81e26),if(mine.isEmpty()) "—" else java.text.NumberFormat.getPercentInstance(displayLocale).format(mine.count {it.correct}.toDouble()/mine.size))
                    Metric(stringResource(R.string.ui_bc182863ac),independent.toString())
                    Metric(stringResource(R.string.ui_38677d4b0b), ctx.getString(R.string.ui_6ddee0eb12, selected?.let {clock.total(it.id)/60_000} ?: 0))
                }
                Text(stringResource(R.string.ui_4090e48ce1))
                Heading(stringResource(R.string.ui_8c4eef5ab2),true)
                val today=LocalDate.now()
                Row(Modifier.fillMaxWidth().height(130.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    (6 downTo 0).forEach { ago-> val day=today.minusDays(ago.toLong()); val count=mine.count { Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate()==day }
                        Column(Modifier.weight(1f)) { Spacer(Modifier.weight(1f)); Text(count.toString()); Box(Modifier.fillMaxWidth().height((count*8+4).coerceAtMost(90).dp).background(Leaf,RoundedCornerShape(8.dp))); Text(day.format(java.time.format.DateTimeFormatter.ofPattern("EEE",displayLocale)),fontSize=11.sp) }
                    }
                }
                Heading(stringResource(R.string.ui_eec91566a6),true)
                if(mine.isEmpty()) Text(stringResource(R.string.ui_2c5b14e58c))
                mine.groupBy {it.skill}.forEach { (skill,evidence)->
                    val m=LearningEngine.mastery(evidence.map {Evidence(it.correct,it.hints,it.difficulty)})
                    Row(Modifier.fillMaxWidth().background(Color(0xFFEAF0DE),RoundedCornerShape(14.dp)).padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text(skillLabel(ctx,skill),fontWeight=FontWeight.Bold); Text(m.displayLabel(ctx)) }; Text(stringResource(R.string.ui_a6f2fc5594 ,m.accuracy))
                    }
                }
                Action(stringResource(R.string.ui_c5b4cd9303),enabled=attempts.isNotEmpty() && !progressExportStatus.busy) {onExport()}
                ProgressExportFeedback(progressExportStatus)
                Text(stringResource(R.string.ui_0bdd03b627),fontSize=12.sp)
            }
            "Practice" -> if(!access.assignmentsAllowed) Text(stringResource(R.string.family_practice_locked)) else if(selected==null) Text(stringResource(R.string.ui_0f76020e56)) else key(selected.id) {
                var game by remember {mutableStateOf(Game.MATHS)}; var year by remember {mutableIntStateOf(selected.year)}
                var skill by remember {mutableStateOf("")}; var count by remember {mutableStateOf("5")}
                var difficulty by remember {mutableStateOf("1")}; var due by remember {mutableStateOf(LocalDate.now().plusDays(3).toString())}
                var repeat by remember {mutableStateOf("0")}; var target by remember {mutableStateOf("0")}
                Heading(stringResource(R.string.ui_65e0d4a7ba),true)
                Choice(stringResource(R.string.ui_3c92dbb8e3),Game.entries.filter {it.name in selected.subjects.split(',')}.map {it.displayTitle(ctx)},game.displayTitle(ctx)) { value->game=Game.entries.first {it.displayTitle(ctx)==value};skill="" }
                Choice(stringResource(R.string.ui_58f2367d9e),listOf(ctx.getString(R.string.ui_758d3c3f5d))+content.filter {it.game==game && it.year==year}.map {it.skill}.distinct(),skill.ifBlank {ctx.getString(R.string.ui_758d3c3f5d)},optionLabel={skillLabel(ctx,it)}) {skill=if(it==ctx.getString(R.string.ui_758d3c3f5d)) "" else it}
                Choice(stringResource(R.string.ui_c52ef48efd),(1..6).map {it.toString()},year.toString()) {year=it.toInt();skill=""}
                Choice(stringResource(R.string.ui_aa1c39de59),listOf("1","2","3"),difficulty) {difficulty=it}
                Choice(stringResource(R.string.ui_3a340cef93),listOf("3","5","10","15","20"),count) {count=it}
                OutlinedTextField(due,{due=it},label={Text(stringResource(R.string.ui_50b406fc25))},singleLine=true)
                Choice(stringResource(R.string.ui_c0e691f2d3),listOf("0","1","3","7"),repeat) {repeat=it}
                Choice(stringResource(R.string.ui_fe6f0d3042),listOf("0","2","3","5","10"),target) {target=it}
                Action(stringResource(R.string.ui_36f68334cb)) {
                    if(runCatching {LocalDate.parse(due)}.isFailure) message=ctx.getString(R.string.ui_7a2bae6fc3)
                    else if(game.name !in selected.subjects.split(',')) message=ctx.getString(R.string.ui_93f98a4440)
                    else {onAssign(Assignment(UUID.randomUUID().toString(),selected.id,game.name,skill,year,count.toInt(),difficulty.toInt(),due,repeat.toInt(),target.toInt()));message=ctx.getString(R.string.ui_6011c8ce77, selected.nickname)}
                }
                assignments.filter {it.childId==selected.id}.forEach { a->Text(stringResource(R.string.ui_7dd30e0364 ,Game.valueOf(a.game).displayTitle(ctx),a.count,a.due,if(a.completed==0) ctx.getString(R.string.status_waiting) else ctx.getString(R.string.ui_f186825f7b, a.completed))) }
            }
            "Explorers" -> {
                Heading(stringResource(R.string.ui_48c15eed99),true)
                Text(stringResource(R.string.ui_b76ba4436f))
                children.forEach { c->FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) { SoftButton(stringResource(R.string.ui_a3e8bd6c7a ,c.nickname,c.year)) {onChild(c)};SoftButton(stringResource(R.string.ui_675f2e9944 ,c.nickname)) {deleteId=c.id} } }
                var nickname by remember {mutableStateOf("")}; var year by remember {mutableStateOf("1")}; var avatar by remember {mutableStateOf("0")}
                OutlinedTextField(nickname,{nickname=it.take(20)},label={Text(stringResource(R.string.ui_35aca3e089))},singleLine=true)
                Choice(stringResource(R.string.ui_86bf181a19),(1..6).map {it.toString()},year) {year=it}
                Choice(stringResource(R.string.ui_9926b3d54f),avatarNames,avatarNames[avatar.toInt().coerceIn(0,3)]) {avatar=avatarNames.indexOf(it).toString()}
                Action(stringResource(R.string.ui_c4df01117c),enabled=children.size<access.profileLimit) {
                    val name=nickname.trim()
                    if(name.length<2) message=ctx.getString(R.string.ui_037f361256)
                    else if(children.any {it.nickname.equals(name,true)}) message=ctx.getString(R.string.ui_13e66bb2b6)
                    else { val c=Child(UUID.randomUUID().toString(),name,avatar.toInt(),year.toInt());onSave(c);onChild(c);nickname="";message=ctx.getString(R.string.ui_b72826b10a) }
                }
                Text(stringResource(R.string.family_profile_limit,access.profileLimit))
            }
            "Controls" -> if(selected==null) Text(stringResource(R.string.ui_0f76020e56)) else key(selected.id,selected) {
                var draft by remember {mutableStateOf(selected)}
                Heading(stringResource(R.string.ui_25ccf390c8),true)
                OutlinedTextField(draft.nickname,{draft=draft.copy(nickname=it.take(20))},label={Text(stringResource(R.string.edit_nickname))},singleLine=true)
                Choice(stringResource(R.string.ui_9926b3d54f),avatarNames,avatarNames[draft.avatar.coerceIn(0,3)]) {draft=draft.copy(avatar=avatarNames.indexOf(it))}
                Choice(stringResource(R.string.ui_0efc95f1c8),(1..6).map {it.toString()},draft.year.toString()) {draft=draft.copy(year=it.toInt())}
                Choice(stringResource(R.string.ui_50c886d4ba),listOf("5","10","15","20","30","45","60"),draft.allowanceMinutes.toString()) {draft=draft.copy(allowanceMinutes=it.toInt())}
                Choice(stringResource(R.string.ui_95291e0e6c),(0..23).map {it.toString()},draft.bedtimeHour.toString()) {draft=draft.copy(bedtimeHour=it.toInt())}
                Choice(stringResource(R.string.ui_fcd9bb72a0),(0..23).map {it.toString()},draft.wakeHour.toString()) {draft=draft.copy(wakeHour=it.toInt())}
                Choice(stringResource(R.string.ui_7e9f47749f),listOf("0","5","10","15","20"),draft.breakMinutes.toString()) {draft=draft.copy(breakMinutes=it.toInt())}
                Toggle(stringResource(R.string.ui_1a98557f98),draft.voice) {draft=draft.copy(voice=it)}
                Text(stringResource(R.string.voice_volume,draft.voiceVolume))
                Slider(value=draft.voiceVolume.toFloat(),onValueChange={draft=draft.copy(voiceVolume=it.toInt())},valueRange=0f..100f,steps=9)
                Text(stringResource(R.string.voice_volume_help),fontSize=13.sp)
                val preview=stringResource(R.string.voice_preview)
                Action(stringResource(R.string.preview_voice),enabled=speechAvailable&&draft.voiceVolume>0) {previewVoice(preview,draft.voiceVolume)}
                if(!speechAvailable) Text(stringResource(R.string.voice_unavailable))
                Toggle(stringResource(R.string.ui_d136b1ae55),draft.reducedMotion) {draft=draft.copy(reducedMotion=it)}
                Toggle(stringResource(R.string.ui_12cbc1d0d5),draft.highContrast) {draft=draft.copy(highContrast=it)}
                Toggle(stringResource(R.string.ui_9b2c87e05a),draft.switchingAllowed) {draft=draft.copy(switchingAllowed=it)}
                Text(stringResource(R.string.ui_22e02cedda),fontWeight=FontWeight.Bold)
                Game.entries.forEach {g->Toggle(g.displayTitle(ctx),g.name in draft.subjects.split(',')) {on-> val set=draft.subjects.split(',').filter {it.isNotBlank()}.toMutableSet();if(on) set.add(g.name) else set.remove(g.name);draft=draft.copy(subjects=set.joinToString(","))}}
                Action(stringResource(R.string.ui_640583cc0c)) {if(draft.nickname.trim().length<2 || children.any {it.id!=draft.id&&it.nickname.equals(draft.nickname.trim(),true)}) message=ctx.getString(R.string.ui_324242a143) else if(draft.subjects.isBlank()) message=ctx.getString(R.string.ui_0549e0a832) else if(draft.bedtimeHour==draft.wakeHour) message=ctx.getString(R.string.ui_652dc53e52) else {onSave(draft.copy(nickname=draft.nickname.trim()));message=ctx.getString(R.string.ui_bba95032cd)}}
                SoftButton(stringResource(R.string.ui_fb217d72ec)) {clock.bonus(selected.id);message=ctx.getString(R.string.ui_92d54da971)}
                SoftButton(stringResource(R.string.ui_1873a4ea29),onClick=onChangePin)
                Text(stringResource(R.string.ui_8ae41ee339),fontSize=12.sp)
                Heading(stringResource(R.string.ui_186352a881),true); Text(stringResource(R.string.ui_2919dde6e0))
            }
            "Packs" -> {
                if(packControlsBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Heading(stringResource(R.string.ui_c476b07dee),true)
                Text(stringResource(R.string.ui_95b7cc2a3e ,content.count {it.id.startsWith("dev-")}))
                Text(stringResource(R.string.ui_ca25384cf7 ,content.count {!it.id.startsWith("dev-")}))
                Text(stringResource(R.string.ui_ef8b86ab1d))
                Action(stringResource(R.string.ui_e4f4c04d74),enabled=!packControlsBusy,onClick=onChoosePack)
                Action(stringResource(R.string.ui_853c2678a2),enabled=!packControlsBusy) {
                    packBusy=true
                    scope.launch {
                        val result=withContext(Dispatchers.IO) {runCatching {packs.deleteOptional()}}
                        result.onSuccess {onPacks();message=ctx.getString(R.string.ui_55ed3295c3)}.onFailure {message=ctx.getString(R.string.ui_25cf6a0e34, it.message)}
                        packBusy=false
                    }
                }
                if(pendingPack!=null) {
                    Text(stringResource(R.string.ui_bb2b29d8b1))
                    OutlinedTextField(digest,{digest=it.trim().lowercase()},label={Text(stringResource(R.string.ui_b038fb0d0e))},modifier=Modifier.fillMaxWidth())
                    Action(stringResource(R.string.ui_27d773b934),enabled=!packControlsBusy) {
                        val bytes=pendingPack ?: return@Action;val expected=digest;packBusy=true
                        scope.launch {
                            val result=withContext(Dispatchers.IO) {runCatching {packs.import(bytes,expected)}}
                            result.onSuccess {message=ctx.getString(R.string.ui_cf0346f55a, it);onClearPack();digest="";onPacks()}.onFailure {message=ctx.getString(R.string.ui_a2d5380b75, it.message)}
                            packBusy=false
                        }
                    }
                    if(!packControlsBusy) SoftButton(stringResource(R.string.ui_6dd6cafb0d)) {onClearPack();digest=""}
                }
            }
            "Premium" -> FamilyPurchases(purchaseState,access,purchases)
        }
        Text(stringResource(R.string.ui_14a3518780),fontSize=12.sp)
    }
    if(deleteId!=null) AlertDialog(onDismissRequest={deleteId=null},title={Text(stringResource(R.string.ui_7a30e2bc21))},text={Text(stringResource(R.string.ui_d36e13a461))},confirmButton={Action(stringResource(R.string.ui_e39fd420a5)) {deleteId?.let(onDelete);deleteId=null}},dismissButton={SoftButton(stringResource(R.string.ui_987316a350)) {deleteId=null}})
}
@Composable fun Metric(label:String,value:String) { Column(Modifier.widthIn(min=140.dp).background(Color(0xFFE6EDD7),RoundedCornerShape(18.dp)).padding(18.dp)) {Text(value,fontSize=30.sp,fontWeight=FontWeight.ExtraBold);Text(label)} }
@Composable fun Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(label,Modifier.weight(1f).padding(vertical=14.dp));Switch(value,onChange)} }
@Composable internal fun ProgressExportFeedback(status:ProgressExportStatus) {
    val message=when(status) {
        ProgressExportStatus.IDLE->null
        ProgressExportStatus.CHOOSING->R.string.progress_export_choosing
        ProgressExportStatus.WRITING->R.string.progress_export_saving
        ProgressExportStatus.SAVED->R.string.progress_export_saved
        ProgressExportStatus.CANCELLED->R.string.progress_export_cancelled
        ProgressExportStatus.FAILED->R.string.progress_export_failed
    }
    if(message!=null) Text(stringResource(message),modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
}

@Composable internal fun PackSelectionFeedback(status:PackSelectionStatus) {
    val message=when(status) {
        PackSelectionStatus.IDLE->null
        PackSelectionStatus.CHOOSING->R.string.pack_file_choosing
        PackSelectionStatus.READING->R.string.pack_file_reading
        PackSelectionStatus.READY->R.string.pack_file_ready
        PackSelectionStatus.CANCELLED->R.string.pack_file_cancelled
        PackSelectionStatus.FAILED->R.string.pack_file_failed
    }
    if(message!=null) Text(stringResource(message),modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
}
