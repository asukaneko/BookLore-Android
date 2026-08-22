package app.booklore.android.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplaySettingsTest {
    @Test
    fun `uses the current neutral defaults`() {
        val settings = DisplaySettings()

        assertEquals(ThemeColorOption.FOREST, settings.themeColor)
        assertEquals(FontOption.SANS, settings.font)
        assertEquals(1f, settings.fontScale)
    }

    @Test
    fun `preserves independently selected appearance values`() {
        val settings = DisplaySettings(
            themeColor = ThemeColorOption.OCEAN,
            font = FontOption.SERIF,
            fontScale = 1.2f,
        )

        assertEquals(ThemeColorOption.OCEAN, settings.themeColor)
        assertEquals(FontOption.SERIF, settings.font)
        assertEquals(1.2f, settings.fontScale)
    }
}
