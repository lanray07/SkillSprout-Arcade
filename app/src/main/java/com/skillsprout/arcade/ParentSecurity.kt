package com.skillsprout.arcade

import android.content.Context
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import java.time.LocalDate
import java.time.LocalTime

interface PinVault {
    val configured:Boolean
    fun setPin(pin:String)
    fun verify(pin:String):Boolean
    fun secondsLocked():Long
}
class ParentSecurity(private val context: Context):PinVault {
    private val prefs = context.getSharedPreferences("parent_secure", Context.MODE_PRIVATE)
    private val bootCount get()=android.provider.Settings.Global.getInt(context.contentResolver,android.provider.Settings.Global.BOOT_COUNT,0)
    override val configured get() = prefs.contains("credential")
    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("parent-pin",null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("parent-pin",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun b64(b: ByteArray)=Base64.encodeToString(b,Base64.NO_WRAP)
    private fun decode(s:String)=Base64.decode(s,Base64.NO_WRAP)
    private fun hash(pin: String,salt:ByteArray):ByteArray {
        val spec=PBEKeySpec(pin.toCharArray(),salt,120_000,256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    override fun setPin(pin:String) {
        require(pin.matches(Regex("[0-9]{6,12}")))
        val salt=ByteArray(32).also { SecureRandom().nextBytes(it) }
        val clear="${b64(salt)}:${b64(hash(pin,salt))}".toByteArray()
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        check(prefs.edit().putString("credential","${b64(cipher.iv)}:${b64(cipher.doFinal(clear))}").commit())
    }
    override fun secondsLocked():Long {
        val remaining=if(prefs.getInt("lockBoot",-1)==bootCount) prefs.getLong("lockElapsed",0)-SystemClock.elapsedRealtime()
            else (prefs.getLong("lockWall",0)-System.currentTimeMillis()).coerceAtMost(60_000)
        return (remaining.coerceAtLeast(0)+999)/1000
    }
    override fun verify(pin:String):Boolean {
        if(secondsLocked()>0) return false
        val ok=runCatching {
            val parts=prefs.getString("credential","")!!.split(':')
            val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,decode(parts[0]))) }
            val clear=String(cipher.doFinal(decode(parts[1]))).split(':')
            MessageDigest.isEqual(hash(pin,decode(clear[0])),decode(clear[1]))
        }.getOrDefault(false)
        val failures=if(ok) 0 else prefs.getInt("failures",0)+1
        val editor=prefs.edit().putInt("failures",failures)
        if(!ok && failures>=5) editor.putLong("lockElapsed",SystemClock.elapsedRealtime()+60_000).putLong("lockWall",System.currentTimeMillis()+60_000).putInt("lockBoot",bootCount)
        if(ok) editor.remove("lockElapsed").remove("lockWall")
        editor.commit(); return ok
    }
}

class PlayClock(context:Context,private val elapsedNow:()->Long=SystemClock::elapsedRealtime,
    private val dayNow:()->LocalDate=LocalDate::now,private val hourNow:()->Int={LocalTime.now().hour}) {
    private val prefs=context.getSharedPreferences("play_clock",Context.MODE_PRIVATE)
    private var lastTick=elapsedNow()
    fun resume() { lastTick=elapsedNow() }
    private fun day(id:String):String {
        val saved=prefs.getString("$id-day",dayNow().toString())!!
        val today=dayNow().toString()
        // A backwards wall-clock change cannot grant a fresh allowance.
        if(today>saved) { prefs.edit().putString("$id-day",today).putLong("$id-used",0).remove("$id-bonus").remove("$id-break-used").commit(); return today }
        if(!prefs.contains("$id-day")) prefs.edit().putString("$id-day",saved).commit()
        return saved
    }
    fun used(id:String):Long { day(id); return prefs.getLong("$id-used",0) }
    fun tick(id:String) {
        val now=elapsedNow(); val delta=(now-lastTick).coerceIn(0,5000); lastTick=now
        day(id)
        prefs.edit().putLong("$id-used",prefs.getLong("$id-used",0)+delta)
            .putLong("$id-total",total(id)+delta).putLong("$id-break-used",prefs.getLong("$id-break-used",0)+delta).commit()
    }
    fun breakUsed(id:String):Long {day(id);return prefs.getLong("$id-break-used",0)}
    fun breakDue(c:Child):Boolean = c.breakMinutes>0 && breakUsed(c.id)>=c.breakMinutes*60_000L
    fun resetBreak(id:String) {day(id);prefs.edit().putLong("$id-break-used",0).commit();resume()}
    fun total(id:String):Long=prefs.getLong("$id-total",0)
    fun bonus(id:String) { day(id); prefs.edit().putLong("$id-bonus",prefs.getLong("$id-bonus",0)+300_000).commit() }
    fun blocked(c:Child):Boolean {
        if(used(c.id)>=c.allowanceMinutes*60_000L+prefs.getLong("${c.id}-bonus",0)) return true
        val hour=hourNow()
        return if(c.bedtimeHour>c.wakeHour) hour>=c.bedtimeHour || hour<c.wakeHour else hour>=c.bedtimeHour && hour<c.wakeHour
    }
    fun clear(id:String) { prefs.edit().remove("$id-day").remove("$id-used").remove("$id-bonus").remove("$id-total").remove("$id-break-used").commit() }
}
