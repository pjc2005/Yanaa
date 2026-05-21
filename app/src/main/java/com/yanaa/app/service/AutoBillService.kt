package com.yanaa.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.yanaa.app.ml.OCRExtractor
import com.yanaa.app.ml.YOLODetector
import java.util.concurrent.Executors

class AutoBillService : AccessibilityService() {

    private lateinit var yoloDetector: YOLODetector
    private lateinit var ocrExtractor: OCRExtractor
    private var lastProcessedPackage = ""
    private var lastProcessedTime = 0L
    private val debounceInterval = 3000L
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
        private const val WECHAT_PACKAGE = "com.tencent.mm"
    }

    override fun onCreate() {
        super.onCreate()
        yoloDetector = YOLODetector(this)
        ocrExtractor = OCRExtractor()
        executor.execute {
            val ready = yoloDetector.initialize()
            if (!ready) {
                android.util.Log.w("AutoBillService", "YOLO model not loaded")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName != ALIPAY_PACKAGE && packageName != WECHAT_PACKAGE) return

        val now = System.currentTimeMillis()
        if (packageName == lastProcessedPackage && (now - lastProcessedTime) < debounceInterval) return

        lastProcessedPackage = packageName
        lastProcessedTime = now

        mainHandler.postDelayed({
            captureAndAnalyze(packageName)
        }, 1200)
    }

    private fun captureAndAnalyze(packageName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            executor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshotResult: ScreenshotResult) {
                    val hardwareBuffer = screenshotResult.hardwareBuffer
                    val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.colorSpace)
                    if (bitmap != null) {
                        analyzeBitmap(bitmap, packageName)
                    }
                    hardwareBuffer.close()
                }

                override fun onFailure(errorCode: Int) {
                    android.util.Log.e("AutoBillService", "Screenshot failed: code=$errorCode")
                }
            }
        )
    }

    private fun analyzeBitmap(bitmap: Bitmap, packageName: String) {
        if (!yoloDetector.isReady()) return

        val detections = yoloDetector.detect(bitmap)
        var amountText = ""
        var merchantText = ""

        for (detection in detections) {
            val rect = detection.rect
            if (rect.width() <= 0 || rect.height() <= 0) continue

            val cropped = try {
                Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
            } catch (e: Exception) {
                continue
            }

            val text = ocrExtractor.extractTextSync(cropped) ?: continue
            cropped.recycle()

            if (detection.label == "amount") {
                amountText = text.filter { it.isDigit() || it == '.' }
            } else if (detection.label == "merchant") {
                merchantText = text
            }
        }

        if (amountText.isNotEmpty()) {
            showEditDialog(amountText, merchantText, packageName)
        }
    }

    private fun showEditDialog(amount: String, merchant: String, packageName: String) {
        val intent = Intent(this, com.yanaa.app.ui.EditRecordActivity::class.java).apply {
            putExtra("amount", amount)
            putExtra("merchant", merchant)
            putExtra("source", packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
