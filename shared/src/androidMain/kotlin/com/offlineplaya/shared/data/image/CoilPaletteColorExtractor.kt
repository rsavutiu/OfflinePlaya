package com.offlineplaya.shared.data.image

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.offlineplaya.shared.domain.image.AlbumArtColorExtractor
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android [AlbumArtColorExtractor] that reuses the app's Coil art pipeline to
 * fetch the cover (cache → embedded → sidecar → remote — exactly what the UI
 * shows) and runs `androidx.palette` over it to pick a seed color.
 *
 * Deliberately uses the singleton [coil3.ImageLoader] installed by
 * `installTrackArtImageLoader`, so the custom `Track` fetcher + keyer (and its
 * disk cache) are in play — palette extraction is usually a cache hit and
 * costs no extra decode for art the user has already seen.
 */
internal class CoilPaletteColorExtractor(
    private val context: Context,
    private val logger: AppLogger,
) : AlbumArtColorExtractor {

    private companion object {
        const val TAG = "CoilPaletteColorExtractor"

        // Palette quantizes a downscaled copy anyway; capping the decode keeps
        // memory low and extraction fast without changing the dominant color.
        const val SAMPLE_PX = 128
    }

    override suspend fun seedColor(track: Track): Int? = withContext(Dispatchers.Default) {
        val bitmap = loadArtBitmap(track) ?: return@withContext null
        try {
            val palette = Palette.from(bitmap).generate()
            // Prefer a saturated, "defining" swatch; fall back through less
            // vivid options, finally the dominant color. material-kolor turns
            // whatever we pick into a full, legible tonal scheme, so the seed
            // only needs to capture the cover's character.
            val swatch = palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
            swatch?.rgb
        } catch (t: Throwable) {
            logger.e(TAG, "Palette extraction failed for ${track.documentUri}", t)
            null
        }
    }

    private suspend fun loadArtBitmap(track: Track): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(track) // the registered Track fetcher resolves the cover
            .allowHardware(false) // Palette must read pixels — force a software bitmap
            .size(SAMPLE_PX)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        if (result !is SuccessResult) return null
        val image = result.image
        val bitmap = (image as? BitmapImage)?.bitmap ?: return null
        // Guard against a hardware-backed bitmap slipping through (would throw
        // inside Palette): copy to a readable config if needed. HARDWARE is
        // API 26+, so the && short-circuits before referencing it on 24/25
        // (where the enum constant doesn't exist).
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    }
}
