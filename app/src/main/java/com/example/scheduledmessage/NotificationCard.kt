package com.example.scheduledmessage

data class NotificationCard(
    val roomName: String,
    val messageText: String,
    val roomIconUri: String?,
    val timestampMs: Long = System.currentTimeMillis()   // 수신 시각 (상대시간 계산용)
)
