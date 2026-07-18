package chat.schildi.revenge.compose.media

import androidx.compose.ui.graphics.Color
import coil3.Image

internal expect fun extractDominantColor(image: Image): Color?

internal fun dominantOpaqueColor(pixels: IntArray): Color? {
    val colors = mutableMapOf<Int, Int>()
    val hueGroups = Array(HUE_BUCKET_COUNT) { HueGroup() }
    pixels.forEach { argb ->
        if ((argb ushr 24) >= 0x80) {
            // Quantizing prevents minor antialiasing and compression changes from splitting one color.
            val quantized = argb and 0xffe0e0e0.toInt()
            colors[quantized] = (colors[quantized] ?: 0) + 1

            val red = (argb ushr 16) and 0xff
            val green = (argb ushr 8) and 0xff
            val blue = argb and 0xff
            val max = maxOf(red, green, blue)
            val min = minOf(red, green, blue)
            val chroma = max - min
            if (chroma >= MIN_CHROMA) {
                // A hue bucket keeps shaded or antialiased variants of one brand color together.
                val hue = when (max) {
                    red -> (green - blue) / chroma.toFloat()
                    green -> (blue - red) / chroma.toFloat() + 2f
                    else -> (red - green) / chroma.toFloat() + 4f
                }.let { if (it < 0f) it + 6f else it }
                hueGroups[(hue * HUE_BUCKET_COUNT / 6).toInt() % HUE_BUCKET_COUNT].add(red, green, blue)
            }
        }
    }
    val largestHueGroup = hueGroups.maxOf { it.count }
    if (largestHueGroup > 0) {
        // Decoders can vary slightly when resampling evenly colored logos, so resolve near ties predictably.
        return hueGroups.withIndex()
            .filter { it.value.count >= largestHueGroup * NEAR_TIE_COUNT_RATIO }
            .maxWithOrNull(
                compareBy<IndexedValue<HueGroup>> { it.value.color().relativeLuminance() }
                    .thenBy { -it.index }
            )
            ?.value
            ?.color()
    }
    return colors.maxByOrNull { it.value }?.key?.let(::Color)
}

private const val HUE_BUCKET_COUNT = 6
private const val MIN_CHROMA = 32
private const val NEAR_TIE_COUNT_RATIO = 0.9f

private class HueGroup {
    var count = 0
    private var red = 0L
    private var green = 0L
    private var blue = 0L

    fun add(red: Int, green: Int, blue: Int) {
        count++
        this.red += red
        this.green += green
        this.blue += blue
    }

    fun color() = Color(
        0xff000000.toInt() or
            ((red / count).toInt() shl 16) or
            ((green / count).toInt() shl 8) or
            (blue / count).toInt()
    )
}

internal fun Color.contrastRatio(other: Color): Float {
    val first = relativeLuminance()
    val second = other.relativeLuminance()
    return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
}

internal fun Color.relativeLuminance(): Float {
    fun channelLuminance(channel: Float): Float =
        if (channel <= 0.04045f) channel / 12.92f else ((channel + 0.055f) / 1.055f).let { it * it * it }

    return 0.2126f * channelLuminance(red) +
        0.7152f * channelLuminance(green) +
        0.0722f * channelLuminance(blue)
}

internal fun Color.compositeOver(background: Color): Color {
    val outputAlpha = alpha + background.alpha * (1f - alpha)
    if (outputAlpha == 0f) return Color.Transparent
    return Color(
        red = (red * alpha + background.red * background.alpha * (1f - alpha)) / outputAlpha,
        green = (green * alpha + background.green * background.alpha * (1f - alpha)) / outputAlpha,
        blue = (blue * alpha + background.blue * background.alpha * (1f - alpha)) / outputAlpha,
        alpha = outputAlpha,
    )
}
