package com.example.scheduledmessage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 재부팅 시 활성화된 알람 재등록
            val messages = MessageStore.getAll(context)
            messages.filter { it.isEnabled }.forEach { AlarmScheduler.schedule(context, it) }
            return
        }

        val msgId = intent.getIntExtra("message_id", -1)
        val msgText = intent.getStringExtra("message_text") ?: return

        // 화면 깨우기
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "ScheduledMessage::AlarmWakeLock"
        )
        wl.acquire(10000L)

        // OverlayActivity 실행 (잠금화면 위)
        val overlayIntent = Intent(context, OverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("message_id", msgId)
            putExtra("message_text", msgText)
        }
        context.startActivity(overlayIntent)

        wl.release()
    }
}
