package com.skillsprout.arcade

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class ContentPacks(private val context: Context) {
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    fun parse(bytes: ByteArray): List<Activity> {
        require(bytes.size <= 5_000_000) { "Pack is too large" }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.getInt("schemaVersion") == 1 && root.getString("locale") == "en-GB") { "Unsupported pack" }
        val array = root.getJSONArray("activities")
        require(array.length() in 1..10000)
        val activities = (0 until array.length()).map { i -> val a = array.getJSONObject(i)
            Activity(a.getString("id"),Game.valueOf(a.getString("game")),a.getString("subject"),a.getString("skill"),a.getInt("year"),
                a.getString("objective"),a.getString("reference"),a.getInt("difficulty"),a.getJSONArray("prerequisites").strings(),
                a.getString("type"),a.getString("prompt"),a.getJSONArray("choices").strings(),a.getString("answer"),a.getString("explanation"),
                a.getJSONArray("hints").strings(),a.optString("story"),a.optString("visual"),a.getString("reviewStatus"),a.getInt("version")) }
        require(activities.map { it.id }.distinct().size == activities.size) { "Duplicate activity ID" }
        activities.forEach { require(LearningEngine.validate(it).isEmpty()) { "Invalid activity ${it.id}" }
            require(it.reviewStatus !in setOf("Retired","Rejected","Draft"))
            require(BuildConfig.DEVELOPMENT_CONTENT || it.reviewStatus == "Approved") { "Unreviewed release content" } }
        return activities
    }
    fun starter(): List<Activity> {
        val bytes = context.assets.open("packs/starter.json").readBytes()
        val manifest = JSONObject(context.assets.open("packs/manifest.json").bufferedReader().readText())
        require(sha(bytes) == manifest.getString("sha256")) { "Starter pack integrity check failed" }
        return parse(bytes)
    }
    fun installed(): List<Activity> = context.filesDir.resolve("packs").listFiles()?.filter { it.extension == "json" }?.flatMap { f ->
        runCatching { val data=f.readBytes(); require(sha(data)==File(f.path+".sha256").readText()); parse(data) }.getOrDefault(emptyList())
    }.orEmpty()
    fun import(bytes: ByteArray, expectedSha: String): Int {
        require(sha(bytes) == expectedSha) { "Integrity mismatch" }; val pack = parse(bytes)
        require(pack.none { incoming -> starter().any { it.id==incoming.id } }) { "Pack IDs conflict with starter pack" }
        val dir = context.filesDir.resolve("packs").apply { mkdirs() }
        val target = File(dir, "${sha(bytes)}.json")
        File(target.path+".tmp").apply { writeBytes(bytes); require(renameTo(target)) }
        File(target.path+".sha256").writeText(expectedSha)
        return pack.size
    }
    fun deleteOptional() { context.filesDir.resolve("packs").listFiles()?.forEach { it.delete() } }
    companion object { fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } }
}
