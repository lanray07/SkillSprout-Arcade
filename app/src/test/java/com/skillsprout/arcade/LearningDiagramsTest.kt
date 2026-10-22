package com.skillsprout.arcade

import org.junit.Assert.*
import org.junit.Test

class LearningDiagramsTest {
    @Test fun shapesFollowTheirLabelsAcrossAnswerOrderChanges() {
        val expected=mapOf("Triangle" to AnswerShape.TRIANGLE,"Circle" to AnswerShape.CIRCLE,"Square" to AnswerShape.SQUARE)
        for(order in listOf(expected.keys.toList(),expected.keys.reversed(),listOf("Circle","Triangle","Square"))) {
            assertEquals(order.map {expected[it]},order.map {answerShape("choice-shapes",it)})
        }
        assertNull(answerShape("","Triangle"));assertNull(answerShape("choice-shapes","Unknown"))
    }
    @Test fun clockMetadataRejectsImpossibleOrIncompleteTimes() {
        assertEquals(ClockDiagram(3,0),clockDiagram("clock:3:00"))
        assertEquals(ClockDiagram(8,30),clockDiagram("clock:8:30"))
        assertEquals(ClockDiagram(12,59),clockDiagram("clock:12:59"))
        for(value in listOf("clock:0:00","clock:13:00","clock:3:60","clock:3:0","clock:3","clock:-3:00")) assertNull(value,clockDiagram(value))
    }
    @Test fun fruitMetadataBoundsTheSymbolCounts() {
        assertEquals(FruitDiagram(4,7),fruitDiagram("fruit:4:7"))
        assertEquals(FruitDiagram(10,1),fruitDiagram("fruit:10:1"))
        for(value in listOf("fruit:0:7","fruit:4:11","fruit:4","fruit:-4:7","fruit:4:7:1")) assertNull(value,fruitDiagram(value))
    }
}
