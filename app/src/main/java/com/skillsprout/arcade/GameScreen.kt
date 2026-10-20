package com.skillsprout.arcade
import androidx.compose.ui.res.stringResource

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.launch

@Composable fun GameScreen(child:Child,game:Game,content:List<Activity>,attempts:List<Attempt>,assignment:Assignment?,
    speak:(String)->Unit,silence:()->Unit,paused:Boolean,onPause:()->Unit,onHome:()->Unit,onAttempt:(Attempt)->Unit,onComplete:suspend (String)->Unit) {
    val scope=rememberCoroutineScope()
    var finishing by remember { mutableStateOf(false) }
    var finishError by remember { mutableStateOf(false) }
    val ctx=LocalContext.current
    val prefs=ctx.getSharedPreferences("sessions",0)
    val sessionKey="${child.id}-${game.name}-${assignment?.id.orEmpty()}"
    val initial=remember(sessionKey) { runCatching { JSONObject(prefs.getString(sessionKey,"{}")!!) }.getOrDefault(JSONObject()) }
    var runId by rememberSaveable(sessionKey) { mutableStateOf(initial.optString("runId",UUID.randomUUID().toString())) }
    var completed by rememberSaveable(sessionKey) { mutableIntStateOf(initial.optInt("completed",0)) }
    var currentId by rememberSaveable(sessionKey) { mutableStateOf(initial.optString("activity", "")) }
    var seenText by rememberSaveable(sessionKey) { mutableStateOf(initial.optString("seen","")) }
    val total=assignment?.count ?: 5
    var finished by rememberSaveable(sessionKey) { mutableStateOf(false) }
    val history=attempts.groupBy { it.skill }.mapValues { (_,list)->list.map { Evidence(it.correct,it.hints,it.difficulty) } }
    val pool=content.filter { it.game==game && (assignment?.skill.isNullOrBlank() || it.skill==assignment?.skill) }
    LaunchedEffect(currentId,runId) {
        if(currentId.isBlank() || pool.none { it.id==currentId }) currentId=LearningEngine.select(pool,assignment?.year ?: child.year,history,
            seenText.split(',').filter { it.isNotEmpty() },assignment?.difficulty)?.id.orEmpty()
    }
    val activity=pool.find { it.id==currentId }
    LaunchedEffect(currentId,completed,seenText) {
        if(!finished) prefs.edit().putString(sessionKey,JSONObject().put("runId",runId).put("completed",completed).put("activity",currentId).put("seen",seenText).toString()).commit()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(game.title,fontSize=21.sp,lineHeight=25.sp,fontWeight=FontWeight.ExtraBold)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            SoftButton(stringResource(R.string.ui_642926ccf6),Modifier.width(112.dp)) { silence(); onHome() }
            SoftButton(stringResource(R.string.pause),Modifier.widthIn(min=96.dp)) { onPause() }
            }
        }
        if(finished) {
            Row(verticalAlignment=Alignment.CenterVertically) { Pip(Modifier.height(230.dp).width(200.dp)); Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Heading(stringResource(R.string.done)); Text(stringResource(R.string.ui_bc84820192 ,total),fontSize=20.sp)
                Text(stringResource(R.string.ui_4e4fd5370f))
                Action(stringResource(R.string.ui_71007eedfb)) { onHome() }
            } }; return@Column
        }
        if(activity==null) { Text(stringResource(R.string.ui_f64df908fb)); return@Column }
        key(activity.id,runId) {
            var picked by rememberSaveable { mutableStateOf("") }
            var tileIndices by rememberSaveable { mutableStateOf("") }
            var hintCount by rememberSaveable { mutableIntStateOf(0) }
            var feedback by rememberSaveable { mutableStateOf("") }
            var correct by rememberSaveable { mutableStateOf(false) }
            var submitted by rememberSaveable { mutableStateOf(attempts.any { it.id=="$runId-$completed-${activity.id}" }) }
            var groups by rememberSaveable { mutableIntStateOf(0) }
            val started=remember { SystemClock.elapsedRealtime() }
            val options=remember { activity.choices.withIndex().sortedBy { (i,s)-> (activity.id+s+i).hashCode() }.map { it.value } }
            val indices=tileIndices.split(',').mapNotNull { it.toIntOrNull() }
            val assembled=indices.joinToString(if(activity.type=="sequence") " " else "") { options[it] }
            LaunchedEffect(activity.id) { if(child.voice && !paused) speak(if(game==Game.SPELLING) activity.answer else activity.prompt) }
            LinearProgressIndicator(progress={completed.toFloat()/total},modifier=Modifier.fillMaxWidth(),color=Leaf,trackColor=Color(0xFFDFE7D4))
            Text(stringResource(R.string.ui_2fe2eadb1c ,completed+1,total,activity.skill.replace('-',' ')),color=Forest)
            if(game==Game.MATHS || game==Game.TABLES) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp).background(GameColors[game.ordinal],RoundedCornerShape(24.dp))) {
                    Image(painterResource(R.drawable.sproutland),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alpha=0.7f)
                    val target=if(correct) 0.78f else 0.08f+completed.toFloat()/total*0.4f
                    val position by animateFloatAsState(target,animationSpec=tween(if(child.reducedMotion) 0 else 700),label=ctx.getString(R.string.ui_3f0c87faa4))
                    Pip(Modifier.offset(x=(maxWidth-100.dp)*position).align(Alignment.BottomStart).size(100.dp))
                    Text(if(game==Game.MATHS) ctx.getString(R.string.ui_4261887e3b) else ctx.getString(R.string.ui_1341c4dbd2),Modifier.align(Alignment.TopStart).padding(12.dp).background(Cream,RoundedCornerShape(8.dp)).padding(6.dp),fontWeight=FontWeight.Bold)
                }
            }
            if(activity.story.isNotBlank()) Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFE4F0EF))) { Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Heading(stringResource(R.string.ui_4c4fae9eee),true); Text(activity.story,fontSize=21.sp,lineHeight=32.sp)
                if(child.voice) SoftButton(stringResource(R.string.ui_f6dd46fb00)) { speak(activity.story) }
            } }
            Heading(activity.prompt)
            if(game==Game.TABLES) {
                val array=activity.visual.split(':'); val n=array.getOrNull(1)?.toIntOrNull() ?: 2; val each=array.getOrNull(2)?.toIntOrNull() ?: 2
                Text(stringResource(R.string.ui_866390a4b7))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Action(stringResource(R.string.ui_2466db329c),enabled=groups<n && !correct) { groups++ }
                    SoftButton(stringResource(R.string.ui_1de18d6b06)) { groups=0 }
                }
                Text(pluralStringResource(R.plurals.groups_count,groups,groups,each),fontWeight=FontWeight.Bold)
                Column(Modifier.fillMaxWidth().semantics { contentDescription=ctx.getString(R.string.ui_46b8d31730, groups,each) }) {
                    repeat(groups) { Text(List(each){"●"}.joinToString("  "),color=Forest,fontSize=18.sp,modifier=Modifier.padding(2.dp)) }
                }
            }
            if(activity.type=="letters" || activity.type=="sequence") {
                Text(if(activity.type=="letters") ctx.getString(R.string.ui_50d5ce7598) else ctx.getString(R.string.ui_a19dd62a9f))
                Text(assembled.ifEmpty { ctx.getString(R.string.ui_347fb42965) },fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(18.dp)).padding(22.dp).semantics { liveRegion=LiveRegionMode.Polite; contentDescription=ctx.getString(R.string.ui_25dbfb26f8, assembled) })
                FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    options.forEachIndexed { i,tile -> Button(onClick={tileIndices=(indices+i).joinToString(",")},enabled=i !in indices && !correct,modifier=Modifier.sizeIn(minWidth=56.dp,minHeight=56.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=GameColors[game.ordinal],contentColor=Forest)) { Text(tile,fontSize=23.sp,fontWeight=FontWeight.Bold) } }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { SoftButton(stringResource(R.string.ui_9abd215902)) { tileIndices=indices.dropLast(1).joinToString(",") }; SoftButton(stringResource(R.string.ui_962e64b761)) {tileIndices=""} }
            } else if(activity.type=="word") {
                Text(activity.prompt.replace("___",picked.ifEmpty { "______" }),fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=12.dp))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { options.forEach { option->Action(if(picked==option) ctx.getString(R.string.ui_5a122987ac, option) else option,enabled=!correct) { picked=option } } }
            } else {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    options.forEach { option->OutlinedButton(onClick={picked=option},enabled=!correct,modifier=Modifier.heightIn(min=68.dp).widthIn(min=145.dp),shape=RoundedCornerShape(20.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(picked==option) GameColors[game.ordinal] else Color.White)) {
                        Text(if(picked==option) ctx.getString(R.string.ui_5a122987ac, option) else option,fontSize=23.sp,fontWeight=FontWeight.Bold,color=Forest)
                    } }
                }
            }
            if(feedback.isNotBlank()) Card(colors=CardDefaults.cardColors(containerColor=if(correct) Color(0xFFDFECCD) else Color(0xFFFFE8C3))) {
                Text(feedback,Modifier.padding(20.dp).semantics { liveRegion=LiveRegionMode.Polite },fontSize=19.sp,lineHeight=28.sp)
            }
            if(hintCount>0 && !correct) Text(activity.hints[(hintCount-1).coerceAtMost(activity.hints.lastIndex)],Modifier.fillMaxWidth().background(Color(0xFFEAF0DB),RoundedCornerShape(14.dp)).padding(16.dp),fontSize=18.sp)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(!correct) {
                    Action(stringResource(R.string.ui_4d24a9c147),enabled=(if(activity.type in listOf("letters","sequence")) indices.size==options.size else picked.isNotBlank()) && !paused) {
                        val answer=if(activity.type in listOf("letters","sequence")) assembled else picked
                        correct=answer.equals(activity.answer,true)
                        feedback=if(correct) ctx.getString(R.string.ui_82949e30a3, activity.explanation) else ctx.getString(R.string.ui_046a7ded41, activity.explanation)
                        if(!submitted) { submitted=true; onAttempt(Attempt("$runId-$completed-${activity.id}",child.id,activity.id,activity.skill,game.name,correct,hintCount,activity.difficulty,(SystemClock.elapsedRealtime()-started).coerceAtMost(30*60_000),System.currentTimeMillis(),assignment?.id)) }
                        if(child.voice) speak(if(correct) ctx.getString(R.string.ui_f1166d2bfe) else ctx.getString(R.string.ui_d6cf296f1a))
                    }
                    SoftButton(stringResource(R.string.hint)) { hintCount++; if(child.voice) speak(activity.hints[(hintCount-1).coerceAtMost(activity.hints.lastIndex)]) }
                } else Action(if(finishing) stringResource(R.string.status_saving) else if(completed+1>=total) ctx.getString(R.string.ui_605892c227) else stringResource(R.string.next),enabled=!finishing) {
                    if(completed+1>=total) {
                        finishing=true
                        scope.launch {
                            try { onComplete(runId); prefs.edit().remove(sessionKey).commit(); finished=true; silence() }
                            catch(error:Exception) { if(error is kotlinx.coroutines.CancellationException) throw error; finishError=true }
                            finally { finishing=false }
                        }
                    }
                    else { completed++; seenText=(seenText.split(',').filter {it.isNotEmpty()}+activity.id).joinToString(","); currentId="" }
                }
                if(child.voice) SoftButton(stringResource(R.string.repeat)) { speak(if(game==Game.SPELLING) activity.answer else activity.prompt) }
            }
            if(finishError) Text(stringResource(R.string.ui_ba43b0d5ff),Modifier.semantics { liveRegion=LiveRegionMode.Polite })
            Text(stringResource(R.string.ui_43acd0c6bf),fontSize=13.sp)
        }
    }
}
