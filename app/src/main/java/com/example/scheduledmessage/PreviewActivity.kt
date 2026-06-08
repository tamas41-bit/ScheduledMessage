package com.example.scheduledmessage

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityPreviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private var roomId: Int = 0

    private val fontList: List<Pair<String, Typeface>> by lazy {
        listOf(
            "기본체" to Typeface.DEFAULT,
            "세리프" to Typeface.SERIF,
            "세리프 이탤릭" to Typeface.create(Typeface.SERIF, Typeface.ITALIC),
            "cursive" to Typeface.create("cursive", Typeface.NORMAL),
            "serif light" to Typeface.create("serif", Typeface.NORMAL),
            "roboto" to Typeface.create("roboto", Typeface.NORMAL),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()
        supportActionBar?.hide()
        roomId = intent.getIntExtra("room_id", 0)

        loadBackground()
        applyClock()
        setupTestCard()

        binding.btnClosePreview.setOnClickListener { finish() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun loadBackground() {
        val bgUri = MessageStore.getNotifBgUri(this, roomId)
        if (bgUri != null) {
            Glide.with(this).load(bgUri).centerCrop().into(binding.ivBackground)
        }
    }

    private fun applyClock() {
        // 시간
        binding.tvTime.text = if (MessageStore.getUseCustomTime(this, roomId)) {
            String.format("%02d:%02d",
                MessageStore.getCustomHour(this, roomId),
                MessageStore.getCustomMinute(this, roomId))
        } else {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
        binding.tvDate.text = SimpleDateFormat("M월 d일 EEEE", Locale("ko")).format(Date())
        binding.tvDate.visibility = if (MessageStore.getShowDate(this, roomId)) View.VISIBLE else View.GONE

        // 크기 & 폰트
        binding.tvTime.textSize = MessageStore.getClockSizeSp(this, roomId).toFloat()
        val fontName = MessageStore.getClockFont(this, roomId)
        fontList.find { it.first == fontName }?.let { binding.tvTime.typeface = it.second }

        // 저장된 위치 복원
        binding.root.post {
            val xPct = MessageStore.getClockXPct(this, roomId)
            val yPct = MessageStore.getClockYPct(this, roomId)
            binding.layoutClock.x = xPct * binding.root.width - binding.layoutClock.width / 2f
            binding.layoutClock.y = yPct * binding.root.height - binding.layoutClock.height / 2f
        }
    }

    private fun setupTestCard() {
        val adapter = NotificationCardAdapter(roomId)
        binding.rvCards.layoutManager = LinearLayoutManager(this)
        binding.rvCards.itemAnimator = null   // 미리보기에서는 애니메이션 없이 즉시 표시
        binding.rvCards.adapter = adapter
        adapter.addCard(
            NotificationCard(
                roomName    = "테스트",
                messageText = "테스트입니다.",
                roomIconUri = null
            )
        )
    }
}
