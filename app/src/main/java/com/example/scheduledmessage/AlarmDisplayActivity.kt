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
    private val msgHandler = Handler(Looper.getMainLooper())
    private val scheduledRunnables = mutableListOf<Runnable>()
    private var clockRunnable: Runnable? = null
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
        binding = ActivityAlarmDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()
        supportActionBar?.hide()

        roomId = intent.getIntExtra("room_id", 0)
        val roomName = intent.getStringExtra("room_name") ?: "예약 메세지"
        val iconUri = intent.getStringExtra("icon_uri")

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        val messages: List<ScheduledMessage> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("messages", ScheduledMessage::class.java) ?: emptyList()
        } else {
            intent.getParcelableArrayListExtra<ScheduledMessage>("messages") ?: emptyList()
        }

        setupRecyclerView()
        applyClockSettings()

        // 처음엔 배경과 시계를 숨김 (완전히 까만 화면)
        binding.ivBackground.visibility = View.INVISIBLE
        binding.layoutClock.visibility = View.INVISIBLE

        // 저장된 시계 위치 복원
        binding.root.post {
            val parent = binding.root
            val xPct = MessageStore.getClockXPct(this, roomId)
            val yPct = MessageStore.getClockYPct(this, roomId)
            binding.layoutClock.x = xPct * parent.width - binding.layoutClock.width / 2f
            binding.layoutClock.y = yPct * parent.height - binding.layoutClock.height / 2f
        }

        if (messages.isEmpty()) {
            finish()
            return
        }

        // 시작 대기 시간 (설정값) + 첫 메세지 딜레이 중 더 큰 값에 배경+시계 공개
        val startDelaySec = MessageStore.getStartDelay(this, roomId)
        val firstMsgDelay = messages.minOf { it.delaySeconds }
        val revealDelay = maxOf(startDelaySec, firstMsgDelay) * 1000L

        val revealR = Runnable {
            loadBackground()
            startClock()
            binding.ivBackground.visibility = View.VISIBLE
            binding.layoutClock.visibility = View.VISIBLE
        }
        scheduledRunnables.add(revealR)
        msgHandler.postDelayed(revealR, revealDelay)

        // 각 메세지를 (startDelay + 각 딜레이) 시각에 카드 추가
        messages.forEach { msg ->
            val r = Runnable {
                cardAdapter.addCard(
                    NotificationCard(
                        roomName = roomName,
                        messageText = msg.text,
                        roomIconUri = iconUri
                    )
                )
                binding.rvCards.scrollToPosition(0)
            }
            scheduledRunnables.add(r)
            msgHandler.postDelayed(r, (startDelaySec + msg.delaySeconds) * 1000L)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockRunnable?.let { clockHandler.removeCallbacks(it) }
        scheduledRunnables.forEach { msgHandler.removeCallbacks(it) }
        scheduledRunnables.clear()
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
                if (MessageStore.getUseCustomTime(this@AlarmDisplayActivity, roomId)) {
                    binding.tvTime.text = String.format(
                        "%02d:%02d",
                        MessageStore.getCustomHour(this@AlarmDisplayActivity, roomId),
                        MessageStore.getCustomMinute(this@AlarmDisplayActivity, roomId)
                    )
                } else {
                    binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                }
                binding.tvDate.text = SimpleDateFormat("M월 d일 EEEE", Locale("ko")).format(Date())
                binding.tvDate.visibility = if (MessageStore.getShowDate(this@AlarmDisplayActivity, roomId)) View.VISIBLE else View.GONE
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable!!)
    }

    private fun applyClockSettings() {
        binding.tvTime.textSize = MessageStore.getClockSizeSp(this, roomId).toFloat()
        val fontName = MessageStore.getClockFont(this, roomId)
        fontList.find { it.first == fontName }?.let { binding.tvTime.typeface = it.second }
        binding.tvDate.visibility = if (MessageStore.getShowDate(this, roomId)) View.VISIBLE else View.GONE
    }

    private fun loadBackground() {
        val savedBg = MessageStore.getNotifBgUri(this, roomId)
        if (savedBg != null) {
            Glide.with(this).load(savedBg).centerCrop().into(binding.ivBackground)
        }
    }

    private fun setupRecyclerView() {
        cardAdapter = NotificationCardAdapter(roomId)
        binding.rvCards.layoutManager = LinearLayoutManager(this)
        binding.rvCards.itemAnimator = NotificationCardAnimator()
        binding.rvCards.adapter = cardAdapter
        cardAdapter.attachSwipeToDismiss(binding.rvCards)
    }
}
