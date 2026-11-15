package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationEvaluatorTest {

    @Test
    fun evaluate_exactMatch_returns100AndAccepted() {
        val result = PronunciationEvaluator.evaluate("Apple", listOf("apple"))
        assertEquals(100, result.matchPercent)
        assertTrue(result.isAccepted)
    }

    @Test
    fun evaluate_phraseContainingTargetWord_acceptsWord() {
        val result = PronunciationEvaluator.evaluate("Butterfly", listOf("the butterfly"))
        assertEquals(100, result.matchPercent)
        assertTrue(result.isAccepted)
    }

    @Test
    fun evaluate_closePronunciation_returnsHighScore() {
        val result = PronunciationEvaluator.evaluate("Banana", listOf("banan"))
        assertTrue(result.matchPercent >= 72)
        assertTrue(result.isAccepted)
    }

    @Test
    fun evaluate_wrongWord_isRejected() {
        val result = PronunciationEvaluator.evaluate("Elephant", listOf("cat"))
        assertFalse(result.isAccepted)
    }
}
