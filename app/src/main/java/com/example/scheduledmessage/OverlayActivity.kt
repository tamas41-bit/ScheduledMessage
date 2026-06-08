package com.example.scheduledmessage

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityOverlayBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverlayBinding
    private val handler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null
    private var roomId: Int = 0

    // 드래그용
    private var dX = 0f
    private var dY = 0f

    // 폰트 목록: (표시이름, Typeface)
    data class FontItem(val name: String, val typeface: Typeface)

    private val fontList: List<FontItem> by lazy {
        listOf(
            FontItem("기본체", Typeface.DEFAULT),
            FontItem("굵은 기본체", Typeface.DEFAULT_BOLD),
            FontItem("모노스페이스", Typeface.MONOSPACE),
            FontItem("굵은 모노스페이스", Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)),
            FontItem("세리프", Typeface.SERIF),
            FontItem("굵은 세리프", Typeface.create(Typeface.SERIF, Typeface.BOLD)),
            FontItem("산스세리프", Typeface.SANS_SERIF),
            FontItem("굵은 산스세리프", Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)),
            FontItem("이탤릭", Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)),
            FontItem("굵은 이탤릭", Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)),
            FontItem("세리프 이탤릭", Typeface.create(Typeface.SERIF, Typeface.ITALIC)),
            FontItem("모노 이탤릭", Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC)),
            FontItem("condensed", Typeface.create("sans-serif-condensed", Typeface.NORMAL)),
            FontItem("condensed bold", Typeface.create("sans-serif-condensed", Typeface.BOLD)),
            FontItem("light", Typeface.create("sans-serif-light", Typeface.NORMAL)),
            FontItem("thin", Typeface.create("sans-serif-thin", Typeface.NORMAL)),
            FontItem("medium", Typeface.create("sans-serif-medium", Typeface.NORMAL)),
            FontItem("medium bold", Typeface.create("sans-serif-medium", Typeface.BOLD)),
            FontItem("black", Typeface.create("sans-serif-black", Typeface.NORMAL)),
            FontItem("cursive", Typeface.create("cursive", Typeface.NORMAL)),
            FontItem("serif light", Typeface.create("serif", Typeface.NORMAL)),
            FontItem("nanum gothic", Typeface.create("NanumGothic", Typeface.NORMAL)),
            FontItem("roboto", Typeface.create("roboto", Typeface.NORMAL)),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomId = intent.getIntExtra("room_id", 0)
        val msgText = intent.getStringExtra("message_text") ?: ""
        val iconUri = intent.getStringExtra("icon_uri")
        val roomName = intent.getStringExtra("room_name") ?: "예약 메세지"

        binding.tvNotificationText.text = msgText
        binding.tvNotificationTitle.text = roomName

        // 배경
        val bgUri = MessageStore.getNotifBgUri(this, roomId)
        if (bgUri != null) {
            Glide.with(this).load(bgUri).centerCrop().into(binding.ivBackground)
        }
        if (iconUri != null) {
            Glide.with(this).load(android.net.Uri.parse(iconUri)).circleCrop()
                .into(binding.ivNotificationIcon)
        }

        // 시계 설정 복원
        applyClockSettings()
        startClock()

        // 드래그로 시계 위치 이동
        binding.layoutClock.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    view.animate().x(newX).y(newY).setDuration(0).start()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 비율로 저장
                    val parent = binding.root
                    val xPct = view.x / parent.width
                    val yPct = view.y / parent.height
                    MessageStore.saveClockPosition(this, roomId, xPct, yPct)
                    true
                }
                else -> false
            }
        }

        binding.btnDismiss.setOnClickListener { finish() }
        binding.btnEditClock.setOnClickListener { showTimeDialog() }
        binding.btnFontSelect.setOnClickListener { showFontDialog() }
        binding.btnClockSize.setOnClickListener { showSizeDialog() }

        // 컨트롤 버튼 누를 때는 닫히지 않도록 처리
        binding.root.setOnClickListener {
            // 컨트롤 영역 클릭은 무시 - 나머지 영역만 닫기
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()

        // 시계 초기 위치 설정 (뷰가 측정된 후)
        binding.root.post {
            val parent = binding.root
            val xPct = MessageStore.getClockXPct(this, roomId)
            val yPct = MessageStore.getClockYPct(this, roomId)
            val clockW = binding.layoutClock.width.toFloat()
            val clockH = binding.layoutClock.height.toFloat()
            binding.layoutClock.x = xPct * parent.width - clockW / 2
            binding.layoutClock.y = yPct * parent.height - clockH / 2
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
                updateClock()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(clockRunnable!!)
    }

    private fun updateClock() {
        if (MessageStore.getUseCustomTime(this, roomId)) {
            val h = MessageStore.getCustomHour(this, roomId)
            val m = MessageStore.getCustomMinute(this, roomId)
            binding.tvTime.text = String.format("%02d:%02d", h, m)
        } else {
            binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
        binding.tvDate.text = SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN).format(Date())
    }

    private fun applyClockSettings() {
        // 크기
        val sizeSp = MessageStore.getClockSizeSp(this, roomId)
        binding.tvTime.textSize = sizeSp.toFloat()

        // 폰트
        val fontName = MessageStore.getClockFont(this, roomId)
        val fontItem = fontList.find { it.name == fontName }
        if (fontItem != null) {
            binding.tvTime.typeface = fontItem.typeface
        }
    }

    // ── 시간 설정 다이얼로그 ──────────────────────────────────────────
    private fun showTimeDialog() {
        val view = layoutInflater.inflate(android.R.layout.activity_list_item, null)
        // NumberPicker 직접 생성
        val pickerLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val pickerH = NumberPicker(this).apply {
            minValue = 0; maxValue = 23
            value = if (MessageStore.getUseCustomTime(this@OverlayActivity, roomId))
                MessageStore.getCustomHour(this@OverlayActivity, roomId)
            else
                java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            setFormatter { v -> String.format("%02d", v) }
        }
        val colon = TextView(this).apply {
            text = " : "
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
        }
        val pickerM = NumberPicker(this).apply {
            minValue = 0; maxValue = 59
            value = if (MessageStore.getUseCustomTime(this@OverlayActivity, roomId))
                MessageStore.getCustomMinute(this@OverlayActivity, roomId)
            else
                java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
            setFormatter { v -> String.format("%02d", v) }
        }

        pickerLayout.addView(pickerH)
        pickerLayout.addView(colon)
        pickerLayout.addView(pickerM)

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }

        val tvHint = TextView(this).apply {
            text = "시계에 표시될 시간을 설정합니다.\n'실제 시간 사용' 선택 시 현재 시간이 표시됩니다."
            textSize = 13f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, 0, 0, 16)
        }
        container.addView(tvHint)
        container.addView(pickerLayout)

        AlertDialog.Builder(this)
            .setTitle("시계 시간 설정")
            .setView(container)
            .setPositiveButton("저장") { _, _ ->
                MessageStore.saveCustomTime(this, roomId, true, pickerH.value, pickerM.value)
                updateClock()
            }
            .setNeutralButton("실제 시간 사용") { _, _ ->
                MessageStore.saveCustomTime(this, roomId, false, 0, 0)
                updateClock()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 폰트 선택 다이얼로그 ─────────────────────────────────────────
    private fun showFontDialog() {
        val names = fontList.map { it.name }.toTypedArray()
        val currentFont = MessageStore.getClockFont(this, roomId)
        val currentIdx = fontList.indexOfFirst { it.name == currentFont }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("폰트 선택")
            .setSingleChoiceItems(names, currentIdx) { dialog, which ->
                val selected = fontList[which]
                binding.tvTime.typeface = selected.typeface
                MessageStore.saveClockFont(this, roomId, selected.name)
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 크기 조절 다이얼로그 ─────────────────────────────────────────
    private fun showSizeDialog() {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val currentSize = MessageStore.getClockSizeSp(this, roomId)
        val tvPreview = TextView(this).apply {
            text = binding.tvTime.text
            textSize = currentSize.toFloat()
            typeface = binding.tvTime.typeface
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val tvLabel = TextView(this).apply {
            text = "크기: ${currentSize}sp"
            textSize = 14f
            setTextColor(0xFFCCCCCC.toInt())
            gravity = android.view.Gravity.CENTER
        }

        val seekBar = SeekBar(this).apply {
            min = 24
            max = 150
            progress = currentSize
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                tvPreview.textSize = p.toFloat()
                tvLabel.text = "크기: ${p}sp"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        container.addView(tvPreview)
        container.addView(tvLabel)
        container.addView(seekBar)

        AlertDialog.Builder(this)
            .setTitle("시계 크기 조절")
            .setView(container)
            .setPositiveButton("저장") { _, _ ->
                val newSize = seekBar.progress
                binding.tvTime.textSize = newSize.toFloat()
                MessageStore.saveClockSizeSp(this, roomId, newSize)
            }
            .setNegativeButton("취소") { _, _ ->
                binding.tvTime.textSize = currentSize.toFloat()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockRunnable?.let { handler.removeCallbacks(it) }
    }
}