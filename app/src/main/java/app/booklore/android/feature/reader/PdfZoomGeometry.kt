package app.booklore.android.feature.reader

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

/** Keeps the content under the previous centroid at the current centroid after scaling. */
internal fun pdfZoomTranslation(
    translation: Offset,
    centroid: Offset,
    pan: Offset,
    scaleRatio: Float,
    origin: Offset = Offset.Zero,
): Offset = (translation - (centroid - origin)) * scaleRatio + (centroid - origin) + pan

/** A page-relative anchor survives remeasurement and multiple pointer events in one frame. */
internal data class PdfPageZoomAnchor(
    val pageIndex: Int,
    val pageFraction: Float,
) {
    fun scrollOffset(pageHeight: Int, centroidY: Float, topPadding: Int): Int =
        (pageFraction * pageHeight - centroidY + topPadding).roundToInt()
}
