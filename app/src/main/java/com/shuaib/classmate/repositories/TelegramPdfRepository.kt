package com.shuaib.classmate.repositories

import android.content.Context
import android.util.Log
import com.shuaib.classmate.network.TelegramApi
import com.shuaib.classmate.utils.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class TelegramPdfRepository(private val context: Context) {
    private val TAG = "TelegramPdfRepo"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.telegram.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val telegramApi = retrofit.create(TelegramApi::class.java)

    /**
     * Downloads the PDF from Telegram and saves it to the app's cache directory.
     * Returns the cached File if successful.
     */
    suspend fun downloadPdf(fileId: String, fileName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val token = AppConstants.TELEGRAM_BOT_TOKEN
            if (token.isBlank()) {
                return@withContext Result.failure(Exception("Telegram Bot Token is missing"))
            }

            Log.d(TAG, "Getting file info for ID: \$fileId")
            val response = telegramApi.getFile(token, fileId)
            
            if (!response.isSuccessful || response.body()?.ok != true) {
                return@withContext Result.failure(Exception("Failed to get file info from Telegram: \${response.message()}"))
            }

            val filePath = response.body()?.result?.filePath
            if (filePath.isNullOrBlank()) {
                return@withContext Result.failure(Exception("File path is empty in Telegram response"))
            }

            Log.d(TAG, "File path retrieved: \$filePath, starting download...")
            val downloadUrl = "https://api.telegram.org/file/bot\$token/\$filePath"
            
            val request = Request.Builder().url(downloadUrl).build()
            val downloadResponse = okHttpClient.newCall(request).execute()
            
            if (!downloadResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download PDF bytes: \${downloadResponse.code}"))
            }

            val body = downloadResponse.body ?: return@withContext Result.failure(Exception("Empty response body"))
            
            val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").takeIf { it.isNotBlank() } ?: "document.pdf"
            val finalName = if (!safeFileName.endsWith(".pdf", ignoreCase = true)) "\$safeFileName.pdf" else safeFileName
            
            val cachedFile = File(context.cacheDir, finalName)
            
            FileOutputStream(cachedFile).use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Successfully downloaded PDF to \${cachedFile.absolutePath}")
            Result.success(cachedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading PDF", e)
            Result.failure(e)
        }
    }
}
