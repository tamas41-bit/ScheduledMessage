package com.example.scheduledmessage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 더 이상 시간 기반 알람을 사용하지 않으므로 빈 수신기 유지
    }
}