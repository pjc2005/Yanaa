package com.yanaa.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

data class DetectionResult(
    val rect: RectF,
    val label: String,
    val confidence: Float
)

class YOLODetector(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val modelFilename = "yolo_model.tflite"
    private val labelFilename = "labels.txt"

    // EfficientDet output params
    private val inputImageWidth = 320
    private val inputImageHeight = 320
    private val outputLocationsShape = intArrayOf(1, 100, 4)
    private val outputClassesShape = intArrayOf(1, 100)
    private val outputScoresShape = intArrayOf(1, 100)
    private val numDetectionsShape = intArrayOf(1)

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, modelFilename)
            interpreter = Interpreter(modelBuffer)
            loadLabels()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLabels() {
        val labelsList = mutableListOf<String>()
        try {
            context.assets.open(labelFilename).bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) labelsList.add(line.trim())
                }
            }
        } catch (e: Exception) {
            labelsList.addAll(listOf("amount", "merchant"))
        }
        labels = labelsList
    }

    fun isReady(): Boolean = interpreter != null

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interp = interpreter ?: return emptyList()

        // Preprocess: resize to 320x320
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Prepare output buffers
        val outputLocations = TensorBuffer.createFixedSize(outputLocationsShape, DataType.FLOAT32)
        val outputClasses = TensorBuffer.createFixedSize(outputClassesShape, DataType.FLOAT32)
        val outputScores = TensorBuffer.createFixedSize(outputScoresShape, DataType.FLOAT32)
        val numDetections = TensorBuffer.createFixedSize(numDetectionsShape, DataType.FLOAT32)

        // Run inference
        val outputs = mapOf(
            0 to outputLocations.buffer.rewind(),
            1 to outputClasses.buffer.rewind(),
            2 to outputScores.buffer.rewind(),
            3 to numDetections.buffer.rewind()
        )
        interp.run(tensorImage.tensorBuffer.buffer.rewind(), outputs)

        // Parse results
        val locations = outputLocations.floatArray
        val classes = outputClasses.floatArray
        val scores = outputScores.floatArray
        val num = numDetections.floatArray[0].toInt()

        val results = mutableListOf<DetectionResult>()
        for (i in 0 until num.coerceAtMost(100)) {
            val confidence = scores[i]
            if (confidence > 0.5f) {
                val classId = classes[i].toInt()
                val label = if (classId < labels.size) labels[classId] else "unknown"

                // Locations: [y1, x1, y2, x2] normalized [0,1]
                val y1 = locations[i * 4]
                val x1 = locations[i * 4 + 1]
                val y2 = locations[i * 4 + 2]
                val x2 = locations[i * 4 + 3]

                val rect = RectF(
                    x1 * bitmap.width,
                    y1 * bitmap.height,
                    x2 * bitmap.width,
                    y2 * bitmap.height
                )
                results.add(DetectionResult(rect, label, confidence))
            }
        }
        return results
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
