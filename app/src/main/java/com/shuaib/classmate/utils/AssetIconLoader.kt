package com.shuaib.classmate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView

/**
 * Utility to load centralized app icons and brand assets directly from the `assets/` folder,
 * reducing duplicate resource overhead in `res/` and optimizing APK size.
 */
object AssetIconLoader {

    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    /**
     * Loads a Bitmap from `assets/<assetPath>` (with optional in-memory caching).
     */
    fun loadBitmap(context: Context, assetPath: String): Bitmap? {
        memoryCache.get(assetPath)?.let { return it }

        return try {
            val normalizedPath = assetPath.trimStart('/')
            context.assets.open(normalizedPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.also { bitmap ->
                    memoryCache.put(assetPath, bitmap)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads a Drawable representation from `assets/<assetPath>`.
     */
    fun loadDrawable(context: Context, assetPath: String): Drawable? {
        val bitmap = loadBitmap(context, assetPath) ?: return null
        return BitmapDrawable(context.resources, bitmap)
    }

    /**
     * Helper to load an asset image directly into an ImageView.
     */
    fun loadInto(imageView: ImageView, assetPath: String) {
        val drawable = loadDrawable(imageView.context, assetPath)
        if (drawable != null) {
            imageView.setImageDrawable(drawable)
        }
    }

    /**
     * Clears cached asset bitmaps.
     */
    fun clearCache() {
        memoryCache.evictAll()
    }
}
