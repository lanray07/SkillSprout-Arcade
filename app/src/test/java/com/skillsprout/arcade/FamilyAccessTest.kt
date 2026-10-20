package com.skillsprout.arcade
import org.junit.Assert.*
import org.junit.Test

class FamilyAccessTest {
    private val children=listOf(Child("a","Ada"),Child("b","Pip"))
    private fun item(id:String,game:Game,year:Int,difficulty:Int)=Activity(id,game,"English","skill",year,"Objective","Reference",difficulty,emptyList(),"choice","Question",listOf("Yes","No"),"Yes","Explanation",listOf("Hint"))
    @Test fun exactExpiryReturnsToFreeWithoutChangingStoredProfiles() {
        assertTrue(FamilyAccess(false,1001,1000).premium)
        val free=FamilyAccess(false,1000,1000)
        assertFalse(free.premium);assertEquals(1,free.profileLimit);assertFalse(free.assignmentsAllowed)
        assertTrue(free.canUseProfile("a",children));assertFalse(free.canUseProfile("b",children));assertEquals(2,children.size)
    }
    @Test fun primaryExplorerDoesNotChangeWhenNicknamesSortDifferently() {
        val free=FamilyAccess(false,0,1000,"a")
        assertTrue(free.canUseProfile("a",children.reversed()));assertFalse(free.canUseProfile("b",children.reversed()))
    }
    @Test fun freeLibraryIsStableBoundedAndExcludesOptionalPacks() {
        val content=(1..6).flatMap {y->Game.entries.flatMap {g->(1..8).map {n->item("dev-${g.name}-$y-$n",g,y,n%3+1)}}}+item("extra:one",Game.MATHS,1,1)
        val free=FamilyAccess(false,0,1000)
        val bundled=content.dropLast(1).map {it.id}.toSet()
        val subset=free.activities(content,bundled)
        assertEquals(108,subset.size);assertFalse(subset.any {it.id=="extra:one"})
        assertEquals(subset.map {it.id}.toSet(),free.activities(content.reversed(),bundled).map {it.id}.toSet())
        assertTrue(subset.groupBy {it.game to it.year}.values.all {it.size==3})
        assertEquals(content,FamilyAccess(false,2000,1000).activities(content,bundled))
    }
    @Test fun developmentAccessIsExplicitAndNeverAnEntitlement() {
        val dev=FamilyAccess(true,0,1000);assertTrue(dev.premium);assertEquals(4,dev.profileLimit)
        assertFalse(FamilyAccess(false,0,1000).premium)
    }
}
