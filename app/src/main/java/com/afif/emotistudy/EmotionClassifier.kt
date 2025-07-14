package com.afif.emotistudy.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream

class EmotionClassifier(private val context: Context) {

    private val model: Module = Module.load(assetFilePath(context, "autoencoder_mobile.pt"))

    // Labels from 1 to 7 mapped to emotions
    private val labelMap = mapOf(
        1 to "Angry 😠",
        2 to "Disgust 😖",
        3 to "Fear 😨",
        4 to "Happy 😊",
        5 to "Sad 😢",
        6 to "Surprise 😲",
        7 to "Neutral 😐"
    )

    fun classify(bitmap: Bitmap): Pair<String, Float> {
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            createScaledBitmap(bitmap, 64, 64, true),
            floatArrayOf(0.0f, 0.0f, 0.0f),
            floatArrayOf(1.0f, 1.0f, 1.0f)
        )

        val output = model.forward(IValue.from(inputTensor)).toTensor().dataAsFloatArray

        val predictedIndex = output.indices.maxByOrNull { output[it] } ?: -1
        val confidence = output.getOrNull(predictedIndex) ?: 0f

        // Since your labels start from 1, add +1 to 0-based index
        val label = labelMap[predictedIndex + 1] ?: "Unknown"

        logFeatureToFile(label, confidence)

        return Pair(label, confidence)
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath

        context.assets.open(assetName).use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    private fun logFeatureToFile(label: String, value: Float) {
        val timestamp = System.currentTimeMillis()
        val logEntry = "$timestamp,$label,${"%.2f".format(value)}\n"
        val logFile = File(context.filesDir, "feature_log.csv")
        logFile.appendText(logEntry)
    }
}
