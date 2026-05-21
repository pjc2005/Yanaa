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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName != ALIPAY_PACKAGE && packageName != WECHAT_PACKAGE) return

        // Extract text directly from the event (most reliable across vendors)
        val eventTexts = mutableListOf<String>()
        if (event.text != null) {
            for (i in event.text.indices) {
                event.text[i]?.toString()?.takeIf { it.isNotBlank() }?.let { eventTexts.add(it) }
            }
        }
        event.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { eventTexts.add(it) }

        val now = System.currentTimeMillis()
        if (packageName == lastProcessedPackage && (now - lastProcessedTime) < debounceInterval) return

        lastProcessedPackage = packageName
        lastProcessedTime = now

        // Try to extract payment info from event text immediately
        var amountText = ""
        var merchantText = ""
        var isPaymentPage = false

        for (text in eventTexts) {
            if (text.contains("支付成功") || text.contains("付款成功") || text.contains("支付金额")) {
                isPaymentPage = true
            }
            val amountMatch = Regex("[¥￥]?\\d+\\.?\\d{0,2}[元]?").find(text)
            if (amountMatch != null && amountText.isEmpty()) {
                amountText = amountMatch.value.filter { it.isDigit() || it == '.' }
            }
            if (text.contains("商户") || text.contains("收款方") || text.contains("商家")) {
                val parts = text.split("商户|收款方|商家".toRegex())
                if (parts.size >= 2) merchantText = parts[1].trim()
            }
        }

        if (isPaymentPage || amountText.isNotEmpty()) {
            showEditDialog(amountText, merchantText, packageName)
            return
        }

        // Fallback: try screenshot + accessibility tree after a delay
        mainHandler.postDelayed({
            captureAndAnalyze(packageName)
        }, 1200)
    }

    private fun captureAndAnalyze(packageName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                executor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        try {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            // Convert HARDWARE buffer to mutable ARGB_8888 Bitmap
                            val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.colorSpace)
                            if (bitmap != null) {
                                analyzeBitmap(bitmap, packageName)
                            } else {
                                // Screenshot failed, try accessibility tree
                                extractFromAccessibilityTree(packageName)
                            }
                            hardwareBuffer.close()
                        } catch (e: Exception) {
                            android.util.Log.e("AutoBillService", "Screenshot processing error: ${e.message}")
                            extractFromAccessibilityTree(packageName)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        android.util.Log.e("AutoBillService", "Screenshot failed: code=$errorCode")
                        extractFromAccessibilityTree(packageName)
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AutoBillService", "takeScreenshot threw: ${e.message}")
            extractFromAccessibilityTree(packageName)
        }
    }

    /**
     * Fallback: parse the accessibility node tree to extract payment info directly
     */
    private fun extractFromAccessibilityTree(packageName: String) {
        val root = rootInActiveWindow ?: run {
            // Still show the edit dialog so user knows it triggered
            showEditDialog("", "", packageName)
            return
        }

        val collectedTexts = mutableListOf<String>()

        // Walk the tree and collect all text
        fun walk(node: android.view.accessibility.AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.text != null) collectedTexts.add(node.text.toString())
            for (i in 0 until node.childCount) {
                walk(node.getChild(i))
            }
        }
        walk(root)

        var amountText = ""
        var merchantText = ""

        for (text in collectedTexts) {
            // Look for amount patterns: ¥123.45, 123.45元
            val amountMatch = Regex("[¥￥]?\\d+\\.?\\d{0,2}[元]?").find(text)
            if (amountMatch != null && amountText.isEmpty()) {
                amountText = amountMatch.value.filter { it.isDigit() || it == '.' }
            }

            // Look for merchant/store name (common patterns in payment success pages)
            if (text.contains("商户") || text.contains("收款方") || text.contains("商家")) {
                val parts = text.split("商户|收款方|商家".toRegex())
                if (parts.size >= 2) {
                    merchantText = parts[1].trim()
                }
            }

            // Detect payment success keywords
            if (text.contains("支付成功") || text.contains("付款成功")) {
                android.util.Log.d("AutoBillService", "Payment success detected from: $packageName")
            }
        }

        showEditDialog(amountText, merchantText, packageName)
    }

    private fun analyzeBitmap(bitmap: Bitmap, packageName: String) {
        // Ensure bitmap is ARGB_8888 (takeScreenshot may return HARDWARE bitmaps)
        val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        if (argbBitmap == null) {
            showEditDialog("", "", packageName)
            return
        }

        var amountText = ""
        var merchantText = ""

        if (yoloDetector.isReady()) {
            val detections = yoloDetector.detect(argbBitmap)

            for (detection in detections) {
                val rect = detection.rect
                if (rect.width() <= 0f || rect.height() <= 0f) continue

                val x = rect.left.toInt().coerceAtLeast(0)
                val y = rect.top.toInt().coerceAtLeast(0)
                val w = rect.width().toInt().coerceAtMost(argbBitmap.width - x)
                val h = rect.height().toInt().coerceAtMost(argbBitmap.height - y)

                if (w <= 0 || h <= 0) continue

                val cropped = try {
                    Bitmap.createBitmap(argbBitmap, x, y, w, h)
                } catch (e: Exception) {
                    continue
                }

                val text = ocrExtractor.extractTextSync(cropped) ?: continue

                if (detection.label == "amount") {
                    amountText = text.filter { it.isDigit() || it == '.' }
                } else if (detection.label == "merchant") {
                    merchantText = text
                }
            }
        }

        // Always show the edit dialog when a payment screen is detected
        showEditDialog(amountText, merchantText, packageName)
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
