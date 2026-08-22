package app.booklore.android.core.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingStatisticsContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes BookLore user statistics responses`() {
        val heatmap = json.decodeFromString<List<ReadingSessionHeatmapDay>>(
            """[{"date":"2026-08-22","count":3}]""",
        )
        val streak = json.decodeFromString<ReadingStreak>(
            """{"currentStreak":4,"longestStreak":12,"totalReadingDays":51,"last52Weeks":[]}""",
        )
        val distributions = json.decodeFromString<ReadingBookDistributions>(
            """{
                "ratingDistribution":[{"rating":5,"count":8}],
                "progressDistribution":[{"range":"0-25%","min":0,"max":25,"count":2}],
                "statusDistribution":[{"status":"READING","count":3}]
            }""",
        )

        assertEquals("2026-08-22", heatmap.single().date)
        assertEquals(3, heatmap.single().count)
        assertEquals(4, streak.currentStreak)
        assertEquals(12, streak.longestStreak)
        assertEquals(51, streak.totalReadingDays)
        assertEquals("READING", distributions.statusDistribution.single().status)
        assertTrue(distributions.progressDistribution.isNotEmpty())
    }
}
