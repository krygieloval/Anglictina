package com.example.ui.child

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VocabWord
import com.example.ui.components.WordIllustrationBox
import com.example.ui.theme.BalooFontFamily
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintDarkText
import com.example.ui.theme.MintTertiary
import com.example.ui.theme.SunnyAmber
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class FallingAsteroid(
    val id: Long,
    val label: String,
    val isCorrect: Boolean,
    val xFraction: Float,
    var yFraction: Float,
    val speedPerSec: Float,
    val rotationDeg: Float,
    val spinDegPerSec: Float,
    val shapeSeed: Int,
    val rockColor: Color
)

private data class LaserBolt(
    val id: Long,
    val xFraction: Float,
    val yFraction: Float
)

private data class BurstParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    var ttl: Float
)

private val FALLBACK_DISTRACTORS = listOf(
    "Sun", "Moon", "Star", "Tree", "House", "Water", "Cloud", "Train", "Flower", "Smile"
)

private val ASTEROID_COLORS = listOf(
    Color(0xFF64748B),
    Color(0xFF78716C),
    Color(0xFF6D5D7B),
    Color(0xFF52667A)
)

@Composable
fun WordShooterGameScreen(
    words: List<VocabWord>,
    fallbackEmoji: String,
    onSpeakWord: (String) -> Unit,
    onSpeakCzech: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: (wordOutcomes: Map<VocabWord, Boolean>, starsEarned: Int, score: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val rounds = remember(words) {
        val list = mutableListOf<VocabWord>()
        val shuffled = words.shuffled()
        while (list.size < 8 && shuffled.isNotEmpty()) {
            list.addAll(shuffled)
        }
        list.take(maxOf(words.size, 6).coerceAtMost(10))
    }

    var roundIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(1) }
    var gameEnded by remember { mutableStateOf(false) }
    var roundSolvedBanner by remember { mutableStateOf<String?>(null) }

    val outcomes = remember { mutableStateMapOf<VocabWord, Boolean>() }
    val currentTargetWord = rounds.getOrNull(roundIndex) ?: return

    val asteroids = remember { mutableStateListOf<FallingAsteroid>() }
    val bolts = remember { mutableStateListOf<LaserBolt>() }
    val particles = remember { mutableStateListOf<BurstParticle>() }
    var rocketXFraction by remember { mutableFloatStateOf(0.5f) }
    var targetRocketXFraction by remember { mutableFloatStateOf(0.5f) }
    var movingLeft by remember { mutableStateOf(false) }
    var movingRight by remember { mutableStateOf(false) }
    var pendingArenaShot by remember { mutableStateOf(false) }
    var rocketTiltDeg by remember { mutableFloatStateOf(0f) }
    var lastShotMillis by remember { mutableLongStateOf(0L) }
    var flamePhase by remember { mutableFloatStateOf(0f) }
    var nextId by remember { mutableLongStateOf(1L) }
    var arenaSize by remember { mutableStateOf(IntSize.Zero) }
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    fun fireBolt() {
        if (gameEnded || roundSolvedBanner != null) return
        val now = System.currentTimeMillis()
        if (now - lastShotMillis < 160L) return
        lastShotMillis = now
        bolts.add(
            LaserBolt(
                id = nextId++,
                xFraction = rocketXFraction,
                yFraction = 0.87f
            )
        )
    }

    fun spawnWaveForRound(targetWord: VocabWord) {
        asteroids.clear()
        bolts.clear()
        val otherEnglish = words
            .filter { it.id != targetWord.id && !it.englishWord.equals(targetWord.englishWord, ignoreCase = true) }
            .map { it.englishWord }
            .distinct()
            .shuffled()
            .toMutableList()
        for (fallback in FALLBACK_DISTRACTORS.shuffled()) {
            if (otherEnglish.size >= 3) break
            if (!fallback.equals(targetWord.englishWord, ignoreCase = true) && fallback !in otherEnglish) {
                otherEnglish.add(fallback)
            }
        }
        val labels = (listOf(targetWord.englishWord to true) +
            otherEnglish.take(3).map { it to false }).shuffled()

        val baseLanes = listOf(0.16f, 0.39f, 0.62f, 0.84f).shuffled()
        val yOffsets = listOf(-0.04f, -0.15f, -0.26f, -0.37f).shuffled()
        labels.forEachIndexed { idx, (label, isCorrect) ->
            val jitterX = (Random.nextFloat() - 0.5f) * 0.06f
            val x = (baseLanes[idx % baseLanes.size] + jitterX).coerceIn(0.14f, 0.86f)
            val speed = 0.072f + Random.nextFloat() * 0.036f + (roundIndex * 0.005f)
            asteroids.add(
                FallingAsteroid(
                    id = nextId++,
                    label = label,
                    isCorrect = isCorrect,
                    xFraction = x,
                    yFraction = yOffsets[idx % yOffsets.size],
                    speedPerSec = speed,
                    rotationDeg = Random.nextFloat() * 360f,
                    spinDegPerSec = (Random.nextFloat() - 0.5f) * 70f,
                    shapeSeed = Random.nextInt(1000),
                    rockColor = ASTEROID_COLORS[(idx + roundIndex) % ASTEROID_COLORS.size]
                )
            )
        }
    }

    fun finishNow() {
        if (gameEnded) return
        gameEnded = true
        val correctCount = outcomes.values.count { it }
        val stars = when {
            lives == 3 && correctCount >= rounds.size / 2 -> 3
            correctCount >= rounds.size / 2 -> 2
            else -> 1
        }
        onFinish(outcomes.toMap(), stars, score)
    }

    // Play Czech target word automatically when a new round begins
    LaunchedEffect(roundIndex, gameEnded) {
        if (!gameEnded) {
            roundSolvedBanner = null
            spawnWaveForRound(currentTargetWord)
            onSpeakCzech(currentTargetWord.czechTranslation)
        }
    }

    // Wait briefly after hitting the right asteroid so English TTS finishes before next Czech word
    LaunchedEffect(roundSolvedBanner) {
        if (roundSolvedBanner != null && !gameEnded) {
            delay(1150)
            if (roundIndex + 1 < rounds.size) {
                roundIndex += 1
            } else {
                finishNow()
            }
        }
    }

    LaunchedEffect(roundIndex, gameEnded) {
        var lastNanos = 0L
        while (!gameEnded) {
            withFrameNanos { nanos ->
                if (lastNanos == 0L) {
                    lastNanos = nanos
                    return@withFrameNanos
                }
                val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = nanos
                flamePhase += dt * 18f

                val w = arenaSize.width.toFloat().coerceAtLeast(1f)
                val h = arenaSize.height.toFloat().coerceAtLeast(1f)

                // Smooth rocket steering (continuous hold or eased glide toward targetX)
                val prevRocketX = rocketXFraction
                if (movingLeft && !movingRight) {
                    pendingArenaShot = false
                    rocketXFraction = (rocketXFraction - 0.62f * dt).coerceIn(0.10f, 0.90f)
                    targetRocketXFraction = rocketXFraction
                } else if (movingRight && !movingLeft) {
                    pendingArenaShot = false
                    rocketXFraction = (rocketXFraction + 0.62f * dt).coerceIn(0.10f, 0.90f)
                    targetRocketXFraction = rocketXFraction
                } else {
                    val diff = targetRocketXFraction - rocketXFraction
                    if (abs(diff) <= 0.004f) {
                        rocketXFraction = targetRocketXFraction
                    } else {
                        val maxStep = 1.65f * dt
                        val easedStep = diff * (11f * dt).coerceAtMost(0.45f)
                        val clampedStep = if (abs(easedStep) > maxStep) {
                            if (diff > 0f) maxStep else -maxStep
                        } else {
                            easedStep
                        }
                        rocketXFraction = (rocketXFraction + clampedStep).coerceIn(0.10f, 0.90f)
                    }
                    if (pendingArenaShot && abs(targetRocketXFraction - rocketXFraction) <= 0.04f) {
                        pendingArenaShot = false
                        fireBolt()
                    }
                }
                val rocketVel = if (dt > 0f) (rocketXFraction - prevRocketX) / dt else 0f
                val targetTilt = (rocketVel * 18f).coerceIn(-12f, 12f)
                rocketTiltDeg += (targetTilt - rocketTiltDeg) * (14f * dt).coerceAtMost(1f)

                // Move asteroids straight down top -> bottom (no sideways drift)
                var missedCorrect = false
                if (roundSolvedBanner == null) {
                    val aIt = asteroids.listIterator()
                    while (aIt.hasNext()) {
                        val a = aIt.next()
                        val nextY = a.yFraction + a.speedPerSec * dt
                        if (nextY > 0.89f) {
                            aIt.remove()
                            if (a.isCorrect) {
                                missedCorrect = true
                            }
                        } else {
                            aIt.set(
                                a.copy(
                                    yFraction = nextY,
                                    rotationDeg = a.rotationDeg + a.spinDegPerSec * dt
                                )
                            )
                        }
                    }
                }

                // Move bolts bottom -> top
                val bIt = bolts.listIterator()
                while (bIt.hasNext()) {
                    val b = bIt.next()
                    val nextY = b.yFraction - 1.45f * dt
                    if (nextY < -0.05f) {
                        bIt.remove()
                    } else {
                        bIt.set(b.copy(yFraction = nextY))
                    }
                }

                // Bolt <-> Asteroid collision checks
                if (roundSolvedBanner == null && arenaSize.width > 0 && arenaSize.height > 0) {
                    val hitBoltIds = mutableSetOf<Long>()
                    val hitAsteroidIds = mutableSetOf<Long>()
                    var hitCorrectAsteroid: FallingAsteroid? = null
                    var hitWrongAsteroid = false

                    for (bolt in bolts) {
                        val bx = bolt.xFraction * w
                        val by = bolt.yFraction * h
                        for (asteroid in asteroids) {
                            if (asteroid.id in hitAsteroidIds) continue
                            val ax = asteroid.xFraction * w
                            val ay = asteroid.yFraction * h
                            val dist = hypot((bx - ax).toDouble(), (by - ay).toDouble())
                            if (dist <= 68.0) {
                                hitBoltIds += bolt.id
                                hitAsteroidIds += asteroid.id
                                repeat(14) {
                                    val angle = Random.nextFloat() * 6.28f
                                    val spd = Random.nextFloat() * 280f + 90f
                                    particles.add(
                                        BurstParticle(
                                            x = ax,
                                            y = ay,
                                            vx = cos(angle) * spd,
                                            vy = sin(angle) * spd,
                                            color = if (asteroid.isCorrect) SunnyAmber else CoralSecondary,
                                            ttl = 0.5f
                                        )
                                    )
                                }
                                if (asteroid.isCorrect) {
                                    hitCorrectAsteroid = asteroid
                                } else {
                                    hitWrongAsteroid = true
                                }
                                break
                            }
                        }
                    }

                    if (hitBoltIds.isNotEmpty()) {
                        bolts.removeAll { it.id in hitBoltIds }
                    }
                    if (hitAsteroidIds.isNotEmpty()) {
                        asteroids.removeAll { it.id in hitAsteroidIds }
                    }

                    if (hitCorrectAsteroid != null) {
                        if (!outcomes.containsKey(currentTargetWord)) {
                            outcomes[currentTargetWord] = true
                        }
                        score += 10 * combo
                        combo = (combo + 1).coerceAtMost(5)
                        onSpeakWord(currentTargetWord.englishWord)
                        roundSolvedBanner = currentTargetWord.englishWord
                    } else if (hitWrongAsteroid) {
                        outcomes[currentTargetWord] = false
                        combo = 1
                        lives -= 1
                        if (lives <= 0) {
                            finishNow()
                        }
                    }
                }

                // Update burst particles
                val pIt = particles.listIterator()
                while (pIt.hasNext()) {
                    val p = pIt.next()
                    val rem = p.ttl - dt
                    if (rem <= 0f) {
                        pIt.remove()
                    } else {
                        pIt.set(
                            p.copy(
                                x = p.x + p.vx * dt,
                                y = p.y + p.vy * dt,
                                ttl = rem
                            )
                        )
                    }
                }

                if (missedCorrect) {
                    outcomes[currentTargetWord] = false
                    lives -= 1
                    combo = 1
                    if (lives <= 0) {
                        finishNow()
                    } else if (roundIndex + 1 < rounds.size) {
                        roundIndex += 1
                    } else {
                        finishNow()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 820.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // HUD Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("shooter_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Skóre: $score  •  x$combo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { idx ->
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Život",
                        tint = if (idx < lives) CoralSecondary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Czech target prompt card (tapping replays Czech pronunciation)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable { onSpeakCzech(currentTargetWord.czechTranslation) }
                .testTag("shooter_czech_prompt_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WordIllustrationBox(
                    cacheKey = currentTargetWord.id,
                    imageBase64 = currentTargetWord.imageBase64,
                    fallbackEmoji = fallbackEmoji,
                    englishWord = currentTargetWord.englishWord,
                    contentDescription = currentTargetWord.czechTranslation,
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sestřel asteroid se slovíčkem:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentTargetWord.czechTranslation,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { onSpeakCzech(currentTargetWord.czechTranslation) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("shooter_speak_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Zopakovat české slovíčko",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${roundIndex + 1}/${rounds.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Space shooter arena with rocket + falling asteroids
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B1026),
                            Color(0xFF151E46),
                            Color(0xFF1E2D68)
                        )
                    )
                )
                .onSizeChanged { arenaSize = it }
                .pointerInput(roundIndex, gameEnded) {
                    detectTapGestures { tapOffset ->
                        if (gameEnded || arenaSize.width == 0) return@detectTapGestures
                        targetRocketXFraction = (tapOffset.x / arenaSize.width.toFloat()).coerceIn(0.10f, 0.90f)
                        if (abs(targetRocketXFraction - rocketXFraction) <= 0.04f) {
                            pendingArenaShot = false
                            fireBolt()
                        } else {
                            pendingArenaShot = true
                        }
                    }
                }
                .pointerInput(roundIndex, gameEnded) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        if (gameEnded || arenaSize.width == 0) return@detectDragGestures
                        pendingArenaShot = false
                        targetRocketXFraction = (change.position.x / arenaSize.width.toFloat()).coerceIn(0.10f, 0.90f)
                    }
                }
                .testTag("shooter_arena_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Twinkling stars
                for (i in 0 until 26) {
                    val sx = ((i * 197 + 31) % 100) / 100f * w
                    val sy = ((i * 313 + 17) % 100) / 100f * h
                    drawCircle(
                        color = Color.White.copy(alpha = if (i % 4 == 0) 0.45f else 0.22f),
                        radius = if (i % 3 == 0) 3.5f else 2f,
                        center = Offset(sx, sy)
                    )
                }

                // Draw falling jagged asteroids + English word labels
                for (asteroid in asteroids) {
                    val cx = asteroid.xFraction * w
                    val cy = asteroid.yFraction * h
                    val baseR = 52f

                    rotate(degrees = asteroid.rotationDeg, pivot = Offset(cx, cy)) {
                        val rockPath = Path()
                        val vertices = 10
                        for (v in 0 until vertices) {
                            val angle = (v.toFloat() / vertices) * (2f * Math.PI.toFloat())
                            val bump = 0.82f + (((asteroid.shapeSeed + v * 37) % 35) / 100f)
                            val r = baseR * bump
                            val vx = cx + cos(angle) * r
                            val vy = cy + sin(angle) * r
                            if (v == 0) rockPath.moveTo(vx, vy) else rockPath.lineTo(vx, vy)
                        }
                        rockPath.close()

                        drawPath(
                            path = rockPath,
                            color = asteroid.rockColor.copy(alpha = 0.35f)
                        )
                        drawPath(
                            path = rockPath,
                            color = asteroid.rockColor
                        )
                        // Subtle asteroid craters
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.22f),
                            radius = 11f,
                            center = Offset(cx - 16f, cy - 12f)
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.18f),
                            radius = 7f,
                            center = Offset(cx + 15f, cy + 14f)
                        )
                    }

                    // High-contrast badge on asteroid so English word is super clear
                    val measured = textMeasurer.measure(
                        text = asteroid.label,
                        style = TextStyle(
                            fontFamily = BalooFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    )
                    val pillW = measured.size.width + 26f
                    val pillH = measured.size.height + 14f
                    drawRoundRect(
                        color = Color(0xCC0F172A),
                        topLeft = Offset(cx - pillW / 2f, cy - pillH / 2f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = cx - measured.size.width / 2f,
                            y = cy - measured.size.height / 2f
                        )
                    )
                }

                // Draw upward laser bolts
                for (bolt in bolts) {
                    val bx = bolt.xFraction * w
                    val by = bolt.yFraction * h
                    drawLine(
                        color = MintTertiary,
                        start = Offset(bx, by + 18f),
                        end = Offset(bx, by - 18f),
                        strokeWidth = 10f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(bx, by + 14f),
                        end = Offset(bx, by - 16f),
                        strokeWidth = 4f
                    )
                }

                // Draw burst particles
                for (p in particles) {
                    drawCircle(
                        color = p.color.copy(alpha = (p.ttl / 0.5f).coerceIn(0f, 1f)),
                        radius = 8f,
                        center = Offset(p.x, p.y)
                    )
                }

                // Draw player's spaceship rocket at bottom with smooth banking tilt
                val rx = rocketXFraction * w
                val ry = h - 46f

                rotate(degrees = rocketTiltDeg, pivot = Offset(rx, ry)) {
                    // Exhaust flame
                    val flameFlicker = 16f + sin(flamePhase) * 6f
                    val flamePath = Path().apply {
                        moveTo(rx - 10f, ry + 20f)
                        lineTo(rx, ry + 22f + flameFlicker)
                        lineTo(rx + 10f, ry + 20f)
                        close()
                    }
                    drawPath(flamePath, color = SunnyAmber)

                    // Left & right rocket fins
                    val leftFin = Path().apply {
                        moveTo(rx - 14f, ry + 2f)
                        lineTo(rx - 28f, ry + 24f)
                        lineTo(rx - 12f, ry + 18f)
                        close()
                    }
                    val rightFin = Path().apply {
                        moveTo(rx + 14f, ry + 2f)
                        lineTo(rx + 28f, ry + 24f)
                        lineTo(rx + 12f, ry + 18f)
                        close()
                    }
                    drawPath(leftFin, color = CoralSecondary)
                    drawPath(rightFin, color = CoralSecondary)

                    // Rocket body
                    val bodyPath = Path().apply {
                        moveTo(rx, ry - 34f)
                        lineTo(rx + 16f, ry - 6f)
                        lineTo(rx + 14f, ry + 20f)
                        lineTo(rx - 14f, ry + 20f)
                        lineTo(rx - 16f, ry - 6f)
                        close()
                    }
                    drawPath(bodyPath, color = Color(0xFFF8FAFC))

                    // Rocket nose cone
                    val nosePath = Path().apply {
                        moveTo(rx, ry - 36f)
                        lineTo(rx + 11f, ry - 14f)
                        lineTo(rx - 11f, ry - 14f)
                        close()
                    }
                    drawPath(nosePath, color = CoralSecondary)

                    // Cockpit window
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 8f,
                        center = Offset(rx, ry - 2f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = 3f,
                        center = Offset(rx - 2.5f, ry - 4f)
                    )
                }
            }

            if (roundSolvedBanner != null) {
                Surface(
                    color = MintContainer,
                    shape = RoundedCornerShape(50),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Skvělý zásah! $roundSolvedBanner",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MintDarkText,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                    )
                }
            }
        }

        // Bottom kid-friendly rocket steering (hold to glide smoothly) + fire bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .semantics {
                        role = Role.Button
                        contentDescription = "Doleva"
                        onClick {
                            targetRocketXFraction = (targetRocketXFraction - 0.10f).coerceIn(0.10f, 0.90f)
                            true
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            movingLeft = true
                            val downAt = System.currentTimeMillis()
                            waitForUpOrCancellation()
                            movingLeft = false
                            if (System.currentTimeMillis() - downAt < 140L) {
                                targetRocketXFraction = (targetRocketXFraction - 0.10f).coerceIn(0.10f, 0.90f)
                            }
                        }
                    }
                    .testTag("shooter_move_left")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Button(
                onClick = { fireBolt() },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralSecondary),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("shooter_fire_button")
            ) {
                Icon(Icons.Rounded.RocketLaunch, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vystřelit!", style = MaterialTheme.typography.titleMedium)
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .semantics {
                        role = Role.Button
                        contentDescription = "Doprava"
                        onClick {
                            targetRocketXFraction = (targetRocketXFraction + 0.10f).coerceIn(0.10f, 0.90f)
                            true
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            movingRight = true
                            val downAt = System.currentTimeMillis()
                            waitForUpOrCancellation()
                            movingRight = false
                            if (System.currentTimeMillis() - downAt < 140L) {
                                targetRocketXFraction = (targetRocketXFraction + 0.10f).coerceIn(0.10f, 0.90f)
                            }
                        }
                    }
                    .testTag("shooter_move_right")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
