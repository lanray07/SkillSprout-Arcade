package com.skillsprout.arcade

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class GameCheckpoint(val runId:String=UUID.randomUUID().toString(),val completed:Int=0,
    val activity:String="",val seen:List<String> = emptyList())

/** Recovery metadata only; attempts and mission completions remain in Room. */
internal object GameCheckpoints {
    fun scope(child:Child,game:Game,pool:List<Activity>,assignment:Assignment?):String {
        val activities=JSONArray()
        pool.sortedBy {it.id}.forEach {a->activities.put(JSONArray(listOf(a.id,a.game.name,a.subject,a.skill,a.year,
            a.objective,a.reference,a.difficulty,JSONArray(a.prerequisites),a.type,a.prompt,JSONArray(a.choices),
            a.answer,a.explanation,JSONArray(a.hints),a.story,a.visual,a.reviewStatus,a.version)))}
        return ContentPacks.sha(JSONArray(listOf(child.id,child.year,game.name,assignment?.id.orEmpty(),
            assignment?.year ?: child.year,assignment?.skill.orEmpty(),assignment?.difficulty ?: 0,
            assignment?.count ?: 5,activities)).toString().toByteArray(Charsets.UTF_8))
    }
    fun restore(raw:String?,scope:String,total:Int,allowed:Set<String>):GameCheckpoint {
        if(raw==null || raw.length>131_072) return GameCheckpoint()
        return runCatching {
            val json=JSONObject(raw)
            require(json.get("format")==1 && json.get("scope")==scope)
            val runId=json.get("runId") as String
            require(UUID.fromString(runId).toString()==runId)
            val completed=json.get("completed") as Int
            require(total>0 && completed in 0 until total)
            val activity=json.get("activity") as String
            require(activity.isEmpty() || activity in allowed)
            val rows=json.getJSONArray("seen")
            require(rows.length()==completed)
            val seen=(0 until rows.length()).map {rows.get(it) as String}
            require(seen.all {it in allowed})
            GameCheckpoint(runId,completed,activity,seen)
        }.getOrElse {GameCheckpoint()}
    }
    fun encode(scope:String,state:GameCheckpoint):String = JSONObject().put("format",1).put("scope",scope)
        .put("runId",state.runId).put("completed",state.completed).put("activity",state.activity)
        .put("seen",JSONArray(state.seen)).toString()

    fun tileIndices(raw:String,count:Int):List<Int> {
        if(raw.isEmpty()) return emptyList()
        val parts=raw.split(',')
        if(parts.size>count) return emptyList()
        val indices=parts.map {it.toIntOrNull() ?: return emptyList()}
        return if(indices.all {it in 0 until count} && indices.distinct().size==indices.size) indices else emptyList()
    }
}
