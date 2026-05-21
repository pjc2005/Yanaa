package com.yanaa.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer

data class DetectionResult(
    val rect: Rect,
    val label: String,
    val confidence: Float
)

class YOLODetector(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var isInitialized = false

    data class BoundingBox(
        var x1: Float, var y1: Float,
        var x2: Float, var y2: Float,
        var cx: Float, var cy: Float,
        var w: Float, var h: Float,
        var cnf: Float, var cls: Int,
        var label: String = ""
    )

    fun initialize(): Boolean {
        return try {
            val model = loadModelFile("yolo_model.tflite")
            interpreter = Interpreter(model)
            labels = loadLabels("labels.txt")
            isInitialized = true
            true
        } catch (e: Exception) {
            isInitialized = false
            false
        }
    }

    fun isReady(): Boolean = isInitialized

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (!isInitialized) return emptyList()

        val interpreter = interpreter ?: return emptyList()

        // 1. Resize to 640x640
        val resized = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

        // 2. Preprocess: [0..1] float, RGB, NHWC
        val inputShape = interpreter.getInputTensor(0).shape()
        val inputBuffer = ByteBuffer.allocateDirect(1 * 640 * 640 * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        val pixels = IntArray(640 * 640)
        resized.getPixels(pixels, 0, 640, 0, 0, 640, 640)
        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }

        // 3. Run inference
        val outputShape = interpreter.getOutputTensor(0).shape()
        val numDetections = outputShape[2] // 8400
        val numCoords = outputShape[1]     // 84 (4 bbox + 80 class)
        val output = Array(1) { Array(numDetections) { FloatArray(numCoords) } }
        interpreter.run(inputBuffer, output)

        // 4. Parse detections
        val boxes = mutableListOf<BoundingBox>()
        for (i in 0 until numDetections) {
            val confidence = output[0][i][4]
            if (confidence < 0.5f) continue

            var maxClassScore = 0f
            var maxClassIdx = 0
            for (j in 5 until numCoords) {
                if (output[0][i][j] > maxClassScore) {
                    maxClassScore = output[0][i][j]
                    maxClassIdx = j - 5
                }
            }

            val totalCnf = confidence * maxClassScore
            if (totalCnf < 0.5f || maxClassIdx >= labels.size) continue

            val cx = output[0][i][0]
            val cy = output[0][i][1]
            val w = output[0][i][2]
            val h = output[0][i][3]

            boxes.add(BoundingBox(
                x1 = cx - w / 2, y1 = cy - h / 2,
                x2 = cx + w / 2, y2 = cy + h / 2,
                cx = cx, cy = cy, w = w, h = h,
                cnf = totalCnf, cls = maxClassIdx,
                label = labels.getOrElse(maxClassIdx) { "unknown" }
            ))
        }

        // 5. NMS
        boxes.sortByDescending { it.cnf }
        val selected = mutableListOf<BoundingBox>()
        val iouThreshold = 0.45f

        for (box in boxes) {
            var keep = true
            for (sel in selected) {
                if (iou(box, sel) > iouThreshold && box.cls == sel.cls) {
                    keep = false
                    break
                }
            }
            if (keep) selected.add(box)
        }

        // 6. Scale back to original bitmap size
        val scaleX = bitmap.width.toFloat() / 640f
        val scaleY = bitmap.height.toFloat() / 640f

        return selected.map { box ->
            DetectionResult(
                rect = Rect(
                    (box.x1 * scaleX).toInt().coerceAtLeast(0),
                    (box.y1 * scaleY).toInt().coerceAtLeast(0),
                    (box.x2 * scaleX).toInt().coerceAtMost(bitmap.width),
                    (box.y2 * scaleY).toInt().coerceAtMost(bitmap.height)
                ),
                label = box.label,
                confidence = box.cnf
            )
        }
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val interX1 = maxOf(a.x1, b.x1)
        val interY1 = maxOf(a.y1, b.y1)
        val interX2 = minOf(a.x2, b.x2)
        val interY2 = minOf(a.y2, b.y2)
        val interArea = maxOf(0f, interX2 - interX1) * maxOf(0f, interY2 - interY1)
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        return interArea / (areaA + areaB - interArea)
    }

    private fun loadModelFile(filename: String): ByteBuffer {
        val fd = context.assets.openFd(filename)
        val inputStream = context.assets.open(filename)
        val bytes = inputStream.readBytes()
        inputStream.close()
        return ByteBuffer.wrap(bytes)
    }

    private fun loadLabels(filename: String): List<String> {
        val reader = BufferedReader(InputStreamReader(context.assets.open(filename)))
        val result = reader.readLines()
        reader.close()
        return result
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
