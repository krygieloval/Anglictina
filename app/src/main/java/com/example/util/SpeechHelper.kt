package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.min

class VocabTtsPlayer(context: Context) {
    private var enTts: TextToSpeech? = null
    private var csTts: TextToSpeech? = null
    private var isEnReady = false
    private var isCsReady = false

    init {
        val appContext = context.applicationContext
        enTts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = enTts?.setLanguage(Locale.US)
                enTts?.setSpeechRate(0.85f)
                enTts?.setPitch(1.05f)
                isEnReady = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        csTts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val csCz = Locale.forLanguageTag("cs-CZ")
                var res = csTts?.setLanguage(csCz) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    res = csTts?.setLanguage(Locale.forLanguageTag("cs")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                }
                csTts?.setSpeechRate(0.95f)
                csTts?.setPitch(1.03f)
                isCsReady = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speakEnglish(text: String) {
        if (text.isBlank()) return
        csTts?.stop()
        if (isEnReady) {
            enTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "en_${text.hashCode()}")
        }
    }

    fun speakCzech(text: String) {
        if (text.isBlank()) return
        enTts?.stop()
        if (isCsReady) {
            csTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cs_${text.hashCode()}")
        }
    }

    fun release() {
        enTts?.stop()
        enTts?.shutdown()
        enTts = null
        csTts?.stop()
        csTts?.shutdown()
        csTts = null
    }
}

object PronunciationEvaluator {
    data class EvaluationResult(
        val heardText: String,
        val matchPercent: Int,
        val isAccepted: Boolean
    )

    fun evaluate(targetWord: String, candidates: List<String>): EvaluationResult {
        val cleanTarget = normalize(targetWord)
        if (candidates.isEmpty() || cleanTarget.isEmpty()) {
            return EvaluationResult(heardText = "", matchPercent = 0, isAccepted = false)
        }

        var bestCandidate = candidates.first()
        var bestScore = 0

        for (candidate in candidates) {
            val tokens = normalize(candidate).split(" ").filter { it.isNotBlank() }
            val options = listOf(normalize(candidate)) + tokens
            for (opt in options) {
                val sim = similarityPercent(cleanTarget, opt)
                if (sim > bestScore) {
                    bestScore = sim
                    bestCandidate = candidate
                }
            }
        }

        return EvaluationResult(
            heardText = bestCandidate,
            matchPercent = bestScore,
            isAccepted = bestScore >= 72
        )
    }

    private fun normalize(s: String): String =
        s.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun similarityPercent(a: String, b: String): Int {
        if (a == b) return 100
        if (a.isEmpty() || b.isEmpty()) return 0
        val dist = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length)
        return (((maxLen - dist).toFloat() / maxLen) * 100f).toInt().coerceIn(0, 100)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}

class EnglishSpeechListener(
    private val context: Context,
    private val onRmsChanged: (Float) -> Unit,
    private val onResults: (List<String>) -> Unit,
    private val onError: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        stopListening()
        if (!isAvailable()) {
            onError("Rozpoznávání řeči není na tomto zařízení dostupné.")
            return
        }
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                onRmsChanged(rmsdB)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Neslyšel jsem slovíčko, zkus to říct ještě jednou nahlas!"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Čas na mluvení vypršel, zkus to znovu."
                    SpeechRecognizer.ERROR_AUDIO -> "Problém s mikrofonem."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chybí oprávnění pro mikrofon."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Rozpoznávání řeči potřebuje připojení k internetu."
                    else -> "Nepodařilo se rozpoznat řeč (kód $error)."
                }
                onError(msg)
            }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                onResults(list)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        sr.startListening(intent)
    }

    fun stopListening() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }
}
