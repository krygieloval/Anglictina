package com.example.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import com.caverock.androidsvg.SVG
import com.example.util.ImageCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

data class StockPhotoResult(
    val id: String,
    val title: String,
    val thumbUrl: String,
    val sourceName: String,
    val license: String,
    val fallbackUrl: String = ""
)

class StockPhotoRepository(
    val okHttpClient: OkHttpClient = defaultOkHttpClient()
) {
    suspend fun searchPhotos(query: String): List<StockPhotoResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            coroutineScope {
                val iconifyDeferred = async {
                    runCatching { fetchIconifyIcons(cleanQuery) }
                }
                val openverseDeferred = async {
                    runCatching { fetchOpenversePhotos(cleanQuery) }
                }
                val commonsDeferred = async {
                    runCatching { fetchCommonsPhotos(cleanQuery) }
                }
                val wikiDeferred = async {
                    runCatching { fetchWikipediaPhotos(cleanQuery) }
                }

                val iconifyAttempt = iconifyDeferred.await()
                val openverseAttempt = openverseDeferred.await()
                val commonsAttempt = commonsDeferred.await()
                val wikiAttempt = wikiDeferred.await()

                val attempts = listOf(iconifyAttempt, openverseAttempt, commonsAttempt, wikiAttempt)
                attempts.forEachIndexed { idx, result ->
                    result.exceptionOrNull()?.let { err ->
                        if (err is CancellationException) throw err
                        Log.w(TAG, "Image bank source #$idx failed for '$cleanQuery'", err)
                    }
                }

                val anySucceeded = attempts.any { it.isSuccess }
                if (!anySucceeded) {
                    val firstError = attempts.firstNotNullOfOrNull { it.exceptionOrNull() }
                    throw IllegalStateException(
                        "Nepodařilo se spojit s databankou obrázků. Zkontrolujte internetové připojení nebo nahrajte vlastní obrázek.",
                        firstError
                    )
                }

                val iconifyResults = iconifyAttempt.getOrDefault(emptyList())
                val openverseResults = openverseAttempt.getOrDefault(emptyList())
                val commonsResults = commonsAttempt.getOrDefault(emptyList())
                val wikiResults = wikiAttempt.getOrDefault(emptyList())

                val combined = LinkedHashMap<String, StockPhotoResult>()
                for (item in iconifyResults + openverseResults + commonsResults + wikiResults) {
                    val key = item.thumbUrl.substringAfterLast('/')
                    if (!combined.containsKey(item.thumbUrl) && !combined.containsKey(key)) {
                        combined[item.thumbUrl] = item
                    }
                }
                combined.values.take(MAX_RESULTS)
            }
        }
    }

    suspend fun downloadAndEncodeBase64(photo: StockPhotoResult): String {
        val candidates = listOf(photo.thumbUrl, photo.fallbackUrl)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        var lastError: Throwable? = null
        for (url in candidates) {
            try {
                return downloadAndEncodeBase64(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download stock photo from $url", e)
                lastError = e
            }
        }
        throw IllegalStateException(
            lastError?.localizedMessage ?: "Nepodařilo se stáhnout vybraný obrázek.",
            lastError
        )
    }

    suspend fun downloadAndEncodeBase64(thumbUrl: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(thumbUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        "Nepodařilo se stáhnout obrázek (HTTP ${response.code})."
                    )
                }
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                val bytes = response.body?.bytes()
                    ?: throw IllegalStateException("Databanka nevrátila žádná data obrázku.")

                val looksLikeSvg = thumbUrl.contains(".svg", ignoreCase = true) ||
                    contentType.contains("svg") ||
                    bytes.take(256).toByteArray().decodeToString().contains("<svg", ignoreCase = true)

                val bitmap = if (looksLikeSvg) {
                    renderSvgBytesToBitmap(bytes)
                } else {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } ?: throw IllegalStateException("Stažený soubor není platný obrázek.")

                try {
                    ImageCodec.encodeToBase64(bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun renderSvgBytesToBitmap(bytes: ByteArray): Bitmap? {
        return try {
            val svg = SVG.getFromInputStream(ByteArrayInputStream(bytes))
            val targetSize = 320
            svg.setDocumentWidth(targetSize.toFloat())
            svg.setDocumentHeight(targetSize.toFloat())
            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(255, 251, 245))
            svg.renderToCanvas(canvas)
            bitmap
        } catch (e: Exception) {
            Log.w(TAG, "Failed to render SVG bytes", e)
            null
        }
    }

    private fun fetchIconifyIcons(query: String): List<StockPhotoResult> {
        val preferredUrl = "https://api.iconify.design/search".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("prefixes", ICONIFY_COLORED_PREFIXES)
            .addQueryParameter("limit", "16")
            .build()
        val preferredResults = parseIconifyResults(executeGet(preferredUrl.toString()))
        if (preferredResults.size >= 4) {
            return preferredResults
        }
        val fallbackUrl = "https://api.iconify.design/search".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("limit", "16")
            .build()
        val fallbackResults = runCatching {
            parseIconifyResults(executeGet(fallbackUrl.toString()))
        }.getOrDefault(emptyList())
        return (preferredResults + fallbackResults).distinctBy { it.thumbUrl }
    }

    private fun fetchOpenversePhotos(query: String): List<StockPhotoResult> {
        val url = "https://api.openverse.org/v1/images/".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("mature", "false")
            .addQueryParameter("page_size", "16")
            .addQueryParameter("category", "photograph,illustration")
            .build()

        val body = executeGet(url.toString())
        return parseOpenverseResults(body)
    }

    private fun fetchWikipediaPhotos(query: String): List<StockPhotoResult> {
        val hasCzechDiacritics = query.any { it in "áčďéěíňóřšťúůýžÁČĎÉĚÍŇÓŘŠŤÚŮÝŽ" }
        val host = if (hasCzechDiacritics) "cs.wikipedia.org" else "en.wikipedia.org"
        val url = "https://$host/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrsearch", query)
            .addQueryParameter("gsrlimit", "6")
            .addQueryParameter("prop", "pageimages")
            .addQueryParameter("piprop", "thumbnail")
            .addQueryParameter("pithumbsize", "480")
            .addQueryParameter("format", "json")
            .build()

        val body = executeGet(url.toString())
        return parseWikipediaPages(body)
    }

    private fun fetchCommonsPhotos(query: String): List<StockPhotoResult> {
        val url = "https://commons.wikimedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrnamespace", "6")
            .addQueryParameter("gsrsearch", "$query filetype:bitmap")
            .addQueryParameter("gsrlimit", "10")
            .addQueryParameter("prop", "imageinfo")
            .addQueryParameter("iiprop", "thumburl|mime|extmetadata")
            .addQueryParameter("iiurlwidth", "480")
            .addQueryParameter("format", "json")
            .build()

        val body = executeGet(url.toString())
        return parseCommonsPages(body)
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Databanka obrázků neodpověděla (HTTP ${response.code}).")
            }
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        private const val TAG = "StockPhotoRepository"
        private const val MAX_RESULTS = 20
        private const val ICONIFY_COLORED_PREFIXES =
            "fluent-emoji-flat,noto,twemoji,icon-park,streamline-emojis,flat-color-icons"
        const val USER_AGENT = "SlovickaAndroidApp/1.0 (https://slovicka.app; support@slovicka.app)"

        private val ALLOWED_MIMES = setOf("image/jpeg", "image/png", "image/webp")
        private val EXCLUDED_KEYWORDS = listOf(
            "commons-logo",
            "ambox",
            "flag_of_",
            "question_book",
            "disambig",
            "symbol_",
            "padlock",
            "wikimedia-logo",
            "crystal_clear"
        )

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(req)
            }
            .build()

        internal fun parseIconifyResults(jsonString: String): List<StockPhotoResult> {
            if (jsonString.isBlank()) return emptyList()
            val root = JSONObject(jsonString)
            val iconsArray = root.optJSONArray("icons") ?: return emptyList()
            val collectionsObj = root.optJSONObject("collections")
            val items = mutableListOf<StockPhotoResult>()
            for (i in 0 until iconsArray.length()) {
                val fullName = iconsArray.optString(i).trim()
                val colonIdx = fullName.indexOf(':')
                if (colonIdx <= 0 || colonIdx >= fullName.lastIndex) continue
                val prefix = fullName.substring(0, colonIdx)
                val name = fullName.substring(colonIdx + 1)
                val svgUrl = "https://api.iconify.design/$prefix/$name.svg"
                val title = name.replace('-', ' ').replace('_', ' ')
                    .replaceFirstChar { it.uppercase() }
                val collectionInfo = collectionsObj?.optJSONObject(prefix)
                val collectionName = collectionInfo?.optString("name")?.trim().orEmpty()
                    .ifBlank { prefix }
                val licenseTitle = collectionInfo?.optJSONObject("license")
                    ?.optString("title")
                    ?.trim()
                    .orEmpty()
                    .ifBlank { "Iconify" }

                items += StockPhotoResult(
                    id = "iconify_$fullName",
                    title = title,
                    thumbUrl = svgUrl,
                    sourceName = "Iconify • $collectionName",
                    license = licenseTitle
                )
            }
            return items
        }

        internal fun parseOpenverseResults(jsonString: String): List<StockPhotoResult> {
            if (jsonString.isBlank()) return emptyList()
            val root = JSONObject(jsonString)
            val resultsArray = root.optJSONArray("results") ?: return emptyList()
            val items = mutableListOf<StockPhotoResult>()
            for (i in 0 until resultsArray.length()) {
                val obj = resultsArray.optJSONObject(i) ?: continue
                val id = obj.optString("id").ifBlank { "ov_$i" }
                val filetype = obj.optString("filetype").lowercase()
                if (filetype == "svg") continue

                val thumb = obj.optString("thumbnail").trim()
                val full = obj.optString("url").trim()
                val primaryUrl = thumb.ifBlank { full }
                if (primaryUrl.isBlank() || isUnwantedImageUrl(primaryUrl)) continue

                val fallback = if (thumb.isNotBlank() && full.isNotBlank() && full != thumb && !isUnwantedImageUrl(full)) {
                    full
                } else {
                    ""
                }

                val title = obj.optString("title").trim().ifBlank { "Obrázek" }
                val rawSource = obj.optString("source").trim()
                    .ifBlank { obj.optString("provider").trim() }
                val sourceLabel = if (rawSource.isNotBlank()) {
                    "Openverse • ${rawSource.replaceFirstChar { it.uppercase() }}"
                } else {
                    "Openverse"
                }
                val license = obj.optString("license").trim().uppercase().ifBlank { "CC" }

                items += StockPhotoResult(
                    id = "ov_$id",
                    title = title,
                    thumbUrl = primaryUrl,
                    sourceName = sourceLabel,
                    license = license,
                    fallbackUrl = fallback
                )
            }
            return items
        }

        internal fun parseWikipediaPages(jsonString: String): List<StockPhotoResult> {
            if (jsonString.isBlank()) return emptyList()
            val root = JSONObject(jsonString)
            val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
            val indexed = mutableListOf<Pair<Int, StockPhotoResult>>()
            val keys = pages.keys()
            while (keys.hasNext()) {
                val pageId = keys.next()
                val page = pages.optJSONObject(pageId) ?: continue
                val thumbUrl = page.optJSONObject("thumbnail")?.optString("source").orEmpty()
                if (thumbUrl.isBlank() || isUnwantedImageUrl(thumbUrl)) continue
                val title = page.optString("title").ifBlank { "Foto" }
                val index = page.optInt("index", 999)
                indexed += index to StockPhotoResult(
                    id = "wiki_$pageId",
                    title = title,
                    thumbUrl = thumbUrl,
                    sourceName = "Wikipedia",
                    license = "CC / Public"
                )
            }
            return indexed.sortedBy { it.first }.map { it.second }
        }

        internal fun parseCommonsPages(jsonString: String): List<StockPhotoResult> {
            if (jsonString.isBlank()) return emptyList()
            val root = JSONObject(jsonString)
            val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
            val indexed = mutableListOf<Pair<Int, StockPhotoResult>>()
            val keys = pages.keys()
            while (keys.hasNext()) {
                val pageId = keys.next()
                val page = pages.optJSONObject(pageId) ?: continue
                val imageInfoArray = page.optJSONArray("imageinfo") ?: continue
                if (imageInfoArray.length() == 0) continue
                val info = imageInfoArray.optJSONObject(0) ?: continue
                val mime = info.optString("mime").lowercase()
                if (mime !in ALLOWED_MIMES) continue
                val thumbUrl = info.optString("thumburl")
                    .ifBlank { info.optString("url") }
                if (thumbUrl.isBlank() || isUnwantedImageUrl(thumbUrl)) continue

                val rawTitle = page.optString("title")
                    .removePrefix("File:")
                    .substringBeforeLast('.')
                    .replace('_', ' ')
                    .trim()
                val license = info.optJSONObject("extmetadata")
                    ?.optJSONObject("LicenseShortName")
                    ?.optString("value")
                    ?.ifBlank { "Wikimedia" }
                    ?: "Wikimedia"
                val index = page.optInt("index", 999)

                indexed += index to StockPhotoResult(
                    id = "commons_$pageId",
                    title = rawTitle.ifBlank { "Fotografie" },
                    thumbUrl = thumbUrl,
                    sourceName = "Wikimedia Commons",
                    license = license
                )
            }
            return indexed.sortedBy { it.first }.map { it.second }
        }

        private fun isUnwantedImageUrl(url: String): Boolean {
            val lower = url.lowercase()
            if (lower.endsWith(".svg") || lower.contains(".svg/")) return true
            return EXCLUDED_KEYWORDS.any { lower.contains(it) }
        }
    }
}
