package app.booklore.android.feature.reader

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfZoomGeometryTest {
    @Test
    fun `continuous zoom keeps an off-center content point under the fingers`() {
        assertContentFollowsFingers(
            translation = Offset.Zero,
            origin = Offset.Zero,
            centroid = Offset(780f, 950f),
            pan = Offset.Zero,
            oldScale = 1f,
            newScale = 2f,
        )
    }

    @Test
    fun `paginated zoom and pan use the same viewport coordinates`() {
        assertContentFollowsFingers(
            translation = Offset(-180f, 120f),
            origin = Offset(540f, 960f),
            centroid = Offset(730f, 1220f),
            pan = Offset(35f, -48f),
            oldScale = 1.5f,
            newScale = 2.7f,
        )
    }

    @Test
    fun `pan at the maximum zoom moves by the finger distance`() {
        val translation = Offset(-400f, 200f)
        val pan = Offset(37f, -61f)

        assertOffsetEquals(
            translation + pan,
            pdfZoomTranslation(translation, Offset(650f, 900f), pan, scaleRatio = 1f),
        )
    }

    @Test
    fun `zooming out keeps the existing focal point`() {
        assertContentFollowsFingers(
            translation = Offset(-700f, -520f),
            origin = Offset.Zero,
            centroid = Offset(330f, 580f),
            pan = Offset(-22f, 14f),
            oldScale = 3f,
            newScale = 1.4f,
        )
    }

    @Test
    fun `vertical anchor preserves the touched point on a later page with reader padding`() {
        val pageIndex = 17
        val oldPageHeight = 1500
        val oldPageTop = -320
        val topPadding = 64
        val centroidY = 870f
        val anchor = PdfPageZoomAnchor(
            pageIndex,
            (centroidY - topPadding - oldPageTop) / oldPageHeight,
        )

        // Several events may arrive before layout measures the final page height.
        for (newHeight in listOf(1800, 2600, 3000, 2200, 1500)) {
            val movedCentroidY = centroidY + 43f
            val scrollOffset = anchor.scrollOffset(newHeight, movedCentroidY, topPadding)
            val contentY = topPadding - scrollOffset + anchor.pageFraction * newHeight

            assertEquals(pageIndex, anchor.pageIndex)
            assertEquals(movedCentroidY, contentY, 0.51f)
        }
    }

    @Test
    fun `vertical anchor also works with hidden reader controls`() {
        val anchor = PdfPageZoomAnchor(pageIndex = 0, pageFraction = 0.6f)
        val offset = anchor.scrollOffset(pageHeight = 2400, centroidY = 720f, topPadding = 0)

        assertEquals(720f, anchor.pageFraction * 2400 - offset, 0.51f)
    }

    private fun assertContentFollowsFingers(
        translation: Offset,
        origin: Offset,
        centroid: Offset,
        pan: Offset,
        oldScale: Float,
        newScale: Float,
    ) {
        val contentPoint = (centroid - origin - translation) / oldScale
        val newTranslation = pdfZoomTranslation(translation, centroid, pan, newScale / oldScale, origin)

        assertOffsetEquals(centroid + pan, origin + contentPoint * newScale + newTranslation)
    }

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.001f)
        assertEquals(expected.y, actual.y, 0.001f)
    }
}
