package com.example.scheduledmessage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 재부팅 시 모든 방의 활성 알람 재등록
            RoomStore.getAll(context).forEach { room ->
                MessageStore.getAll(context, room.id)
                    .filter { it.isEnabled && it.hour >= 0 }
                    .forEach { AlarmScheduler.schedule(context, it, room.id) }
            }
            return
        }

        val msgId = intent.getIntExtra("message_id", -1)
        val msgText = intent.getStringExtra("message_text") ?: return
        val roomId = intent.getIntExtra("room_id", 0)

        // 방 아이콘 가져오기
        val room = RoomStore.getAll(context).find { it.id == roomId }
        val iconUri = room?.iconUri

        // OverlayActivity 실행 (앱이 포그라운드에 있을 때만 동작)
        val overlayIntent = Intent(context, OverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("message_id", msgId)
            putExtra("message_text", msgText)
            putExtra("room_name", room?.name ?: "예약 메세지")
            putExtra("icon_uri", iconUri)
        }
        try {
            context.startActivity(overlayIntent)
        } catch (e: Exception) {
            // 앱이 백그라운드에 있으면 무시 (Android 10+ 제한)
        }
    }
}
