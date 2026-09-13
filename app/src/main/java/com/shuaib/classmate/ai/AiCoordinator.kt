package com.shuaib.classmate.ai

import android.util.Log
import com.shuaib.classmate.ClassMateApp
import com.shuaib.classmate.utils.AppPreferences

class AiCoordinator(
    private val geminiProvider: GeminiAiProvider,
    private val groqProvider: GroqAiProvider
) {

    private fun isGeminiPrimary(): Boolean {
        return try {
            AppPreferences(ClassMateApp.instance).isGeminiPrimary()
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun summarizeNotice(input: NoticeSummaryInput): Result<AiResult<String>> {
        Log.d("AiCoordinator", "Trying AI provider: Gemini (Forced - Summarize)")
        val geminiResult = geminiProvider.summarizeNotice(input)
        if (geminiResult.isSuccess) {
            return Result.success(AiResult(geminiResult.getOrThrow(), "GEMINI"))
        }
        val error = geminiResult.exceptionOrNull() ?: Exception("Unknown Gemini error")
        return Result.failure(error)
    }

    suspend fun generateNoticeDraft(input: NoticeDraftInput): Result<AiResult<com.shuaib.classmate.models.AiNoticeDraft>> {
        Log.d("AiCoordinator", "Trying AI provider: Gemini (Forced - Draft)")
        val geminiResult = geminiProvider.generateNoticeDraft(input)
        if (geminiResult.isSuccess) {
            return Result.success(AiResult(geminiResult.getOrThrow(), "GEMINI"))
        }
        val error = geminiResult.exceptionOrNull() ?: Exception("Unknown Gemini error")
        return Result.failure(error)
    }

    suspend fun chatWithAi(input: AiChatInput): Result<AiResult<String>> {
        Log.d("AiCoordinator", "Trying AI provider: Gemini (Forced - Chat)")
        val geminiResult = geminiProvider.chatWithAi(input)
        if (geminiResult.isSuccess) {
            return Result.success(AiResult(geminiResult.getOrThrow(), "GEMINI"))
        }
        val error = geminiResult.exceptionOrNull() ?: Exception("Unknown Gemini error")
        return Result.failure(error)
    }
}
