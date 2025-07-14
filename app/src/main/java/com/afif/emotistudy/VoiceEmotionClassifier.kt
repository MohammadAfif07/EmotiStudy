package com.afif.emotistudy

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class VoiceEmotionClassifier(context: Context) {

    private val classifier: AudioClassifier =
        AudioClassifier.createFromFile(context, "voice_mood_model.tflite")
    private val audioRecord: AudioRecord = classifier.createAudioRecord()

    fun classifyRealtime(): Pair<String, Float> {
        // Start recording from microphone
        audioRecord.startRecording()

        // Record short audio for inference
        val tensor = classifier.createInputTensorAudio()
        tensor.load(audioRecord)

        // Run classification
        val results = classifier.classify(tensor)
        audioRecord.stop()

        val topCategory = results.firstOrNull()?.categories?.maxByOrNull { it.score }
            ?: return "Unknown" to 0f

        return topCategory.label to topCategory.score * 100f
    }

    fun classify(): Any {
        return TODO("Provide the return value")
    }
}
