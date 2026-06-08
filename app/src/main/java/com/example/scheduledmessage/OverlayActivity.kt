package com.example.scheduledmessage

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityOverlayBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 잠금화면 위에 표시
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val msgText = intent.getStringExtra("message_text") ?: ""
        val iconUri = intent.getStringExtra("icon_uri")

        // 현재 시간
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val dateStr = SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN).format(Date())
        binding.tvTime.text = timeStr
        binding.tvDate.text = dateStr

        // 알림 카드에 메세지 표시
        binding.tvNotificationText.text = msgText

        // 배경 이미지
        val bgUri = MessageStore.getBackgroundUri(this)
        if (bgUri != null) {
            Glide.with(this)
                .load(bgUri)
                .centerCrop()
                .into(binding.ivBackground)
        }

        // 커스텀 아이콘
        if (iconUri != null) {
            Glide.with(this)
                .load(android.net.Uri.parse(iconUri))
                .circleCrop()
                .into(binding.ivNotificationIcon)
        }

        // 닫기 버튼
        binding.btnDismiss.setOnClickListener { finish() }

        // 빈 곳 터치해도 닫기
        binding.root.setOnClickListener { finish() }
    }
}
