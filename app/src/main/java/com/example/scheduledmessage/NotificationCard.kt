package com.example.scheduledmessage

data class NotificationCard(
    val roomName: String,
    val messageText: String,
    val roomIconUri: String?,
    val textColor: String? = null,   // null = 기본값 (#F0F0F5)
    val textBold: Boolean = false,
    val textSizeSp: Int? = null,      // null = 기본값 14sp
    val timestampMs: Long = System.currentTimeMillis()   // 수신 시각 (상대시간 계산용)
)
