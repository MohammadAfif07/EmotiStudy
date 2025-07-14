package com.afif.emotistudy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class VoiceMoodActivity : AppCompatActivity() {

    private lateinit var tvInstruction: TextView
    private lateinit var tvRecognizedText: TextView
    private lateinit var tvMoodResult: TextView
    private lateinit var btnStartListening: Button
    private lateinit var btnTestWav: Button

    private lateinit var tflite: Interpreter
    private val expectedAudioLength = 16000
    private val SPEECH_REQUEST_CODE = 101
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_mood)

        tvInstruction = findViewById(R.id.tvInstruction)
        tvRecognizedText = findViewById(R.id.tvRecognizedText)
        tvMoodResult = findViewById(R.id.tvMoodResult)
        btnStartListening = findViewById(R.id.btnStartListening)
        btnTestWav = findViewById(R.id.btnTestWav)

        try {
            tflite = Interpreter(loadModelFile("voice_mood_model.tflite"))
            val shape = tflite.getInputTensor(0).shape()
            val type = tflite.getInputTensor(0).dataType()
            Log.d("MODEL_SHAPE", "Shape: ${shape.contentToString()}, Type: $type")
        } catch (e: Exception) {
            Log.e("MODEL_INIT", "Model loading failed", e)
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show()
        }

        btnStartListening.setOnClickListener {
            Log.d("BUTTON", "Start Listening clicked")
            requestAudioPermissionAndStart()
        }

        btnTestWav.setOnClickListener {
            Log.d("BUTTON", "Test WAV clicked")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                testWavFile()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun requestAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startVoiceInput()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                startVoiceInput()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your mood")

            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = result?.get(0).orEmpty()
            tvRecognizedText.text = recognizedText

            val mood = detectMoodFromText(recognizedText)
            tvMoodResult.text = "Detected Mood (from voice): $mood"
        }
    }

    private fun detectMoodFromText(text: String): String {
        return when {
            text.contains("happy", ignoreCase = true) -> "Happy"
            text.contains("sad", ignoreCase = true) -> "Sad"
            text.contains("angry", ignoreCase = true) -> "Angry"
            else -> "Neutral"
        }
    }

    private fun testWavFile() {
        Log.d("WAV_TEST", "testWavFile() triggered")
        try {
            val inputStream = resources.openRawResource(R.raw.happy_sample) // Replace with your .wav
            val byteArray = inputStream.readBytes()

            // Strip 44-byte WAV header
            val pcmBytes = byteArray.copyOfRange(44, byteArray.size)
            val shortBuffer = ByteBuffer.wrap(pcmBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
            val shortData = ShortArray(shortBuffer.capacity())
            shortBuffer.get(shortData)

            // Convert to float array
            val floatInput = FloatArray(shortData.size) { i -> shortData[i] / 32768.0f }

            // Pad or trim to 16000 samples
            val inputLength = 16000
            val modelInput = if (floatInput.size >= inputLength) {
                floatInput.copyOf(inputLength)
            } else {
                floatInput + FloatArray(inputLength - floatInput.size)
            }

            Log.d("WAV_TEST", "Running inference on input size ${modelInput.size}")

            val input = arrayOf(modelInput)
            val output = Array(400) { FloatArray(8) } // <-- [400, 8] output shape

            tflite.run(input, output)

            // Average across 400 time steps
            val avgOutput = FloatArray(8)
            for (i in 0 until 400) {
                for (j in 0 until 8) {
                    avgOutput[j] += output[i][j]
                }
            }
            for (j in 0 until 8) {
                avgOutput[j] /= 400f
            }

            val emotionIndex = avgOutput.indices.maxByOrNull { avgOutput[it] } ?: -1
            val emotionLabel = when (emotionIndex) {
                0 -> "Neutral"
                1 -> "Calm"
                2 -> "Happy"
                3 -> "Sad"
                4 -> "Angry"
                5 -> "Fearful"
                6 -> "Disgust"
                7 -> "Surprised"
                else -> "Unknown"
            }

            tvMoodResult.text = "Detected Mood: $emotionLabel"
            tvRecognizedText.text = "Raw: ${avgOutput.joinToString(", ") { "%.2f".format(it) }}"

            Log.d("WAV_TEST", "Prediction: $emotionLabel | Scores: ${avgOutput.joinToString()}")

        } catch (e: Exception) {
            Toast.makeText(this, "Inference failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("WAV_TEST", "Error processing WAV file", e)
        }
    }


    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }
}
