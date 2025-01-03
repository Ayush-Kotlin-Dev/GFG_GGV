package com.ayush.geeksforgeeks.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteGenerator {
    // Default theme colors
    private val defaultColors = mapOf(
        "vibrant" to "#2E8B57",
        "darkVibrant" to "#1A5D3A",
        "lightVibrant" to "#98FB98",
        "onDarkVibrant" to "#FFFFFF",
        "dominant" to "#2E8B57",
        "muted" to "#95A5A6",
        "darkMuted" to "#2C3E50"
    )

    suspend fun convertImageUrlToBitmap(
        imageUrl: String,
        context: Context
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val imageResult = loader.execute(request)
            if (imageResult is SuccessResult) {
                (imageResult.drawable as BitmapDrawable).bitmap
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun extractColorsFromBitmap(bitmap: Bitmap): Map<String, String> = withContext(Dispatchers.Default) {
        try {
            val palette = Palette.from(bitmap).generate()

            mapOf(
                "vibrant" to parseColorSwatch(palette.vibrantSwatch),
                "darkVibrant" to parseColorSwatch(palette.darkVibrantSwatch),
                "onDarkVibrant" to parseBodyColor(palette.darkVibrantSwatch?.bodyTextColor)
            )
        } catch (e: Exception) {
            mapOf(
                "vibrant" to "#2E8B57",
                "darkVibrant" to "#1A5D3A",
                "onDarkVibrant" to "#FFFFFF"
            )
        }
    }

    private fun parseColorSwatch(swatch: Palette.Swatch?): String {
        return swatch?.let { "#${Integer.toHexString(it.rgb).substring(2)}" } ?: "#2E8B57"
    }

    private fun parseBodyColor(color: Int?): String {
        return color?.let { "#${Integer.toHexString(it).substring(2)}" } ?: "#FFFFFF"
    }
}

fun parserColor(colorString: String): Int {
    return Color.parseColor(colorString)
}