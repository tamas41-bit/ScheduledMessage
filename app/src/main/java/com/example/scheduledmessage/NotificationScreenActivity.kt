package com.example.scheduledmessage

import android.graphics.Typeface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityNotificationScreenBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationScreenBinding
    private lateinit var cardAdapter: NotificationCardAdapter
    private val clockHandler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null

    private var dX = 0f
    private var dY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private val TAP_THRESHOLD_DP = 8f

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

    private val bgPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            getSharedPreferences("notif_screen_prefs", MODE_PRIVATE)
                .edit().putString("bg_uri", it.toString()).apply()
            loadBackground(it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()

        supportActionBar?.hide()

        setupRecyclerView()
        val savedBg = getSharedPreferences("notif_screen_prefs", MODE_PRIVATE).getString("bg_uri", null)
        loadBackground(savedBg)

        applyClockSettings()
        startClock()

        setupClockTouch()

        // 시계 위치 복원 (레이아웃 완료 후 한 번만)
        binding.root.post {
            val parent = binding.root
            val xPct = MessageStore.getClockXPct(this)
            val yPct = MessageStore.getClockYPct(this)
            binding.layoutClock.x = xPct * parent.width - binding.layoutClock.width / 2f
            binding.layoutClock.y = yPct * parent.height - binding.layoutClock.height / 2f
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnChangeBg.setOnClickListener { bgPickerLauncher.launch("image/*") }

        handleNotificationIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
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

    private fun setupClockTouch() {
        val tapThresholdPx = TAP_THRESHOLD_DP * resources.displayMetrics.density
        binding.layoutClock.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    downRawX = event.rawX
                    downRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.animate().x(event.rawX + dX).y(event.rawY + dY).setDuration(0).start()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val movedX = Math.abs(event.rawX - downRawX)
                    val movedY = Math.abs(event.rawY - downRawY)
                    if (movedX < tapThresholdPx && movedY < tapThresholdPx) {
                        // 탭 → 설정 바텀시트
                        showClockSettingsBottomSheet()
                    } else {
                        // 드래그 → 위치 저장 (중심 좌표 기준)
                        val parent = binding.root
                        val cx = (view.x + view.width / 2f) / parent.width
                        val cy = (view.y + view.height / 2f) / parent.height
                        MessageStore.saveClockPosition(this, cx, cy)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showClockSettingsBottomSheet() {
        val sheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_clock_settings, null)
        sheet.setContentView(sheetView)

        val switchShowDate = sheetView.findViewById<Switch>(R.id.switchShowDate)
        val seekSize = sheetView.findViewById<SeekBar>(R.id.seekClockSize)
        val tvSizeLabel = sheetView.findViewById<TextView>(R.id.tvSizeLabel)
        val tvCurrentFont = sheetView.findViewById<TextView>(R.id.tvCurrentFont)
        val tvCurrentTime = sheetView.findViewById<TextView>(R.id.tvCurrentTime)
        val rowFont = sheetView.findViewById<View>(R.id.rowFontSelect)
        val rowTime = sheetView.findViewById<View>(R.id.rowTimeEdit)

        // 현재 값 채우기
        switchShowDate.isChecked = MessageStore.getShowDate(this)
        seekSize.progress = MessageStore.getClockSizeSp(this)
        tvSizeLabel.text = "${seekSize.progress}sp"
        tvCurrentFont.text = MessageStore.getClockFont(this).let { saved ->
            fontList.find { it.name == saved }?.name ?: "기본체"
        }
        tvCurrentTime.text = if (MessageStore.getUseCustomTime(this))
            String.format("%02d:%02d", MessageStore.getCustomHour(this), MessageStore.getCustomMinute(this))
        else "실제 시간"

        // 날짜 표시 토글
        switchShowDate.setOnCheckedChangeListener { _, checked ->
            MessageStore.saveShowDate(this, checked)
            binding.tvDate.visibility = if (checked) View.VISIBLE else View.GONE
        }

        // 크기 슬라이더
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                tvSizeLabel.text = "${p}sp"
                binding.tvTime.textSize = p.toFloat()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                MessageStore.saveClockSizeSp(this@NotificationScreenActivity, sb?.progress ?: 72)
            }
        })

        // 폰트 선택
        rowFont.setOnClickListener {
            sheet.dismiss()
            showFontDialog()
        }

        // 시간 설정
        rowTime.setOnClickListener {
            sheet.dismiss()
            showTimeDialog()
        }

        sheet.show()
    }

    private fun startClock() {
        clockRunnable = object : Runnable {
            override fun run() {
                updateClock()
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable!!)
    }

    private fun updateClock() {
        if (MessageStore.getUseCustomTime(this)) {
            binding.tvTime.text = String.format("%02d:%02d",
                MessageStore.getCustomHour(this), MessageStore.getCustomMinute(this))
        } else {
            binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
        binding.tvDate.text = SimpleDateFormat("M월 d일 EEEE", Locale("ko")).format(Date())
        binding.tvDate.visibility = if (MessageStore.getShowDate(this)) View.VISIBLE else View.GONE
    }

    private fun applyClockSettings() {
        binding.tvTime.textSize = MessageStore.getClockSizeSp(this).toFloat()
        val fontName = MessageStore.getClockFont(this)
        fontList.find { it.name == fontName }?.let { binding.tvTime.typeface = it.typeface }
        binding.tvDate.visibility = if (MessageStore.getShowDate(this)) View.VISIBLE else View.GONE
    }

    private fun handleNotificationIntent(intent: Intent) {
        val roomName = intent.getStringExtra("room_name") ?: return
        val messageText = intent.getStringExtra("message_text") ?: return
        val roomIconUri = intent.getStringExtra("icon_uri")
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val card = NotificationCard(
            roomName = roomName,
            messageText = messageText,
            roomIconUri = roomIconUri,
            time = timeStr
        )
        cardAdapter.addCard(card)
        binding.rvCards.scrollToPosition(0)
    }

    private fun setupRecyclerView() {
        cardAdapter = NotificationCardAdapter()
        binding.rvCards.layoutManager = LinearLayoutManager(this)
        binding.rvCards.itemAnimator = NotificationCardAnimator()
        binding.rvCards.adapter = cardAdapter
        cardAdapter.attachSwipeToDismiss(binding.rvCards)
    }

    private fun loadBackground(uriString: String?) {
        if (uriString != null) {
            Glide.with(this).load(uriString).centerCrop().into(binding.ivBackground)
        }
    }

    private fun showTimeDialog() {
        val pickerLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        val pickerH = NumberPicker(this).apply {
            minValue = 0; maxValue = 23
            value = if (MessageStore.getUseCustomTime(this@NotificationScreenActivity))
                MessageStore.getCustomHour(this@NotificationScreenActivity)
            else java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            setFormatter { v -> String.format("%02d", v) }
        }
        val colon = TextView(this).apply {
            text = " : "; textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
        }
        val pickerM = NumberPicker(this).apply {
            minValue = 0; maxValue = 59
            value = if (MessageStore.getUseCustomTime(this@NotificationScreenActivity))
                MessageStore.getCustomMinute(this@NotificationScreenActivity)
            else java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
            setFormatter { v -> String.format("%02d", v) }
        }
        pickerLayout.addView(pickerH); pickerLayout.addView(colon); pickerLayout.addView(pickerM)

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }
        container.addView(TextView(this).apply {
            text = "시계에 표시될 시간을 설정합니다.\n'실제 시간 사용' 선택 시 현재 시간이 표시됩니다."
            textSize = 13f; setTextColor(0xFFAAAAAA.toInt()); setPadding(0, 0, 0, 16)
        })
        container.addView(pickerLayout)

        AlertDialog.Builder(this)
            .setTitle("시간 설정")
            .setView(container)
            .setPositiveButton("저장") { _, _ ->
                MessageStore.saveCustomTime(this, true, pickerH.value, pickerM.value)
                updateClock()
            }
            .setNeutralButton("실제 시간 사용") { _, _ ->
                MessageStore.saveCustomTime(this, false, 0, 0)
                updateClock()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showFontDialog() {
        val names = fontList.map { it.name }.toTypedArray()
        val currentIdx = fontList.indexOfFirst { it.name == MessageStore.getClockFont(this) }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("폰트 선택")
            .setSingleChoiceItems(names, currentIdx) { dialog, which ->
                binding.tvTime.typeface = fontList[which].typeface
                MessageStore.saveClockFont(this, fontList[which].name)
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
