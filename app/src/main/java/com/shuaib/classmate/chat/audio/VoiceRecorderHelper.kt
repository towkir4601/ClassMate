package com.shuaib.classmate.chat.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

data class VoiceRecordResult(
    val file: File,
    val durationSec: Int
)

class VoiceRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0L
    var isRecording: Boolean = false
        private set

    fun startRecording(): Boolean {
        return try {
            val audioDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val outputFile = File(audioDir, "vn_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(32000)
                setAudioSamplingRate(22050)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordStartTime = System.currentTimeMillis()
            isRecording = true
            true
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Failed to start audio recording: ${e.message}", e)
            cancelRecording()
            false
        }
    }

    fun stopRecording(): VoiceRecordResult? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            val durationMs = System.currentTimeMillis() - recordStartTime
            val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)
            val file = currentOutputFile

            if (file != null && file.exists() && file.length() > 0) {
                VoiceRecordResult(file, durationSec)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Failed to stop recording: ${e.message}", e)
            cancelRecording()
            null
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (ignored: Exception) {
        } finally {
            mediaRecorder = null
            isRecording = false
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }
}
