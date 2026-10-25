package com.skillsprout.arcade

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun acceptsPinInput(value:String)=value.length<=12 && value.all {it in '0'..'9'}
internal fun validParentPin(value:String)=value.length in 6..12 && acceptsPinInput(value)
internal enum class PinSubmissionStatus { MATCH,NO_MATCH,INVALID,LOCKED,UNAVAILABLE }
internal data class PinSubmissionResult(val status:PinSubmissionStatus,val secondsLocked:Long=0)

/** Input values are snapshots. No credential or exception text is retained in the result. */
internal suspend fun submitParentPin(vault:PinVault,setup:Boolean,pin:String,confirmation:String=""):PinSubmissionResult {
    if(!validParentPin(pin) || (setup && pin!=confirmation)) return PinSubmissionResult(PinSubmissionStatus.INVALID)
    return withContext(Dispatchers.IO) {
        try {
            if(setup) {vault.setPin(pin);PinSubmissionResult(PinSubmissionStatus.MATCH)}
            else if(vault.verify(pin)) PinSubmissionResult(PinSubmissionStatus.MATCH)
            else {
                val remaining=vault.secondsLocked()
                if(remaining>0) PinSubmissionResult(PinSubmissionStatus.LOCKED,remaining)
                else PinSubmissionResult(PinSubmissionStatus.NO_MATCH)
            }
        } catch(cancelled:CancellationException) {throw cancelled}
        catch(_:Exception) {PinSubmissionResult(PinSubmissionStatus.UNAVAILABLE)}
    }
}
