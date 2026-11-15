package com.example.util

import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WordVisualsTest {

    @Test
    fun `word visuals resolves bundled drawable and emojis for curriculum words`() {
        assertEquals(R.drawable.img_word_cat, WordVisuals.drawableResFor("Cat"))
        assertEquals(R.drawable.img_word_cherry, WordVisuals.drawableResFor("  cherry "))
        assertEquals(R.drawable.img_word_color_orange, WordVisuals.drawableResFor("Orange", "oranžová"))
        assertEquals(R.drawable.img_word_orange, WordVisuals.drawableResFor("Orange", "pomeranč"))
        assertEquals(R.drawable.img_word_color_pink, WordVisuals.drawableResFor("Pink", "růžová"))
        assertEquals(R.drawable.img_word_sweater, WordVisuals.drawableResFor("Sweater"))
        assertEquals(R.drawable.img_word_skirt, WordVisuals.drawableResFor("Skirt"))
        assertEquals(R.drawable.img_word_sofa, WordVisuals.drawableResFor("Sofa"))
        assertEquals(R.drawable.img_word_table, WordVisuals.drawableResFor("Table"))
        assertEquals(R.drawable.img_word_garden, WordVisuals.drawableResFor("Garden"))
        assertEquals(R.drawable.img_word_living_room, WordVisuals.drawableResFor("Living room"))
        assertEquals(R.drawable.img_word_dining_room, WordVisuals.drawableResFor("Dining room"))
        assertEquals(R.drawable.img_word_catch, WordVisuals.drawableResFor("Catch"))
        assertEquals(R.drawable.img_word_dive, WordVisuals.drawableResFor("Dive"))
        assertEquals(R.drawable.img_word_sing, WordVisuals.drawableResFor("Sing"))
        assertEquals(R.drawable.img_word_sing, WordVisuals.drawableResFor("Song"))
        assertEquals(R.drawable.img_word_chicken, WordVisuals.drawableResFor("Chicken", "kuře"))
        assertEquals(R.drawable.img_word_roast_chicken, WordVisuals.drawableResFor("Chicken", "kuřecí maso"))
        assertEquals(R.drawable.img_word_skipping_rope, WordVisuals.drawableResFor("Skipping rope"))
        assertEquals(R.drawable.img_word_trampoline, WordVisuals.drawableResFor("Trampoline"))
        assertEquals(R.drawable.img_word_peas, WordVisuals.drawableResFor("Peas"))
        assertEquals(R.drawable.img_word_num_1, WordVisuals.drawableResFor("One"))
        assertEquals(R.drawable.img_word_carrot, WordVisuals.drawableResFor("Carrot"))
        assertEquals("🦁", WordVisuals.emojiFor("Lion", "🐾"))
        assertEquals("🤾", WordVisuals.emojiFor("Catch", "🏃"))
        assertEquals("💌", WordVisuals.emojiFor("Card", "🎁"))
        assertEquals("🟣", WordVisuals.emojiFor("Purple", "🎨"))
        assertEquals("1️⃣", WordVisuals.emojiFor("One", "🔢"))
    }
}
