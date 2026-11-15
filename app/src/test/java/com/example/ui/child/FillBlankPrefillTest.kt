package com.example.ui.child

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FillBlankPrefillTest {

    @Test
    fun prefillCountForLength_scalesFromZeroToTwo() {
        assertEquals(0, prefillCountForLength(0))
        assertEquals(0, prefillCountForLength(1))
        assertEquals(1, prefillCountForLength(2))
        assertEquals(1, prefillCountForLength(3))
        assertEquals(1, prefillCountForLength(5))
        assertEquals(2, prefillCountForLength(6))
        assertEquals(2, prefillCountForLength(10))
    }

    @Test
    fun choosePrefilledIndices_returnsDistinctValidIndices() {
        val random = Random(42)
        for (len in 2..12) {
            val indices = choosePrefilledIndices(len, random)
            val expectedCount = if (len <= 5) 1 else 2
            assertEquals(expectedCount, indices.size)
            assertTrue(indices.all { it in 0 until len })
        }
        assertTrue(choosePrefilledIndices(1, random).isEmpty())
    }
}
