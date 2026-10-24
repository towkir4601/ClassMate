package com.shuaib.classmate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class CompressedImageResult(
    val file: File,
    val uri: Uri,
    val sizeBytes: Long,
    val sizeKb: Long,
    val width: Int,
    val height: Int
)

object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val DEFAULT_TARGET_MAX_KB = 100L
    private const val DEFAULT_MAX_DIMENSION = 1200

    /**
     * Copies any URI (especially ephemeral Android PhotoPicker content:// URIs) into a locally
     * accessible temporary file in the app's cache directory while permissions are active.
     */
    fun copyUriToLocalCache(context: Context, uri: Uri): File? {
        if (uri.scheme == "file" && uri.path != null) {
            val f = File(uri.path!!)
            if (f.exists() && f.length() > 0) return f
        }

        val tempFile = File(context.cacheDir, "raw_pick_${System.currentTimeMillis()}.tmp")
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) tempFile else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed copying URI to local cache: ${e.message}", e)
            tempFile.delete()
            null
        }
    }

    /**
     * Compresses an image given its Uri so that its file size is strictly <= targetSizeKb (default 100KB).
     * Handles orientation rotation, dimension scaling, and adaptive quality optimization.
     */
    suspend fun compressImage(
        context: Context,
        imageUri: Uri,
        targetSizeKb: Long = DEFAULT_TARGET_MAX_KB,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Result<CompressedImageResult> = withContext(Dispatchers.IO) {
        runCatching {
            val targetBytes = targetSizeKb * 1024L

            // Step 1: Ensure we have a local File we can read reliably
            val localSourceFile: File
            val isTempCopied: Boolean
            if (imageUri.scheme == "file" && imageUri.path != null) {
                localSourceFile = File(imageUri.path!!)
                isTempCopied = false
            } else {
                localSourceFile = copyUriToLocalCache(context, imageUri)
                    ?: throw IllegalArgumentException("Cannot open image stream for $imageUri")
                isTempCopied = true
            }

            try {
                // Step 2: Read image bounds directly from local file
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(localSourceFile.absolutePath, boundsOptions)

                val originalWidth = boundsOptions.outWidth
                val originalHeight = boundsOptions.outHeight
                if (originalWidth <= 0 || originalHeight <= 0) {
                    throw IllegalStateException("Invalid image dimensions ($originalWidth x $originalHeight)")
                }

                // Step 3: Compute sample size
                var inSampleSize = 1
                val maxSide = max(originalWidth, originalHeight)
                while ((maxSide / (inSampleSize * 2)) >= maxDimension) {
                    inSampleSize *= 2
                }

                // Step 4: Decode scaled bitmap directly from local file
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                var bitmap = BitmapFactory.decodeFile(localSourceFile.absolutePath, decodeOptions)
                    ?: throw IllegalStateException("Failed to decode bitmap from image file")

                // Step 5: Handle EXIF orientation rotation
                val rotationAngle = getExifOrientation(localSourceFile)
                if (rotationAngle != 0) {
                    val matrix = Matrix().apply { postRotate(rotationAngle.toFloat()) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotated != bitmap) {
                        bitmap.recycle()
                        bitmap = rotated
                    }
                }

                // Step 6: Dimension scaling if still larger than maxDimension
                val currentMax = max(bitmap.width, bitmap.height)
                if (currentMax > maxDimension) {
                    val scale = maxDimension.toFloat() / currentMax.toFloat()
                    val targetW = (bitmap.width * scale).roundToInt()
                    val targetH = (bitmap.height * scale).roundToInt()
                    val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                    if (scaled != bitmap) {
                        bitmap.recycle()
                        bitmap = scaled
                    }
                }

                // Step 7: Adaptive iterative compression loop to guarantee <= targetBytes (100 KB)
                var quality = 85
                var compressedBytes: ByteArray
                val byteStream = ByteArrayOutputStream()

                while (true) {
                    byteStream.reset()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteStream)
                    compressedBytes = byteStream.toByteArray()

                    if (compressedBytes.size <= targetBytes || quality <= 25) {
                        break
                    }
                    quality -= 10
                }

                // If still slightly over target due to high image complexity, scale down dimensions once more
                if (compressedBytes.size > targetBytes && bitmap.width > 400 && bitmap.height > 400) {
                    val furtherScaled = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * 0.75).roundToInt(),
                        (bitmap.height * 0.75).roundToInt(),
                        true
                    )
                    if (furtherScaled != bitmap) {
                        bitmap.recycle()
                        bitmap = furtherScaled
                    }
                    byteStream.reset()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 55, byteStream)
                    compressedBytes = byteStream.toByteArray()
                }

                // Step 8: Save to app cache file
                val outputDir = File(context.cacheDir, "compressed_photos").apply { mkdirs() }
                val outputFile = File(outputDir, "img_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outputFile).use { fos ->
                    fos.write(compressedBytes)
                    fos.flush()
                }

                val finalWidth = bitmap.width
                val finalHeight = bitmap.height
                bitmap.recycle()

                CompressedImageResult(
                    file = outputFile,
                    uri = Uri.fromFile(outputFile),
                    sizeBytes = compressedBytes.size.toLong(),
                    sizeKb = (compressedBytes.size + 1023) / 1024L,
                    width = finalWidth,
                    height = finalHeight
                )
            } finally {
                // Clean up temporary copied source if one was created
                if (isTempCopied) {
                    localSourceFile.delete()
                }
            }
        }
    }

    private fun getExifOrientation(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }
}
