package app.booklore.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthEnvelope(
    val code: Int? = null,
    val message: String? = null,
    val data: HealthData? = null,
)

@Serializable
data class HealthData(
    val status: String? = null,
    val version: String? = null,
)

@Serializable
data class PublicSettings(
    val oidcEnabled: Boolean = false,
    val remoteAuthEnabled: Boolean = false,
    val oidcForceOnlyMode: Boolean = false,
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val isDefaultPassword: String? = null,
)

@Serializable
data class Library(
    val id: Long,
    val name: String,
    val icon: String? = null,
    val iconType: String? = null,
)

@Serializable
data class Shelf(
    val id: Long,
    val name: String,
    val icon: String? = null,
    val iconType: String? = null,
    val bookCount: Int = 0,
    val publicShelf: Boolean = false,
)

@Serializable
data class Book(
    val id: Long,
    val libraryId: Long? = null,
    val libraryName: String? = null,
    val title: String? = null,
    // Some BookLore server versions expose series data on the book as well as metadata.
    val seriesName: String? = null,
    val seriesNumber: Float? = null,
    val metadata: BookMetadata? = null,
    val primaryFile: BookFile? = null,
    val lastReadTime: String? = null,
    val addedOn: String? = null,
    val dateFinished: String? = null,
    val readStatus: String? = null,
    val personalRating: Int? = null,
    val shelves: Set<Shelf> = emptySet(),
    val epubProgress: EpubProgress? = null,
    val pdfProgress: PdfProgress? = null,
    val isPhysical: Boolean? = false,
)

@Serializable
data class BookMetadata(
    val title: String? = null,
    val subtitle: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val authors: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val language: String? = null,
    val pageCount: Int? = null,
    val seriesName: String? = null,
    val series: String? = null,
    val seriesTitle: String? = null,
    val seriesNumber: Float? = null,
    val seriesTotal: Int? = null,
    val rating: Double? = null,
    val goodreadsRating: Double? = null,
    val amazonRating: Double? = null,
    val hardcoverRating: Double? = null,
)

@Serializable
data class BookFile(
    val id: Long? = null,
    val fileName: String? = null,
    val bookType: String? = null,
    val extension: String? = null,
    val fileSizeKb: Long? = null,
)

@Serializable
data class EpubProgress(
    val cfi: String? = null,
    val href: String? = null,
    val percentage: Float? = null,
)

@Serializable
data class PdfProgress(
    val page: Int? = null,
    val percentage: Float? = null,
)

data class ServerConnection(
    val baseUrl: String,
    val version: String? = null,
    val settings: PublicSettings = PublicSettings(),
)

data class Session(
    val baseUrl: String,
    val accessToken: String,
    val refreshToken: String,
)

/** A remembered server + account pair that allows one-tap sign-in from the login screen. */
@Serializable
data class SavedLogin(
    val baseUrl: String,
    val username: String,
    val password: String? = null,
)

enum class ThemeColorOption {
    FOREST,
    OCEAN,
    CORAL,
}

enum class FontOption {
    SANS,
    SERIF,
    MONOSPACE,
}

data class DisplaySettings(
    val themeColor: ThemeColorOption = ThemeColorOption.FOREST,
    val font: FontOption = FontOption.SANS,
    val fontScale: Float = 1f,
)

enum class EpubReaderTheme {
    SYSTEM,
    LIGHT,
    SEPIA,
    DARK,
}

data class EpubReaderSettings(
    val theme: EpubReaderTheme = EpubReaderTheme.SYSTEM,
    val fontScale: Float = 1f,
    val font: FontOption = FontOption.SANS,
    val pageMargins: Float = 1f,
)

enum class PdfReadingMode {
    PAGINATED,
    CONTINUOUS,
}

data class PdfReaderSettings(
    val readingMode: PdfReadingMode = PdfReadingMode.PAGINATED,
)

@Serializable
data class EpubPosition(
    val bookId: Long,
    val bookFileId: Long,
    val cfi: String? = null,
    val href: String? = null,
    // Reading progression within the current chapter (0f..1f). The navigator
    // uses it to restore the exact page inside the chapter on reopen.
    val progression: Float? = null,
    val progressPercent: Float = 0f,
)

@Serializable
data class PdfPosition(
    val bookId: Long,
    val bookFileId: Long,
    val page: Int = 1,
    val progressPercent: Float = 0f,
)

/** A highlight/underline annotation synced with the BookLore server. */
@Serializable
data class Annotation(
    val id: Long,
    val userId: Long? = null,
    val bookId: Long,
    val cfi: String? = null,
    val text: String? = null,
    val color: String? = null,
    val style: String? = null,
    val note: String? = null,
    val chapterTitle: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CreateAnnotationRequest(
    val bookId: Long,
    val cfi: String,
    val text: String,
    val color: String? = null,
    val style: String? = null,
    val note: String? = null,
    val chapterTitle: String? = null,
)

@Serializable
data class UpdateAnnotationRequest(
    val color: String? = null,
    val style: String? = null,
    val note: String? = null,
)

/** Queued update carrying the target annotation id alongside the payload. */
@Serializable
data class PendingAnnotationUpdate(
    val annotationId: Long,
    val request: UpdateAnnotationRequest,
)

/** Default highlight colors offered in the annotation editor. */
val ANNOTATION_COLORS: List<String> = listOf(
    "#FFF500",
    "#7CE38B",
    "#63C8F5",
    "#FF80AB",
    "#B39DDB",
)

const val ANNOTATION_DEFAULT_COLOR = "#FFF500"
const val ANNOTATION_STYLE_HIGHLIGHT = "highlight"
const val ANNOTATION_STYLE_UNDERLINE = "underline"

/** Converts a hex color like "#FFF500" to an ARGB int (opaque alpha). */
fun String.toArgbColorOrNull(): Int? = runCatching {
    if (length == 7 && startsWith("#")) {
        (0xFF shl 24) or substring(1).toLong(16).toInt()
    } else {
        null
    }
}.getOrNull()

/**
 * A finished reading session reported to the server's reading records,
 * mirroring the payload of the BookLore web reader.
 */
@Serializable
data class ReadingSessionReport(
    val bookId: Long,
    val bookType: String? = null,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSeconds: Int,
    val startLocation: String? = null,
    val endLocation: String? = null,
    val startProgressPercent: Float? = null,
    val endProgressPercent: Float? = null,
) {
    val progressDeltaPercent: Float?
        get() = if (startProgressPercent != null && endProgressPercent != null) {
            endProgressPercent - startProgressPercent
        } else {
            null
    }
}

/** Aggregated reading data returned by BookLore's user statistics endpoints. */
data class ReadingStatistics(
    val year: Int,
    val heatmap: List<ReadingSessionHeatmapDay>,
    val streak: ReadingStreak,
    val favoriteDays: List<FavoriteReadingDay>,
    val peakHours: List<PeakReadingHour>,
    val genres: List<ReadingGenreStatistic>,
    val bookDistributions: ReadingBookDistributions,
    val timeline: List<ReadingSessionTimeline>,
    val speed: List<ReadingSpeed>,
    val completionTimeline: List<ReadingCompletionTimeline>,
    val completionHeatmap: List<BookCompletionHeatmap>,
    val pageTurnerScores: List<PageTurnerScore>,
    val completionRace: List<CompletionRaceEntry>,
    val readingDates: List<ReadingSessionHeatmapDay>,
    val sessionScatter: List<ReadingSessionScatter>,
    val bookTimeline: List<ReadingBookTimeline>,
)

@Serializable
data class ReadingSessionHeatmapDay(
    val date: String,
    val count: Long = 0,
)

@Serializable
data class ReadingStreak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalReadingDays: Int = 0,
)

@Serializable
data class FavoriteReadingDay(
    val dayOfWeek: Int = 0,
    val dayName: String = "",
    val sessionCount: Long = 0,
    val totalDurationSeconds: Long = 0,
)

@Serializable
data class PeakReadingHour(
    val hourOfDay: Int = 0,
    val sessionCount: Long = 0,
    val totalDurationSeconds: Long = 0,
)

@Serializable
data class ReadingGenreStatistic(
    val genre: String = "",
    val bookCount: Long = 0,
    val totalSessions: Long = 0,
    val totalDurationSeconds: Long = 0,
    val averageSessionsPerBook: Double = 0.0,
)

@Serializable
data class ReadingBookDistributions(
    val ratingDistribution: List<ReadingRatingBucket> = emptyList(),
    val progressDistribution: List<ReadingProgressBucket> = emptyList(),
    val statusDistribution: List<ReadingStatusBucket> = emptyList(),
)

@Serializable
data class ReadingRatingBucket(
    val rating: Int = 0,
    val count: Long = 0,
)

@Serializable
data class ReadingProgressBucket(
    val range: String = "",
    val min: Int = 0,
    val max: Int = 0,
    val count: Long = 0,
)

@Serializable
data class ReadingStatusBucket(
    val status: String = "",
    val count: Long = 0,
)

@Serializable
data class ReadingSessionTimeline(
    val bookId: Long = 0,
    val bookTitle: String = "",
    val bookType: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val totalSessions: Long = 0,
    val totalDurationSeconds: Long = 0,
)

@Serializable
data class ReadingSpeed(
    val date: String = "",
    val avgProgressPerMinute: Double = 0.0,
    val totalSessions: Int = 0,
)

@Serializable
data class ReadingCompletionTimeline(
    val year: Int = 0,
    val month: Int = 0,
    val totalBooks: Long = 0,
    val statusBreakdown: Map<String, Long> = emptyMap(),
    val finishedBooks: Long = 0,
    val completionRate: Double = 0.0,
)

@Serializable
data class BookCompletionHeatmap(
    val year: Int = 0,
    val month: Int = 0,
    val count: Long = 0,
)

@Serializable
data class PageTurnerScore(
    val bookId: Long = 0,
    val bookTitle: String = "",
    val categories: List<String> = emptyList(),
    val pageCount: Int = 0,
    val personalRating: Int? = null,
    val gripScore: Int = 0,
    val totalSessions: Long = 0,
    val avgSessionDurationSeconds: Double = 0.0,
    val sessionAcceleration: Double = 0.0,
    val gapReduction: Double = 0.0,
    val finishBurst: Boolean = false,
)

@Serializable
data class CompletionRaceEntry(
    val bookId: Long = 0,
    val bookTitle: String = "",
    val sessionDate: String = "",
    val endProgress: Float = 0f,
)

@Serializable
data class ReadingSessionScatter(
    val hourOfDay: Double = 0.0,
    val durationMinutes: Double = 0.0,
    val dayOfWeek: Int = 0,
)

@Serializable
data class ReadingBookTimeline(
    val bookId: Long = 0,
    val title: String = "",
    val pageCount: Int = 0,
    val firstSessionDate: String = "",
    val lastSessionDate: String = "",
    val totalSessions: Int = 0,
    val totalDurationSeconds: Long = 0,
    val maxProgress: Double = 0.0,
    val readStatus: String = "",
)

fun Book.displayTitle(): String = title ?: metadata?.title ?: "Untitled"

fun Book.displayAuthors(): String = metadata?.authors?.filter(String::isNotBlank)?.joinToString() ?: ""

fun Book.formatName(): String = primaryFile?.bookType ?: primaryFile?.extension?.uppercase() ?: "BOOK"

fun Book.supportsEpubReader(): Boolean = primaryFile?.bookType.equals("EPUB", ignoreCase = true) ||
    primaryFile?.extension.equals("epub", ignoreCase = true)

fun Book.supportsPdfReader(): Boolean = primaryFile?.bookType.equals("PDF", ignoreCase = true) ||
    primaryFile?.extension.equals("pdf", ignoreCase = true)
