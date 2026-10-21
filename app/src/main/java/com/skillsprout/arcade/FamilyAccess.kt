package com.skillsprout.arcade

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FamilyOffer(val sku:String,val title:String,val price:String,val description:String="",val subscriptionPeriod:String="")
data class PurchaseState(val configured:Boolean=false,val offers:List<FamilyOffer> = emptyList(),val verifiedUntil:Long=0,val busy:Boolean=false,val status:PurchaseStatus=PurchaseStatus.UNAVAILABLE)
enum class PurchaseStatus { UNAVAILABLE, LOADING, READY, VERIFIED, PENDING }
interface PurchaseGateway {
    val state:StateFlow<PurchaseState>
    fun resume()
    fun purchase(sku:String)
    fun restore()
    fun checkAccess()
    fun close()
}
class UnavailablePurchases:PurchaseGateway {
    override val state=MutableStateFlow(PurchaseState())
    override fun resume() {} ; override fun purchase(sku:String) {} ; override fun restore() {}
    override fun checkAccess() {} ; override fun close() {}
}
/** Access is recomputed from verified leases; profiles and progress are never deleted on expiry. */
class FamilyAccess(val development:Boolean,private val verifiedUntil:Long,private val now:Long,private val primaryId:String?=null) {
    val premium get()=development || now<verifiedUntil
    val profileLimit get()=if(premium) 4 else 1
    val assignmentsAllowed get()=premium
    fun canUseProfile(id:String,children:List<Child>)=premium || (primaryId ?: children.firstOrNull()?.id)==id
    fun activities(content:List<Activity>,bundledIds:Set<String>):List<Activity> = if(premium) content else content
        .filter {it.id in bundledIds}
        .groupBy {it.year to it.game}.values.flatMap {items->items.sortedWith(compareBy<Activity>{it.difficulty}.thenBy {it.id}).take(3)}
}
