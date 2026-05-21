package com.yanaa.app.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

class OCRExtractor {
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    fun extractText(bitmap: Bitmap, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onSuccess(visionText.text)
            }
            .addOnFailureListener { e ->
                onFailure(e as Exception)
            }
    }

    fun extractTextSync(bitmap: Bitmap): String? {
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            val task = recognizer.process(image)
            // Blocking call for simpler call sites
            val result = task.result
            return result?.text
        } catch (e: Exception) {
            return null
        }
    }
}
