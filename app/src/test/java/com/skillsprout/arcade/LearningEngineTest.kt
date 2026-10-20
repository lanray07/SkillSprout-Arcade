package com.skillsprout.arcade
import org.junit.Assert.*
import org.junit.Test

class LearningEngineTest {
    private fun activity(id:String,d:Int=1,prereq:List<String> = emptyList()) = Activity(id,Game.MATHS,"Maths","addition",2,"Add two numbers","DfE year 2",d,prereq,"choice","2 + 2?",listOf("3","4"),"4","Two pairs make four",listOf("Use counters"))
    @Test fun unpractisedHasNoInventedMastery() { assertEquals("Not yet practised",LearningEngine.mastery(emptyList()).label) }
    @Test fun hintsPreventDifficultyIncrease() { assertEquals(1,LearningEngine.mastery(List(5){Evidence(true,1,1)}).level) }
    @Test fun consistentSuccessIncreasesDifficultyWithinBounds() { assertEquals(2,LearningEngine.mastery(List(3){Evidence(true,0,1)}).level);assertEquals(3,LearningEngine.mastery(List(10){Evidence(true,0,3)}).level) }
    @Test fun repeatedErrorsOfferEasierPractice() { assertEquals(1,LearningEngine.mastery(List(3){Evidence(false,0,2)}).level) }
    @Test fun masteryNeedsSustainedIndependentEvidence() { assertEquals("Developing",LearningEngine.mastery(List(3){Evidence(true,0,1)}).label); assertEquals("Consistently demonstrated",LearningEngine.mastery(List(8){Evidence(true,0,2)}).label) }
    @Test fun recentActivityIsAvoidedAndSelectionIsDeterministic() { val pool=listOf(activity("a"),activity("b"));assertEquals("b",LearningEngine.select(pool,2,emptyMap(),listOf("a"))?.id);assertEquals(LearningEngine.select(pool,2,emptyMap(),emptyList()),LearningEngine.select(pool,2,emptyMap(),emptyList())) }
    @Test fun missingPrerequisiteKeepsDifficultyLow() { val pool=listOf(activity("a",3,listOf("counting")),activity("b",1,listOf("counting"))); assertEquals(1,LearningEngine.select(pool,2,mapOf("addition" to List(8){Evidence(true,0,3)}),emptyList())?.difficulty) }
    @Test fun invalidAnswerAndDuplicateChoicesRejected() { assertTrue(LearningEngine.validate(activity("a").copy(answer="99")).isNotEmpty()); assertTrue(LearningEngine.validate(activity("a").copy(choices=listOf("4","4"))).isNotEmpty()) }
    @Test fun constrainedVoiceParserRejectsOpenEndedChat() {assertNull(VoiceCommands.parse("tell me anything"));assertEquals("hint",VoiceCommands.parse("Give me a hint."));assertEquals("answer:12",VoiceCommands.parse("my answer is twelve"))}
    @Test fun csvNeutralisesSpreadsheetFormulas() {assertTrue(progressCsv(listOf(Child("1","=CMD()")),listOf(Attempt("x","1","a","add","MATHS",true,0,1,3,0))).contains("'=CMD()"))}
}
