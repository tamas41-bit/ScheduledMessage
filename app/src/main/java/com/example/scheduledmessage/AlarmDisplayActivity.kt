package com.example.scheduledmessage

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityAlarmDisplayBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmDisplayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmDisplayBinding
    private lateinit var cardAdapter: NotificationCardAdapter
    private val clockHandler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null

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
            "roboto" to Typeface.create("roboto", Typeface.NORMAL),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()
        supportActionBar?.hide()

        setupRecyclerView()
        loadBackground()
        applyClockSettings()
        startClock()

        // 저장된 시계 위치 복원
        binding.root.post {
            val parent = binding.root
            val xPct = MessageStore.getClockXPct(this)
            val yPct = MessageStore.getClockYPct(this)
            binding.layoutClock.x = xPct * parent.width - binding.layoutClock.width / 2f
            binding.layoutClock.y = yPct * parent.height - binding.layoutClock.height / 2f
        }

        binding.btnClose.setOnClickListener { finish() }

        handleIntent()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockRunnable?.let { clockHandler.removeCallbacks(it) }
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

    private fun startClock() {
        clockRunnable = object : Runnable {
            override fun run() {
                if (MessageStore.getUseCustomTime(this@AlarmDisplayActivity)) {
                    binding.tvTime.text = String.format(
                        "%02d:%02d",
                        MessageStore.getCustomHour(this@AlarmDisplayActivity),
                        MessageStore.getCustomMinute(this@AlarmDisplayActivity)
                    )
                } else {
                    binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                }
                binding.tvDate.text = SimpleDateFormat("M월 d일 EEEE", Locale("ko")).format(Date())
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable!!)
    }

    private fun applyClockSettings() {
        binding.tvTime.textSize = MessageStore.getClockSizeSp(this).toFloat()
        val fontName = MessageStore.getClockFont(this)
        fontList.find { it.first == fontName }?.let { binding.tvTime.typeface = it.second }
    }

    private fun loadBackground() {
        val savedBg = getSharedPreferences("notif_screen_prefs", MODE_PRIVATE)
            .getString("bg_uri", null)
        if (savedBg != null) {
            Glide.with(this).load(savedBg).centerCrop().into(binding.ivBackground)
        }
    }

    private fun setupRecyclerView() {
        cardAdapter = NotificationCardAdapter()
        binding.rvCards.layoutManager = LinearLayoutManager(this)
        binding.rvCards.adapter = cardAdapter
        cardAdapter.attachSwipeToDismiss(binding.rvCards)
    }

    private fun handleIntent() {
        val roomName = intent.getStringExtra("room_name") ?: return
        val messageText = intent.getStringExtra("message_text") ?: return
        val roomIconUri = intent.getStringExtra("icon_uri")
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        cardAdapter.addCard(
            NotificationCard(
                roomName = roomName,
                messageText = messageText,
                roomIconUri = roomIconUri,
                time = timeStr
            )
        )
        binding.rvCards.scrollToPosition(0)
    }
}
