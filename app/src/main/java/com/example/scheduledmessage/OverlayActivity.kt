package com.example.scheduledmessage

import android.os.Bundle
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
        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val msgText = intent.getStringExtra("message_text") ?: ""
        val iconUri = intent.getStringExtra("icon_uri")
        val roomName = intent.getStringExtra("room_name") ?: "예약 메세지"

        // 현재 시간/날짜 표시
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val dateStr = SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN).format(Date())
        binding.tvTime.text = timeStr
        binding.tvDate.text = dateStr

        // 알림 카드 내용
        binding.tvNotificationText.text = msgText
        binding.tvNotificationTitle.text = roomName

        // 배경 이미지
        val bgUri = MessageStore.getBackgroundUri(this)
        if (bgUri != null) {
            Glide.with(this).load(bgUri).centerCrop().into(binding.ivBackground)
        }

        // 방 아이콘 (없으면 기본 아이콘 유지)
        if (iconUri != null) {
            Glide.with(this)
                .load(android.net.Uri.parse(iconUri))
                .circleCrop()
                .into(binding.ivNotificationIcon)
        }

        binding.btnDismiss.setOnClickListener { finish() }
        binding.root.setOnClickListener { finish() }
    }
}
