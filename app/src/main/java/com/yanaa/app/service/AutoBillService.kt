package com.yanaa.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AutoBillService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // TODO: 监听支付宝/微信支付页面
    }

    override fun onInterrupt() {
        // 服务中断
    }
}
