package com.skillsprout.arcade
import android.content.Context

fun Game.displayTitle(context:Context)=context.getString(listOf(R.string.game_title_0,R.string.game_title_1,R.string.game_title_2,R.string.game_title_3,R.string.game_title_4,R.string.game_title_5)[ordinal])
fun Game.displayDistrict(context:Context)=context.getString(listOf(R.string.game_district_0,R.string.game_district_1,R.string.game_district_2,R.string.game_district_3,R.string.game_district_4,R.string.game_district_5)[ordinal])
fun Mastery.displayLabel(context:Context)=context.getString(when(label) {
    "Not yet practised"->R.string.mastery_new
    "Consistently demonstrated"->R.string.mastery_consistent
    "Practising independently"->R.string.mastery_independent
    else->R.string.mastery_developing
})
fun skillLabel(context:Context,skill:String):String {
    val resource=when(skill) {
        "addition"->R.string.skill_addition
        "algebra"->R.string.skill_algebra
        "angles"->R.string.skill_angles
        "area"->R.string.skill_area
        "counting"->R.string.skill_counting
        "decimals"->R.string.skill_decimals
        "division"->R.string.skill_division
        "division-facts"->R.string.skill_division_facts
        "fractions"->R.string.skill_fractions
        "measurement"->R.string.skill_measurement
        "money"->R.string.skill_money
        "multiplication-facts"->R.string.skill_multiplication_facts
        "negative-numbers"->R.string.skill_negative_numbers
        "number-patterns"->R.string.skill_number_patterns
        "percentages"->R.string.skill_percentages
        "perimeter"->R.string.skill_perimeter
        "place-value"->R.string.skill_place_value
        "ratio"->R.string.skill_ratio
        "reading-character"->R.string.skill_reading_character
        "reading-evidence"->R.string.skill_reading_evidence
        "reading-inference"->R.string.skill_reading_inference
        "reading-retrieval"->R.string.skill_reading_retrieval
        "reading-sequencing"->R.string.skill_reading_sequencing
        "reading-summary"->R.string.skill_reading_summary
        "reading-vocabulary"->R.string.skill_reading_vocabulary
        "shape-properties"->R.string.skill_shape_properties
        "spelling"->R.string.skill_spelling
        "statistics"->R.string.skill_statistics
        "subtraction"->R.string.skill_subtraction
        "time"->R.string.skill_time
        "vocabulary"->R.string.skill_vocabulary
        "volume"->R.string.skill_volume
        "word-problems"->R.string.skill_word_problems
        else->null
    }
    return resource?.let {context.getString(it)} ?: skill.replace('-',' ')
}
