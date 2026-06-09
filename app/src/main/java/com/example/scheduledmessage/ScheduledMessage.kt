package com.example.scheduledmessage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScheduledMessage(
    val id: Int,
    val text: String,
    val delaySeconds: Int = 0,   // 알림 시작 후 몇 초 뒤에 표시
    val isEnabled: Boolean = true
) : Parcelable {
    fun delayString(): String = if (delaySeconds == 0) "즉시" else "${delaySeconds}초 후"
}