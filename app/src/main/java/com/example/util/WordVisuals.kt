package com.example.util

import androidx.annotation.DrawableRes
import com.example.R

object WordVisuals {

    private val bundledDrawables: Map<String, Int> = mapOf(
        // Animals
        "cat" to R.drawable.img_word_cat,
        "dog" to R.drawable.img_word_dog,
        "bird" to R.drawable.img_word_bird,
        "fish" to R.drawable.img_word_fish,
        "frog" to R.drawable.img_word_frog,

        // Fruits & Vegetables
        "apple" to R.drawable.img_word_apple,
        "banana" to R.drawable.img_word_banana,
        "orange" to R.drawable.img_word_orange,
        "pear" to R.drawable.img_word_pear,
        "cherry" to R.drawable.img_word_cherry,
        "cherries" to R.drawable.img_word_cherry,
        "strawberry" to R.drawable.img_word_strawberry,
        "watermelon" to R.drawable.img_word_watermelon,
        "grape" to R.drawable.img_word_grapes,
        "grapes" to R.drawable.img_word_grapes,
        "lemon" to R.drawable.img_word_lemon,
        "peach" to R.drawable.img_word_peach,
        "pineapple" to R.drawable.img_word_pineapple,
        "blueberry" to R.drawable.img_word_blueberry,
        "blueberries" to R.drawable.img_word_blueberry,
        "kiwi" to R.drawable.img_word_kiwi,
        "avocado" to R.drawable.img_word_avocado,
        "carrot" to R.drawable.img_word_carrot,
        "tomato" to R.drawable.img_word_tomato,
        "potato" to R.drawable.img_word_potato,
        "cucumber" to R.drawable.img_word_cucumber,
        "pepper" to R.drawable.img_word_pepper,
        "onion" to R.drawable.img_word_onion,
        "broccoli" to R.drawable.img_word_broccoli,
        "corn" to R.drawable.img_word_corn,
        "pea" to R.drawable.img_word_peas,
        "peas" to R.drawable.img_word_peas,
        "garlic" to R.drawable.img_word_garlic,
        "eggplant" to R.drawable.img_word_eggplant,

        // Colors (always circles)
        "red" to R.drawable.img_word_color_red,
        "blue" to R.drawable.img_word_color_blue,
        "green" to R.drawable.img_word_color_green,
        "yellow" to R.drawable.img_word_color_yellow,
        "purple" to R.drawable.img_word_color_purple,
        "pink" to R.drawable.img_word_color_pink,
        "brown" to R.drawable.img_word_color_brown,
        "black" to R.drawable.img_word_color_black,
        "white" to R.drawable.img_word_color_white,
        "grey" to R.drawable.img_word_color_grey,
        "gray" to R.drawable.img_word_color_grey,

        // Unit 3 – Clothes
        "boots" to R.drawable.img_word_boots,
        "hat" to R.drawable.img_word_hat,
        "jacket" to R.drawable.img_word_jacket,
        "shoes" to R.drawable.img_word_shoes,
        "shoe" to R.drawable.img_word_shoes,
        "socks" to R.drawable.img_word_socks,
        "trousers" to R.drawable.img_word_trousers,
        "shorts" to R.drawable.img_word_shorts,
        "skirt" to R.drawable.img_word_skirt,
        "sweater" to R.drawable.img_word_sweater,
        "t-shirt" to R.drawable.img_word_tshirt,

        // Unit 4 – House & Rooms
        "bath" to R.drawable.img_word_bath,
        "bathroom" to R.drawable.img_word_bathroom,
        "bed" to R.drawable.img_word_bed,
        "bedroom" to R.drawable.img_word_bedroom,
        "dining room" to R.drawable.img_word_dining_room,
        "garden" to R.drawable.img_word_garden,
        "kitchen" to R.drawable.img_word_kitchen,
        "living room" to R.drawable.img_word_living_room,
        "sofa" to R.drawable.img_word_sofa,
        "table" to R.drawable.img_word_table,

        // Unit 5 – Actions
        "catch" to R.drawable.img_word_catch,
        "dive" to R.drawable.img_word_dive,
        "sing" to R.drawable.img_word_sing,
        "song" to R.drawable.img_word_sing,

        // Unit 6 – Animals
        "chicken" to R.drawable.img_word_chicken,

        // Unit 8 – Outdoor
        "skipping rope" to R.drawable.img_word_skipping_rope,
        "trampoline" to R.drawable.img_word_trampoline,

        // Numbers 1–10
        "one" to R.drawable.img_word_num_1,
        "two" to R.drawable.img_word_num_2,
        "three" to R.drawable.img_word_num_3,
        "four" to R.drawable.img_word_num_4,
        "five" to R.drawable.img_word_num_5,
        "six" to R.drawable.img_word_num_6,
        "seven" to R.drawable.img_word_num_7,
        "eight" to R.drawable.img_word_num_8,
        "nine" to R.drawable.img_word_num_9,
        "ten" to R.drawable.img_word_num_10
    )

    private val wordEmojis: Map<String, String> = mapOf(
        // Unit 1 – Toys & Birthday
        "balloon" to "🎈",
        "basketball" to "🏀",
        "cake" to "🎂",
        "candle" to "🕯️",
        "car" to "🚗",
        "card" to "💌",
        "clown" to "🤡",
        "present" to "🎁",
        "puzzle" to "🧩",
        "robot" to "🤖",
        "bike" to "🚲",
        "bicycle" to "🚲",

        // Unit 2 – Weather
        "cloudy" to "☁️",
        "cold" to "🥶",
        "dry" to "🏜️",
        "hot" to "🥵",
        "rainy" to "🌧️",
        "snowy" to "❄️",
        "stormy" to "⛈️",
        "sunny" to "☀️",
        "wet" to "💧",
        "windy" to "💨",
        "sun" to "☀️",
        "moon" to "🌙",
        "star" to "⭐",
        "cloud" to "☁️",
        "rain" to "🌧️",
        "rainbow" to "🌈",

        // Unit 3 – Clothes
        "boots" to "🥾",
        "hat" to "👒",
        "jacket" to "🧥",
        "shoes" to "👟",
        "shoe" to "👟",
        "socks" to "🧦",
        "trousers" to "👖",
        "shorts" to "🩳",
        "skirt" to "👗",
        "sweater" to "🧥",
        "t-shirt" to "👕",

        // Unit 4 – House & Rooms
        "bath" to "🛁",
        "bathroom" to "🛁",
        "bed" to "🛏️",
        "bedroom" to "🛏️",
        "dining room" to "🍽️",
        "garden" to "🏡",
        "kitchen" to "🍳",
        "living room" to "🛋️",
        "sofa" to "🛋️",
        "table" to "🪑",
        "house" to "🏠",
        "home" to "🏡",

        // Unit 5 – Actions
        "catch" to "🤾",
        "climb" to "🧗",
        "cook" to "👩‍🍳",
        "dance" to "💃",
        "dive" to "🤿",
        "fly" to "🕊️",
        "jump" to "🦘",
        "run" to "🏃",
        "sing" to "🧑‍🎤",
        "song" to "🧑‍🎤",
        "swim" to "🏊",

        // Unit 6 – Animals (24+)
        "bat" to "🦇",
        "chicken" to "🐔",
        "cow" to "🐮",
        "duck" to "🦆",
        "fox" to "🦊",
        "frog" to "🐸",
        "goat" to "🐐",
        "horse" to "🐴",
        "pig" to "🐷",
        "squirrel" to "🐿️",
        "cat" to "🐱",
        "dog" to "🐶",
        "bird" to "🐦",
        "fish" to "🐠",
        "lion" to "🦁",
        "tiger" to "🐯",
        "elephant" to "🐘",
        "bear" to "🐻",
        "rabbit" to "🐰",
        "bunny" to "🐰",
        "monkey" to "🐵",
        "sheep" to "🐑",
        "mouse" to "🐭",
        "owl" to "🦉",
        "butterfly" to "🦋",
        "bee" to "🐝",
        "turtle" to "🐢",
        "snake" to "🐍",
        "whale" to "🐳",
        "giraffe" to "🦒",

        // Unit 7 – Food & Drink & Fruits/Vegetables
        "cheese" to "🧀",
        "chips" to "🍟",
        "ice cream" to "🍦",
        "pasta" to "🍝",
        "milk" to "🥛",
        "rice" to "🍚",
        "salad" to "🥗",
        "sweets" to "🍬",
        "water" to "💧",
        "apple" to "🍎",
        "banana" to "🍌",
        "orange" to "🍊",
        "pear" to "🍐",
        "cherry" to "🍒",
        "strawberry" to "🍓",
        "grape" to "🍇",
        "grapes" to "🍇",
        "watermelon" to "🍉",
        "lemon" to "🍋",
        "peach" to "🍑",
        "pineapple" to "🍍",
        "blueberry" to "🫐",
        "blueberries" to "🫐",
        "kiwi" to "🥝",
        "avocado" to "🥑",
        "carrot" to "🥕",
        "tomato" to "🍅",
        "potato" to "🥔",
        "cucumber" to "🥒",
        "pepper" to "🫑",
        "onion" to "🧅",
        "broccoli" to "🥦",
        "corn" to "🌽",
        "peas" to "🫛",
        "garlic" to "🧄",
        "eggplant" to "🍆",
        "bread" to "🍞",
        "egg" to "🥚",
        "pizza" to "🍕",

        // Numbers 1–20 & Outdoor games
        "one" to "1️⃣",
        "two" to "2️⃣",
        "three" to "3️⃣",
        "four" to "4️⃣",
        "five" to "5️⃣",
        "six" to "6️⃣",
        "seven" to "7️⃣",
        "eight" to "8️⃣",
        "nine" to "9️⃣",
        "ten" to "🔟",
        "eleven" to "11",
        "twelve" to "12",
        "thirteen" to "13",
        "fourteen" to "14",
        "fifteen" to "15",
        "sixteen" to "16",
        "seventeen" to "17",
        "eighteen" to "18",
        "nineteen" to "19",
        "twenty" to "20",
        "hula hoop" to "⭕",
        "skateboard" to "🛹",
        "skipping rope" to "🪢",
        "trampoline" to "🤸",

        // Colors
        "red" to "🔴",
        "blue" to "🔵",
        "green" to "🟢",
        "yellow" to "🟡",
        "purple" to "🟣",
        "pink" to "⭕",
        "brown" to "🟤",
        "black" to "⚫",
        "white" to "⚪",
        "grey" to "⭕",
        "gray" to "⭕",

        // Other common
        "tree" to "🌳",
        "flower" to "🌸",
        "bus" to "🚌",
        "train" to "🚂",
        "plane" to "✈️",
        "airplane" to "✈️",
        "boat" to "⛵",
        "ship" to "🚢",
        "book" to "📖",
        "pen" to "🖊️",
        "pencil" to "✏️",
        "ball" to "⚽",
        "clock" to "⏰",
        "key" to "🔑",
        "heart" to "❤️",
        "smile" to "😊",
        "happy" to "😄",
        "family" to "👨‍👩‍👧‍👦",
        "school" to "🏫",
        "music" to "🎵"
    )

    @DrawableRes
    fun drawableResFor(englishWord: String, czechTranslation: String = ""): Int? {
        val key = englishWord.trim().lowercase()
        val czLower = czechTranslation.trim().lowercase()
        if (key == "orange") {
            return if (czLower.contains("oranž")) {
                R.drawable.img_word_color_orange
            } else {
                R.drawable.img_word_orange
            }
        }
        if (key == "chicken") {
            return if (czLower.contains("maso") || czLower.contains("pečen")) {
                R.drawable.img_word_roast_chicken
            } else {
                R.drawable.img_word_chicken
            }
        }
        return bundledDrawables[key]
    }

    fun emojiFor(englishWord: String, fallbackEmoji: String): String {
        val key = englishWord.trim().lowercase()
        wordEmojis[key]?.let { return it }
        val wordMatch = wordEmojis.entries.firstOrNull { (token, _) ->
            Regex("\\b${Regex.escape(token)}\\b").containsMatchIn(key)
        }?.value
        return wordMatch ?: fallbackEmoji.ifBlank { "🖼️" }
    }
}
