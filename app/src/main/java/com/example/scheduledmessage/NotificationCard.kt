package com.example.scheduledmessage

data class NotificationCard(
    val roomName: String,
    val messageText: String,
    val roomIconUri: String?,
    val time: String
)
