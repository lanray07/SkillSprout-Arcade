package com.skillsprout.arcade

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import android.util.AtomicFile
import android.util.Base64

class ContentPacks(private val context: Context,private val publisherKey:PublicKey?=null) {
    private val versions=context.getSharedPreferences("pack_versions",Context.MODE_PRIVATE)
    private data class SignedPack(val id:String,val version:Int,val activities:List<Activity>)
    private fun signed(bytes:ByteArray):SignedPack {
        require(bytes.size<=7_000_000) {"Pack is too large"}
        val envelope=JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getInt("format")==1) {"Unsupported signed pack"}
        val m=envelope.getJSONObject("manifest")
        val id=m.getString("packId");val version=m.getInt("version");val locale=m.getString("locale");val digest=m.getString("sha256")
        require(id.matches(Regex("[a-z0-9][a-z0-9-]{0,63}")) && version>0 && locale=="en-GB" && digest.matches(Regex("[a-f0-9]{64}"))) {"Invalid pack manifest"}
        val key=publisherKey ?: KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(context.assets.open("content-publisher.der").use {it.readBytes()}))
        val message="SkillSprout-Pack-v1\n$id\n$version\n$locale\n$digest".toByteArray(Charsets.UTF_8)
        require(Signature.getInstance("SHA256withECDSA").run {initVerify(key);update(message);verify(Base64.decode(envelope.getString("signature"),Base64.NO_WRAP))}) {"Publisher signature is invalid"}
        val payload=Base64.decode(envelope.getString("payload"),Base64.NO_WRAP)
        require(sha(payload)==digest) {"Pack integrity mismatch"}
        val activities=parse(payload)
        require(activities.all {it.id.startsWith("$id:")}) {"Activity IDs must belong to their pack"}
        return SignedPack(id,version,activities)
    }
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
    @Synchronized fun installed(): List<Activity> {
        val result=mutableListOf<Activity>();val ids=starter().map {it.id}.toMutableSet()
        context.filesDir.resolve("packs").listFiles()?.map {if(it.name.endsWith(".sproutpack.bak")) File(it.path.removeSuffix(".bak")) else it}
            ?.filter {it.extension in setOf("json","sproutpack")}?.distinctBy {it.name}?.sortedBy {it.name}?.forEach {file->
            runCatching {
                val bytes=if(file.extension=="sproutpack") AtomicFile(file).openRead().use {it.readBytes()} else file.readBytes()
                val pack=if(file.extension=="sproutpack") signed(bytes).let {require(it.version>=versions.getInt(it.id,0));it.activities}
                    else {require(BuildConfig.DEVELOPMENT_CONTENT);require(sha(bytes)==File(file.path+".sha256").readText());parse(bytes)}
                require(pack.none {it.id in ids}) {"Activity IDs conflict with an installed pack"}
                result+=pack;ids+=pack.map {it.id}
            }
        }
        return result
    }
    @Synchronized fun importSigned(bytes:ByteArray):Int {
        val pack=signed(bytes)
        val dir=context.filesDir.resolve("packs").apply {mkdirs()};val file=File(dir,"${pack.id}.sproutpack")
        val current=runCatching {signed(AtomicFile(file).openRead().use {it.readBytes()}).version}.getOrDefault(0)
        require(pack.version>maxOf(current,versions.getInt(pack.id,0))) {"Pack version must be newer; rollback is blocked"}
        val previousIds=runCatching {signed(AtomicFile(file).openRead().use {it.readBytes()}).activities.map {it.id}.toSet()}.getOrDefault(emptySet())
        val existing=(starter()+installed()).filter {it.id !in previousIds}.map {it.id}.toSet()
        require(pack.activities.none {it.id in existing}) {"Pack IDs conflict with existing content"}
        val atomic=AtomicFile(file);val stream=atomic.startWrite()
        try {stream.write(bytes);atomic.finishWrite(stream)} catch(error:Exception) {atomic.failWrite(stream);throw error}
        check(versions.edit().putInt(pack.id,pack.version).commit()) {"Could not save pack version"}
        return pack.activities.size
    }
    @Synchronized fun import(bytes: ByteArray, expectedSha: String): Int {
        require(bytes.size<=7_000_000) {"Pack is too large"}
        if(runCatching {JSONObject(bytes.toString(Charsets.UTF_8)).has("manifest")}.getOrDefault(false)) return importSigned(bytes)
        require(BuildConfig.DEVELOPMENT_CONTENT) {"Release packs require a publisher signature"}
        require(sha(bytes) == expectedSha) { "Integrity mismatch" }; val pack = parse(bytes)
        require(pack.none { incoming -> starter().any { it.id==incoming.id } }) { "Pack IDs conflict with starter pack" }
        require(pack.none {incoming->installed().any {it.id==incoming.id}}) {"Pack IDs conflict with installed content"}
        val dir = context.filesDir.resolve("packs").apply { mkdirs() }
        val target = File(dir, "${sha(bytes)}.json")
        File(target.path+".tmp").apply { writeBytes(bytes); require(renameTo(target)) }
        File(target.path+".sha256").writeText(expectedSha)
        return pack.size
    }
    @Synchronized fun deleteOptional() { context.filesDir.resolve("packs").listFiles()?.forEach {check(it.delete()) {"Could not remove optional pack"}} }
    companion object { fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } }
}
