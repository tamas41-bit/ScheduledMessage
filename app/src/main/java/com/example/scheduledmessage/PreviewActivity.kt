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

    private val fontList: List<Pair<String, Typeface>> by lazy {
        listOf(
            "기본체" to Typeface.DEFAULT,
            "굵은 기본체" to Typeface.DEFAULT_BOLD,
            "모노스페이스" to Typeface.MONOSPACE,
            "굵은 모노스페이스" to Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
            "세리프" to Typeface.SERIF,
            "굵은 세리프" to Typeface.create(Typeface.SERIF, Typeface.BOLD),
            "산스세리프" to Typeface.SANS_SERIF,
            "굵은 산스세리프" to Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD),
            "이탤릭" to Typeface.create(Typeface.DEFAULT, Typeface.ITALIC),
            "굵은 이탤릭" to Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC),
            "세리프 이탤릭" to Typeface.create(Typeface.SERIF, Typeface.ITALIC),
            "모노 이탤릭" to Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC),
            "condensed" to Typeface.create("sans-serif-condensed", Typeface.NORMAL),
            "condensed bold" to Typeface.create("sans-serif-condensed", Typeface.BOLD),
            "light" to Typeface.create("sans-serif-light", Typeface.NORMAL),
            "thin" to Typeface.create("sans-serif-thin", Typeface.NORMAL),
            "medium" to Typeface.create("sans-serif-medium", Typeface.NORMAL),
            "medium bold" to Typeface.create("sans-serif-medium", Typeface.BOLD),
            "black" to Typeface.create("sans-serif-black", Typeface.NORMAL),
            "cursive" to Typeface.create("cursive", Typeface.NORMAL),
            "serif light" to Typeface.create("serif", Typeface.NORMAL),
            "nanum gothic" to Typeface.create("NanumGothic", Typeface.NORMAL),
            "roboto" to Typeface.create("roboto", Typeface.NORMAL)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()
        supportActionBar?.hide()

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
        val bgUri = getSharedPreferences("notif_screen_prefs", MODE_PRIVATE)
            .getString("bg_uri", null)
        if (bgUri != null) {
            Glide.with(this).load(bgUri).centerCrop().into(binding.ivBackground)
        }
    }

    private fun applyClock() {
        // 시간
        binding.tvTime.text = if (MessageStore.getUseCustomTime(this)) {
            String.format("%02d:%02d",
                MessageStore.getCustomHour(this),
                MessageStore.getCustomMinute(this))
        } else {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
        binding.tvDate.text = SimpleDateFormat("M월 d일 EEEE", Locale("ko")).format(Date())
        binding.tvDate.visibility = if (MessageStore.getShowDate(this)) View.VISIBLE else View.GONE

        // 크기 & 폰트
        binding.tvTime.textSize = MessageStore.getClockSizeSp(this).toFloat()
        val fontName = MessageStore.getClockFont(this)
        fontList.find { it.first == fontName }?.let { binding.tvTime.typeface = it.second }

        // 저장된 위치 복원
        binding.root.post {
            val xPct = MessageStore.getClockXPct(this)
            val yPct = MessageStore.getClockYPct(this)
            binding.layoutClock.x = xPct * binding.root.width - binding.layoutClock.width / 2f
            binding.layoutClock.y = yPct * binding.root.height - binding.layoutClock.height / 2f
        }
    }

    private fun setupTestCard() {
        val adapter = NotificationCardAdapter()
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
