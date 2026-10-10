package app.booklore.android.core.network

import app.booklore.android.core.model.Annotation
import app.booklore.android.core.model.BookCompletionHeatmap
import app.booklore.android.core.model.Book
import app.booklore.android.core.model.CompletionRaceEntry
import app.booklore.android.core.model.CreateAnnotationRequest
import app.booklore.android.core.model.EpubPosition
import app.booklore.android.core.model.HealthEnvelope
import app.booklore.android.core.model.Library
import app.booklore.android.core.model.PdfPosition
import app.booklore.android.core.model.PublicSettings
import app.booklore.android.core.model.PageTurnerScore
import app.booklore.android.core.model.ReadingBookTimeline
import app.booklore.android.core.model.ReadingBookDistributions
import app.booklore.android.core.model.ReadingCompletionTimeline
import app.booklore.android.core.model.ReadingGenreStatistic
import app.booklore.android.core.model.ReadingSessionScatter
import app.booklore.android.core.model.ReadingSessionHeatmapDay
import app.booklore.android.core.model.ReadingSessionReport
import app.booklore.android.core.model.ReadingSessionTimeline
import app.booklore.android.core.model.ReadingSpeed
import app.booklore.android.core.model.ReadingStatistics
import app.booklore.android.core.model.ReadingStreak
import app.booklore.android.core.model.FavoriteReadingDay
import app.booklore.android.core.model.PeakReadingHour
import app.booklore.android.core.model.ServerConnection
import app.booklore.android.core.model.Session
import app.booklore.android.core.model.Shelf
import app.booklore.android.core.model.TokenResponse
import app.booklore.android.core.model.UpdateAnnotationRequest
import app.booklore.android.core.security.SessionStore
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.WeekFields
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit

class BookLoreApi(private val sessionStore: SessionStore) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val refreshMutex = Mutex()

    suspend fun probe(rawUrl: String, allowHttp: Boolean): ServerConnection {
        val baseUrl = normalizeBaseUrl(rawUrl, allowHttp)
        val health = request(baseUrl, "/api/v1/healthcheck", authenticated = false)
            .decode(HealthEnvelope.serializer())
        if (health.data?.status != "UP") throw ApiException(503, "Server is not ready")
        val settings = request(baseUrl, "/api/v1/public-settings", authenticated = false)
            .decode(PublicSettings.serializer())
        return ServerConnection(baseUrl, health.data.version, settings)
    }

    suspend fun login(baseUrl: String, username: String, password: String): Session {
        val body = buildJsonObject {
            put("username", username)
            put("password", password)
        }
        val tokens = request(baseUrl, "/api/v1/auth/login", method = "POST", body = body, authenticated = false)
            .decode(TokenResponse.serializer())
        return Session(baseUrl, tokens.accessToken, tokens.refreshToken).also(sessionStore::write)
    }

    suspend fun logout() {
        val current = sessionStore.read() ?: return
        try {
            val body = buildJsonObject { put("refreshToken", current.refreshToken) }
            request(current.baseUrl, "/api/v1/auth/logout", method = "POST", body = body, authenticated = true)
        } catch (_: Exception) {
            // Local token deletion is still required when the server is unavailable.
        } finally {
            sessionStore.clear()
        }
    }

    suspend fun libraries(): List<Library> = authenticatedGet("/api/v1/libraries", ListSerializer(Library.serializer()))

    /** Triggers the server-side filesystem scan for one library. */
    suspend fun rescanLibrary(libraryId: Long) {
        authenticatedRequest("/api/v1/libraries/$libraryId/refresh", method = "PUT")
    }

    suspend fun shelves(): List<Shelf> = authenticatedGet("/api/v1/shelves", ListSerializer(Shelf.serializer()))

    suspend fun books(): List<Book> = authenticatedGet("/api/v1/books?withDescription=false", ListSerializer(Book.serializer()))

    suspend fun book(bookId: Long): Book = authenticatedGet("/api/v1/books/$bookId?withDescription=true", Book.serializer())

    /** Fetches the compact set of server-backed statistics displayed by the Android client. */
    suspend fun readingStatistics(year: Int): ReadingStatistics = coroutineScope {
        val week = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear())
        val heatmap = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/heatmap?year=$year",
                ListSerializer(ReadingSessionHeatmapDay.serializer()),
            )
        }
        val streak = async {
            authenticatedGet("/api/v1/user-stats/reading/streak", ReadingStreak.serializer())
        }
        val favoriteDays = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/favorite-days?year=$year",
                ListSerializer(FavoriteReadingDay.serializer()),
            )
        }
        val peakHours = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/peak-hours?year=$year",
                ListSerializer(PeakReadingHour.serializer()),
            )
        }
        val genres = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/genres",
                ListSerializer(ReadingGenreStatistic.serializer()),
            )
        }
        val distributions = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/book-distributions",
                ReadingBookDistributions.serializer(),
            )
        }
        val timeline = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/timeline?year=$year&week=$week",
                ListSerializer(ReadingSessionTimeline.serializer()),
            )
        }
        val speed = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/speed?year=$year",
                ListSerializer(ReadingSpeed.serializer()),
            )
        }
        val completionTimeline = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/completion-timeline?year=$year",
                ListSerializer(ReadingCompletionTimeline.serializer()),
            )
        }
        val completionHeatmap = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/book-completion-heatmap",
                ListSerializer(BookCompletionHeatmap.serializer()),
            )
        }
        val pageTurnerScores = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/page-turner-scores",
                ListSerializer(PageTurnerScore.serializer()),
            )
        }
        val completionRace = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/completion-race?year=$year",
                ListSerializer(CompletionRaceEntry.serializer()),
            )
        }
        val readingDates = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/dates",
                ListSerializer(ReadingSessionHeatmapDay.serializer()),
            )
        }
        val sessionScatter = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/session-scatter?year=$year",
                ListSerializer(ReadingSessionScatter.serializer()),
            )
        }
        val bookTimeline = async {
            authenticatedGet(
                "/api/v1/user-stats/reading/book-timeline?year=$year",
                ListSerializer(ReadingBookTimeline.serializer()),
            )
        }
        ReadingStatistics(
            year = year,
            heatmap = heatmap.await(),
            streak = streak.await(),
            favoriteDays = favoriteDays.await(),
            peakHours = peakHours.await(),
            genres = genres.await(),
            bookDistributions = distributions.await(),
            timeline = timeline.await(),
            speed = speed.await(),
            completionTimeline = completionTimeline.await(),
            completionHeatmap = completionHeatmap.await(),
            pageTurnerScores = pageTurnerScores.await(),
            completionRace = completionRace.await(),
            readingDates = readingDates.await(),
            sessionScatter = sessionScatter.await(),
            bookTimeline = bookTimeline.await(),
        )
    }

    suspend fun updateReadStatus(bookId: Long, status: String) {
        val body = buildJsonObject {
            put("bookIds", json.encodeToJsonElement(listOf(bookId)))
            put("status", status)
        }
        authenticatedRequest("/api/v1/books/status", method = "POST", body = body)
    }

    suspend fun updatePersonalRating(bookId: Long, rating: Int) {
        val body = buildJsonObject {
            put("ids", json.encodeToJsonElement(listOf(bookId)))
            put("rating", rating.coerceIn(1, 5))
        }
        authenticatedRequest("/api/v1/books/personal-rating", method = "PUT", body = body)
    }

    suspend fun updateProgress(bookId: Long, fileId: Long, progressPercent: Float) {
        val body = buildJsonObject {
            put("bookId", bookId)
            put("fileProgress", buildJsonObject {
                put("bookFileId", fileId)
                put("progressPercent", progressPercent.coerceIn(0f, 100f))
            })
        }
        authenticatedRequest("/api/v1/books/progress", method = "POST", body = body)
    }

    suspend fun updateEpubProgress(position: EpubPosition) {
        authenticatedRequest(
            "/api/v1/books/progress",
            method = "POST",
            body = position.toBookLoreProgressPayload(),
        )
    }

    suspend fun updatePdfProgress(position: PdfPosition) {
        authenticatedRequest(
            "/api/v1/books/progress",
            method = "POST",
            body = position.toBookLoreProgressPayload(),
        )
    }

    /** Reports a finished reading session so it accumulates in the server's reading records. */
    suspend fun recordReadingSession(report: ReadingSessionReport) {
        val body = buildJsonObject {
            put("bookId", report.bookId)
            report.bookType?.takeIf(String::isNotBlank)?.let { put("bookType", it) }
            put("startTime", Instant.ofEpochMilli(report.startTimeMs).toString())
            put("endTime", Instant.ofEpochMilli(report.endTimeMs).toString())
            put("durationSeconds", report.durationSeconds)
            put("durationFormatted", report.durationSeconds.toDurationFormatted())
            report.startProgressPercent?.let { put("startProgress", it.roundedTo2Decimals()) }
            report.endProgressPercent?.let { put("endProgress", it.roundedTo2Decimals()) }
            report.progressDeltaPercent?.let { put("progressDelta", it.roundedTo2Decimals()) }
            report.startLocation?.takeIf(String::isNotBlank)?.let { put("startLocation", it) }
            report.endLocation?.takeIf(String::isNotBlank)?.let { put("endLocation", it) }
        }
        authenticatedRequest("/api/v1/reading-sessions", method = "POST", body = body)
    }

    suspend fun annotations(bookId: Long): List<Annotation> =
        authenticatedGet("/api/v1/annotations/book/$bookId", ListSerializer(Annotation.serializer()))

    suspend fun createAnnotation(request: CreateAnnotationRequest): Annotation =
        authenticatedRequest("/api/v1/annotations", method = "POST", body = request.toJson())
            .decode(Annotation.serializer())

    suspend fun updateAnnotation(annotationId: Long, request: UpdateAnnotationRequest): Annotation =
        authenticatedRequest("/api/v1/annotations/$annotationId", method = "PUT", body = request.toJson())
            .decode(Annotation.serializer())

    suspend fun deleteAnnotation(annotationId: Long) {
        authenticatedRequest("/api/v1/annotations/$annotationId", method = "DELETE")
    }

    /** Assigns/unassigns a book to shelves in one request. */
    suspend fun assignShelves(bookId: Long, shelvesToAssign: Set<Long>, shelvesToUnassign: Set<Long>) {
        val body = buildJsonObject {
            put("bookIds", json.encodeToJsonElement(listOf(bookId)))
            put("shelvesToAssign", json.encodeToJsonElement(shelvesToAssign.toList()))
            put("shelvesToUnassign", json.encodeToJsonElement(shelvesToUnassign.toList()))
        }
        authenticatedRequest("/api/v1/books/shelves", method = "POST", body = body)
    }

    suspend fun downloadEpub(
        bookId: Long,
        bookType: String,
        destination: File,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ): File {
        return downloadBook(bookId, bookType, destination, onProgress)
    }

    suspend fun downloadPdf(
        bookId: Long,
        bookType: String,
        destination: File,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ): File {
        return downloadBook(bookId, bookType, destination, onProgress)
    }

    /**
     * Returns the byte length advertised by the content endpoint. A one-byte range request is
     * used to verify range support without downloading a non-range server's full response.
     */
    suspend fun bookContentLength(bookId: Long, bookType: String): Long {
        val response = probeBookRangeResponse(bookId, bookType)
        if (!supportsByteRangeStreaming(response.code, response.totalBytes)) {
            throw ApiException(response.code, "Book server does not support byte-range streaming")
        }
        return requireNotNull(response.totalBytes)
    }

    /** Reads a byte range from a book while applying the same session-refresh behavior as downloads. */
    suspend fun readBookRange(bookId: Long, bookType: String, range: LongRange): ByteArray {
        require(range.first >= 0 && range.last >= range.first) { "Invalid book content range" }
        return readBookRangeResponse(bookId, bookType, range).bytes
    }

    /**
     * URL used by readers which support HTTP range requests directly, such as Readium's EPUB
     * parser. Authentication is intentionally supplied as a request header, not a query token.
     */
    fun bookContentUrl(bookId: Long, bookType: String): String? = sessionStore.read()?.let { session ->
        bookContentUrl(session, bookId, bookType).toString()
    }

    suspend fun downloadCover(bookId: Long, destination: File): File {
        requireOnlineMode()
        val session = sessionStore.read() ?: throw ApiException(401, "No active session")
        val initial = downloadCover(session, bookId, destination)
        if (initial.code != 401) {
            initial.requireSuccess()
            return destination
        }

        val refreshed = refreshSession(session)
        downloadCover(refreshed, bookId, destination).requireSuccess()
        return destination
    }

    private suspend fun downloadBook(
        bookId: Long,
        bookType: String,
        destination: File,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit,
    ): File {
        requireOnlineMode()
        val session = sessionStore.read() ?: throw ApiException(401, "No active session")
        val initial = downloadBookContent(session, bookId, bookType, destination, onProgress)
        if (initial.code != 401) {
            initial.requireSuccess()
            return destination
        }

        val refreshed = refreshSession(session)
        downloadBookContent(refreshed, bookId, bookType, destination, onProgress).requireSuccess()
        return destination
    }

    private suspend fun readBookRangeResponse(
        bookId: Long,
        bookType: String,
        range: LongRange,
    ): BookContentRangeResponse {
        requireOnlineMode()
        val session = sessionStore.read() ?: throw ApiException(401, "No active session")
        val initial = readBookRange(session, bookId, bookType, range)
        if (initial.code != 401) return initial.requireSuccess()

        val refreshed = refreshSession(session)
        return readBookRange(refreshed, bookId, bookType, range).requireSuccess()
    }

    /** Sends a one-byte range request but closes the response before consuming its body. */
    private suspend fun probeBookRangeResponse(bookId: Long, bookType: String): BookContentRangeProbe {
        requireOnlineMode()
        val session = sessionStore.read() ?: throw ApiException(401, "No active session")
        val initial = probeBookRange(session, bookId, bookType)
        if (initial.code != 401) return initial.requireSuccess()

        val refreshed = refreshSession(session)
        return probeBookRange(refreshed, bookId, bookType).requireSuccess()
    }

    fun coverUrl(bookId: Long): String? = sessionStore.read()?.let { session ->
        session.baseUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegments("api/v1/media/book/$bookId/cover")
            // BookLore's browser media URLs authenticate with this query parameter.
            ?.addQueryParameter("token", session.accessToken)
            ?.build()
            ?.toString()
    }

    private fun downloadBookContent(
        session: Session,
        bookId: Long,
        bookType: String,
        destination: File,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ): NetworkResponse {
        val url = bookContentUrl(session, bookId, bookType)
        val temporary = File(destination.parentFile, "${destination.name}.part")
        temporary.delete()
        destination.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/pdf, application/epub+zip, application/octet-stream")
            .header("Authorization", "Bearer ${session.accessToken}")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.code !in 200..299) {
                    NetworkResponse(response.code, response.body?.string().orEmpty().take(512))
                } else {
                    val responseBody = response.body ?: return@use NetworkResponse(502, "Missing book content")
                    val totalBytes = responseBody.contentLength().takeIf { it > 0L }
                    responseBody.byteStream().use { input ->
                        temporary.outputStream().buffered().use { output ->
                            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                            var bytesRead = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                bytesRead += count
                                onProgress(bytesRead, totalBytes)
                            }
                        }
                    }
                    if (!temporary.renameTo(destination)) {
                        temporary.copyTo(destination, overwrite = true)
                        temporary.delete()
                    }
                    NetworkResponse(response.code, "")
                }
            }
        } catch (error: IOException) {
            temporary.delete()
            throw NetworkException(error)
        }
    }

    private fun readBookRange(
        session: Session,
        bookId: Long,
        bookType: String,
        range: LongRange,
    ): BookContentRangeResponse {
        val request = Request.Builder()
            .url(bookContentUrl(session, bookId, bookType))
            .header("Accept", "application/pdf, application/epub+zip, application/octet-stream")
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Range", "bytes=${range.first}-${range.last}")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body
                val bytes = if (response.code == 206) responseBody?.bytes() ?: ByteArray(0) else ByteArray(0)
                val totalBytes = response.header("Content-Range")
                    ?.let(::contentRangeTotalBytes)
                BookContentRangeResponse(
                    code = response.code,
                    bytes = bytes,
                    totalBytes = totalBytes,
                    body = if (response.code in 200..299) "" else responseBody?.string().orEmpty().take(512),
                )
            }
        } catch (error: IOException) {
            throw NetworkException(error)
        }
    }

    private fun probeBookRange(session: Session, bookId: Long, bookType: String): BookContentRangeProbe {
        val request = Request.Builder()
            .url(bookContentUrl(session, bookId, bookType))
            .header("Accept", "application/pdf, application/epub+zip, application/octet-stream")
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Range", "bytes=0-0")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                BookContentRangeProbe(
                    code = response.code,
                    totalBytes = response.header("Content-Range")?.let(::contentRangeTotalBytes),
                    body = if (response.code in 200..299) "" else response.body?.string().orEmpty().take(512),
                )
            }
        } catch (error: IOException) {
            throw NetworkException(error)
        }
    }

    private fun bookContentUrl(session: Session, bookId: Long, bookType: String) =
        session.baseUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegments("api/v1/books/$bookId/content")
            ?.addQueryParameter("bookType", bookType)
            ?.build()
            ?: throw IllegalArgumentException("Invalid server URL")

    private fun downloadCover(session: Session, bookId: Long, destination: File): NetworkResponse {
        val baseUrl = session.baseUrl.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid server URL")
        val url = baseUrl.newBuilder()
            .addPathSegments("api/v1/media/book/$bookId/cover")
            .addQueryParameter("token", session.accessToken)
            .build()
        val temporary = File(destination.parentFile, "${destination.name}.part")
        temporary.delete()
        destination.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "image/*")
            .header("Authorization", "Bearer ${session.accessToken}")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.code !in 200..299) {
                    NetworkResponse(response.code, response.body?.string().orEmpty().take(512))
                } else {
                    val responseBody = response.body ?: return@use NetworkResponse(502, "Missing cover content")
                    responseBody.byteStream().use { input ->
                        temporary.outputStream().buffered().use { output -> input.copyTo(output) }
                    }
                    if (!temporary.renameTo(destination)) {
                        temporary.copyTo(destination, overwrite = true)
                        temporary.delete()
                    }
                    NetworkResponse(response.code, "")
                }
            }
        } catch (error: IOException) {
            temporary.delete()
            throw NetworkException(error)
        }
    }

    private suspend fun <T> authenticatedGet(path: String, serializer: KSerializer<T>): T =
        authenticatedRequest(path).decode(serializer)

    private suspend fun authenticatedRequest(
        path: String,
        method: String = "GET",
        body: JsonObject? = null,
    ): NetworkResponse {
        requireOnlineMode()
        val session = sessionStore.read() ?: throw ApiException(401, "No active session")
        val initial = request(session.baseUrl, path, method, body, authenticated = true, accessToken = session.accessToken)
        if (initial.code != 401) return initial.requireSuccess()

        val refreshed = refreshSession(session)
        val retried = request(refreshed.baseUrl, path, method, body, authenticated = true, accessToken = refreshed.accessToken)
        return retried.requireSuccess()
    }

    private suspend fun refreshSession(previous: Session): Session = refreshMutex.withLock {
        val latest = sessionStore.read() ?: throw ApiException(401, "Session expired")
        if (latest.refreshToken != previous.refreshToken) return@withLock latest
        val refreshBody = buildJsonObject { put("refreshToken", latest.refreshToken) }
        val tokens = request(latest.baseUrl, "/api/v1/auth/refresh", "POST", refreshBody, authenticated = false)
            .requireSuccess()
            .decode(TokenResponse.serializer())
        Session(latest.baseUrl, tokens.accessToken, tokens.refreshToken).also(sessionStore::write)
    }

    private fun request(
        baseUrl: String,
        path: String,
        method: String = "GET",
        body: JsonObject? = null,
        authenticated: Boolean,
        accessToken: String? = null,
    ): NetworkResponse {
        requireOnlineMode()
        val url = "$baseUrl$path".toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid request URL")
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        if (authenticated && !accessToken.isNullOrBlank()) builder.header("Authorization", "Bearer $accessToken")
        val requestBody = body?.let { json.encodeToString(it).toRequestBody(JSON_MEDIA_TYPE) }
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: EMPTY_JSON_BODY)
            else -> builder.method(
                method,
                requestBody ?: if (method == "PUT") EMPTY_JSON_BODY else null,
            )
        }
        return try {
            client.newCall(builder.build()).execute().use { response -> response.toNetworkResponse() }
        } catch (error: IOException) {
            throw NetworkException(error)
        }
    }

    private fun requireOnlineMode() {
        if (sessionStore.readLocalMode()) throw NetworkException(IOException("Local mode is enabled"))
    }

    private fun <T> NetworkResponse.decode(serializer: KSerializer<T>): T = try {
        json.decodeFromString(serializer, body)
    } catch (error: SerializationException) {
        throw ApiException(code, "Unexpected server response", error)
    }

    private fun Response.toNetworkResponse(): NetworkResponse = NetworkResponse(
        code = code,
        body = body?.string().orEmpty(),
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_JSON_BODY = "{}".toRequestBody(JSON_MEDIA_TYPE)
        private const val DOWNLOAD_BUFFER_SIZE = 32 * 1024

        // One shared client for every BookLoreApi instance so cover downloads
        // reuse the same connection pool and thread pools instead of paying
        // per-instance OkHttpClient costs in the book grid.
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        fun normalizeBaseUrl(rawUrl: String, allowHttp: Boolean): String {
            val candidate = rawUrl.trim().removeSuffix("/")
            val parsed = candidate.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid server URL")
            require(parsed.username.isEmpty() && parsed.password.isEmpty()) { "Credentials are not allowed in server URLs" }
            require(parsed.encodedPath == "/" || parsed.encodedPath.isEmpty()) { "Server URL must not include a path" }
            require(parsed.scheme == "https" || (allowHttp && parsed.scheme == "http")) { "HTTPS is required" }
            return parsed.toString().removeSuffix("/")
        }
    }
}

data class NetworkResponse(val code: Int, val body: String) {
    fun requireSuccess(): NetworkResponse {
        if (code !in 200..299) throw ApiException(code, body.take(512))
        return this
    }
}

private data class BookContentRangeResponse(
    val code: Int,
    val bytes: ByteArray,
    val totalBytes: Long?,
    val body: String,
) {
    fun requireSuccess(): BookContentRangeResponse {
        if (code !in 200..299) throw ApiException(code, body.take(512))
        return this
    }
}

private data class BookContentRangeProbe(
    val code: Int,
    val totalBytes: Long?,
    val body: String,
) {
    fun requireSuccess(): BookContentRangeProbe {
        if (code !in 200..299) throw ApiException(code, body.take(512))
        return this
    }
}

/** Extracts the total resource size from a RFC 7233 Content-Range response header. */
internal fun contentRangeTotalBytes(header: String): Long? = CONTENT_RANGE_TOTAL.find(header.trim())
    ?.groupValues
    ?.get(1)
    ?.toLongOrNull()

private val CONTENT_RANGE_TOTAL = Regex("^bytes\\s+\\d+-\\d+/(\\d+)$", RegexOption.IGNORE_CASE)

internal fun supportsByteRangeStreaming(responseCode: Int, totalBytes: Long?): Boolean =
    responseCode == 206 && totalBytes != null && totalBytes > 0L

class ApiException(val statusCode: Int, message: String, cause: Throwable? = null) : Exception(message, cause)

class NetworkException(cause: IOException) : Exception(cause)

internal fun EpubPosition.toBookLoreProgressPayload(): JsonObject = buildJsonObject {
    put("bookId", bookId)
    put("fileProgress", buildJsonObject {
        put("bookFileId", bookFileId)
        cfi?.takeIf(String::isNotBlank)?.let { put("positionData", it) }
        href?.takeIf(String::isNotBlank)?.let { put("positionHref", it) }
        put("progressPercent", progressPercent.coerceIn(0f, 100f))
    })
}

internal fun PdfPosition.toBookLoreProgressPayload(): JsonObject = buildJsonObject {
    put("bookId", bookId)
    put("fileProgress", buildJsonObject {
        put("bookFileId", bookFileId)
        put("positionData", page.coerceAtLeast(1).toString())
        put("progressPercent", progressPercent.coerceIn(0f, 100f))
    })
}

private fun Float.roundedTo2Decimals(): Double = kotlin.math.round(this * 100.0) / 100.0

/** Matches the web reader's "Xh Ym Zs" / "Xm Ys" / "Xs" duration labels. */
private fun Int.toDurationFormatted(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun CreateAnnotationRequest.toJson(): JsonObject = buildJsonObject {
    put("bookId", bookId)
    put("cfi", cfi)
    put("text", text)
    color?.let { put("color", it) }
    style?.let { put("style", it) }
    note?.takeIf(String::isNotBlank)?.let { put("note", it) }
    chapterTitle?.takeIf(String::isNotBlank)?.let { put("chapterTitle", it) }
}

private fun UpdateAnnotationRequest.toJson(): JsonObject = buildJsonObject {
    color?.let { put("color", it) }
    style?.let { put("style", it) }
    note?.let { put("note", it) }
}
