package com.skillsprout.arcade

import kotlin.math.roundToInt

enum class Game(val title: String, val district: String, val symbol: String, val blurb: String) {
    MATHS("Maths Dash", "Number Valley", "＋", "Hop across the valley, one discovery at a time."),
    SPELLING("Spelling Sprint", "Wordwood Forest", "Aa", "Gather letter tiles. Bring a word to life."),
    STORY("Story Detectives", "Story Harbour", "≋", "Follow the clues in a little story."),
    TABLES("Times Table Heroes", "Number Valley", "×", "Build groups to help the woodland heroes."),
    LOGIC("Logic Lab", "Puzzle Peaks", "◇", "Find a pattern. Unlock a new way forward."),
    WORDS("Word Builder", "Wordwood Forest", "abc", "Fit the words together and grow your vocabulary.")
}
data class Activity(
    val id: String, val game: Game, val subject: String, val skill: String, val year: Int,
    val objective: String, val reference: String, val difficulty: Int,
    val prerequisites: List<String>, val type: String, val prompt: String,
    val choices: List<String>, val answer: String, val explanation: String,
    val hints: List<String>, val story: String = "", val visual: String = "",
    val reviewStatus: String = "Awaiting Teacher Review", val version: Int = 1
)
data class Evidence(val correct: Boolean, val hints: Int, val difficulty: Int)
data class Mastery(val level: Int, val label: String, val accuracy: Int)
object LearningEngine {
    fun mastery(history: List<Evidence>): Mastery {
        if (history.isEmpty()) return Mastery(1, "Not yet practised", 0)
        val recent = history.takeLast(10)
        val accuracy = (100.0 * recent.count { it.correct } / recent.size).roundToInt()
        val independent = recent.count { it.correct && it.hints == 0 }
        val label = when { recent.size >= 8 && independent >= 8 -> "Consistently demonstrated"
            recent.size >= 5 && independent >= 4 -> "Practising independently"
            else -> "Developing" }
        val level = when { recent.takeLast(3).size == 3 && recent.takeLast(3).all { it.correct && it.hints == 0 } -> (recent.last().difficulty + 1).coerceAtMost(3)
            recent.takeLast(3).count { !it.correct } >= 2 -> (recent.last().difficulty - 1).coerceAtLeast(1)
            else -> recent.last().difficulty }
        return Mastery(level, label, accuracy)
    }
    fun select(pool: List<Activity>, year: Int, history: Map<String,List<Evidence>>, recent: List<String>, fixedDifficulty: Int? = null): Activity? {
        val eligible = pool.filter { it.year == year && it.id !in recent.takeLast(12) }
        val candidates = eligible.ifEmpty { pool.filter { it.year == year && it.id != recent.lastOrNull() } }
            .ifEmpty {pool.filter {it.year==year}}
        return candidates.minWithOrNull(compareBy<Activity> {
            val missingPrerequisite = it.prerequisites.any { p -> mastery(history[p].orEmpty()).label != "Consistently demonstrated" }
            val wanted = if (missingPrerequisite) 1 else fixedDifficulty ?: mastery(history[it.skill].orEmpty()).level
            kotlin.math.abs(it.difficulty-wanted)
        }.thenBy { history[it.skill].orEmpty().size }.thenBy { it.id })
    }
    fun validate(a: Activity): List<String> = buildList {
        if (a.id.isBlank() || a.skill.isBlank() || a.objective.isBlank() || a.reference.isBlank()) add("Missing mapping")
        if (a.year !in 1..6 || a.difficulty !in 1..3 || a.version < 1) add("Invalid range")
        if (a.answer.isBlank() || a.explanation.isBlank() || a.hints.isEmpty()) add("Missing learning support")
        if (a.type !in setOf("choice","word","letters","sequence")) add("Unsupported question type")
        if (a.choices.size !in 1..40 || a.choices.any {it.isBlank() || it.length>200}) add("Invalid option size")
        if (a.type in setOf("choice","word") && (a.answer !in a.choices || a.choices.distinct().size!=a.choices.size)) add("Invalid options")
        if (a.type=="letters" && a.choices.sorted()!=a.answer.map {it.toString()}.sorted()) add("Invalid letter tiles")
        if (a.type=="sequence" && a.choices.sorted()!=a.answer.split(' ').sorted()) add("Invalid sequence tiles")
        if(a.game==Game.TABLES && (!a.visual.matches(Regex("array:[0-9]+:[0-9]+")) || a.visual.split(':').drop(1).any {it.toIntOrNull() !in 1..12})) add("Invalid grouping visual")
        if(a.visual.startsWith("clock:") && clockDiagram(a.visual)==null) add("Invalid clock visual")
        if(a.visual.startsWith("fruit:") && fruitDiagram(a.visual)==null) add("Invalid fruit visual")
        if(a.visual=="choice-shapes" && (a.type!="choice" || a.choices.any {answerShape(a.visual,it)==null})) add("Invalid shape options")
        if (a.type == "choice" && (a.answer !in a.choices || a.choices.distinct().size != a.choices.size)) add("Invalid choices")
        if (a.reviewStatus !in setOf("Draft", "Automated Validation Passed", "Awaiting Teacher Review", "Approved", "Rejected", "Retired")) add("Invalid review status")
    }
}
object VoiceCommands {
    fun parse(text: String): String? = when(text.lowercase().trim().trimEnd('.', '!')) {
        "start maths" -> "maths"; "repeat question" -> "repeat"; "give me a hint" -> "hint"
        "next question" -> "next"; "pause game" -> "pause"; "read the story" -> "read"
        else -> Regex("my answer is (one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|[0-9]+)")
            .matchEntire(text.lowercase().trim())?.groupValues?.get(1)?.let { word ->
                val n = listOf("zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve").indexOf(word)
                "answer:${if(n >= 0) n.toString() else word}" }
    }
}
