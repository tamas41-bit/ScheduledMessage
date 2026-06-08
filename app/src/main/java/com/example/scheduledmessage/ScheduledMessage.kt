package com.example.scheduledmessage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScheduledMessage(
    val id: Int,
    val text: String,
    val hour: Int = -1,       // -1 = 시간 미설정
    val minute: Int = 0,
    val second: Int = 0,
    val isRepeating: Boolean = false,
    val isEnabled: Boolean = true,
    val iconUri: String? = null
) : Parcelable {
    fun timeString(): String {
        if (hour < 0) return "시간 미설정"
        val ampm = if (hour < 12) "오전" else "오후"
        val h = if (hour % 12 == 0) 12 else hour % 12
        return "$ampm ${h}:${minute.toString().padStart(2, '0')}:${second.toString().padStart(2, '0')}"
    }
}
