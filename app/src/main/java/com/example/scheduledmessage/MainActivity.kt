package com.example.scheduledmessage

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MessageAdapter
    private var roomId: Int = 0
    private var roomName: String = "예약 메세지"

    private val handler = Handler(Looper.getMainLooper())
    private val scheduledRunnables = mutableListOf<Runnable>()
    private var isNotifRunning = false

    private val profilePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val room = RoomStore.getAll(this).find { r -> r.id == roomId } ?: return@let
            RoomStore.update(this, room.copy(iconUri = it.toString()))
            loadRoomProfile()
            Toast.makeText(this, "프로필 이미지가 변경되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 메세지별 아이콘 피커
    private var pendingEditMsg: ScheduledMessage? = null
    private var pendingEditIconView: ImageView? = null
    private var pendingIconUri: String? = null
    private var pendingTextColor: String? = null
    private var pendingBold: Boolean = false
    private var pendingSize: Int? = null
    private var pendingImageUri: String? = null
    private var pendingEditImageView: ImageView? = null
    private var pendingNoImageView: android.widget.TextView? = null

    private val msgIconPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pendingIconUri = it.toString()
            pendingEditIconView?.let { iv ->
                Glide.with(this).load(it).circleCrop().into(iv)
            }
        }
    }

    private val msgImagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pendingImageUri = it.toString()
            pendingEditImageView?.let { iv ->
                iv.visibility = View.VISIBLE
                pendingNoImageView?.visibility = View.GONE
                Glide.with(this).load(it).centerCrop().into(iv)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomId = intent.getIntExtra("room_id", 0)
        roomName = intent.getStringExtra("room_name") ?: "예약 메세지"
        supportActionBar?.title = roomName
        binding.tvRoomTitle.text = roomName

        setupRecyclerView()
        loadRoomProfile()
        refreshList()

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.btnNotifSettings.setOnClickListener {
            startActivity(Intent(this, NotificationScreenActivity::class.java).apply {
                putExtra("room_id", roomId)
            })
        }
        binding.ivRoomProfile.setOnClickListener { profilePickerLauncher.launch("image/*") }
        binding.btnStartNotif.setOnClickListener { toggleNotif() }
    }

    override fun onResume() {
        super.onResume()
        loadRoomProfile()
        refreshList()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotif()
    }

    private fun toggleNotif() {
        if (isNotifRunning) {
            stopNotif()
        } else {
            startNotif()
        }
    }

    private fun startNotif() {
        val messages = MessageStore.getAll(this, roomId).filter { it.isEnabled }
        if (messages.isEmpty()) {
            Toast.makeText(this, "메세지가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val room = RoomStore.getAll(this).find { it.id == roomId }

        // AlarmDisplayActivity를 즉시 실행 (검은 화면으로 시작)
        val intent = Intent(this, AlarmDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("room_id", roomId)
            putExtra("room_name", room?.name ?: "예약 메세지")
            putExtra("icon_uri", room?.iconUri)
            putParcelableArrayListExtra("messages", ArrayList(messages))
        }
        startActivity(intent)

        isNotifRunning = true
        binding.btnStartNotif.text = "■ 알림 중지"
        binding.btnStartNotif.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B71C1C"))

        // 마지막 메세지 표시 후 자동으로 버튼 상태 복귀
        val maxDelay = messages.maxOf { it.delaySeconds } * 1000L
        val resetR = Runnable { resetNotifButton() }
        scheduledRunnables.add(resetR)
        handler.postDelayed(resetR, maxDelay + 500L)
    }

    private fun stopNotif() {
        scheduledRunnables.forEach { handler.removeCallbacks(it) }
        scheduledRunnables.clear()
        resetNotifButton()
    }

    private fun resetNotifButton() {
        isNotifRunning = false
        binding.btnStartNotif.text = "▶ 메세지 알림 시작"
        binding.btnStartNotif.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32"))
    }

    private fun sendMessage() {
        val text = binding.etInput.text.toString().trim()
        if (text.isEmpty()) return
        val msg = ScheduledMessage(
            id = MessageStore.nextId(this, roomId),
            text = text,
            delaySeconds = 0
        )
        MessageStore.add(this, roomId, msg)
        binding.etInput.setText("")
        refreshList()
        binding.rvMessages.scrollToPosition(MessageStore.getAll(this, roomId).size - 1)
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            MessageStore.getAll(this, roomId).toMutableList(),
            onEdit = { msg -> showEditDelayDialog(msg) },
            onDelete = { msg ->
                MessageStore.remove(this, roomId, msg.id)
                refreshList()
            }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun showEditDelayDialog(msg: ScheduledMessage) {
        // 편집 상태 초기화
        pendingEditMsg = msg
        pendingIconUri = msg.iconUri
        pendingTextColor = msg.textColor
        pendingBold = msg.textBold
        pendingSize = msg.textSizeSp
        pendingImageUri = msg.imageUri

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null)
        val etDelay = view.findViewById<EditText>(R.id.etDelaySeconds)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val ivMsgIcon = view.findViewById<ImageView>(R.id.ivMsgIcon)
        val btnPickIcon = view.findViewById<android.widget.Button>(R.id.btnPickMsgIcon)
        val btnClearIcon = view.findViewById<android.widget.Button>(R.id.btnClearMsgIcon)
        val tvSelectedColor = view.findViewById<TextView>(R.id.tvSelectedColor)
        val viewColorDot = view.findViewById<View>(R.id.viewColorDot)
        val seekR = view.findViewById<SeekBar>(R.id.seekColorR)
        val seekG = view.findViewById<SeekBar>(R.id.seekColorG)
        val seekB = view.findViewById<SeekBar>(R.id.seekColorB)
        val tvR = view.findViewById<TextView>(R.id.tvColorR)
        val tvG = view.findViewById<TextView>(R.id.tvColorG)
        val tvB = view.findViewById<TextView>(R.id.tvColorB)
        val switchBold = view.findViewById<Switch>(R.id.switchTextBold)
        val seekSize = view.findViewById<SeekBar>(R.id.seekTextSize)
        val tvSizeLabel = view.findViewById<TextView>(R.id.tvTextSizeLabel)
        val btnResetSize = view.findViewById<android.widget.Button>(R.id.btnResetTextSize)

        tvTitle.text = "\"${msg.text.take(20)}${if (msg.text.length > 20) "…" else ""}\""
        etDelay.setText(msg.delaySeconds.toString())

        // 아이콘 미리보기
        pendingEditIconView = ivMsgIcon
        if (msg.iconUri != null) {
            Glide.with(this).load(Uri.parse(msg.iconUri)).circleCrop().into(ivMsgIcon)
        }

        btnPickIcon.setOnClickListener { msgIconPickerLauncher.launch("image/*") }
        btnClearIcon.setOnClickListener {
            pendingIconUri = null
            ivMsgIcon.setImageResource(R.drawable.ic_notification_icon)
        }

        // ── 색상 헬퍼 ──────────────────────────────────────────
        fun updateColorPreview(hex: String?) {
            pendingTextColor = hex
            if (hex != null) {
                tvSelectedColor.text = hex
                tvSelectedColor.setTextColor(Color.parseColor(hex))
                viewColorDot.visibility = View.VISIBLE
                viewColorDot.setBackgroundColor(Color.parseColor(hex))
            } else {
                tvSelectedColor.text = "기본값"
                tvSelectedColor.setTextColor(Color.parseColor("#AAAAAA"))
                viewColorDot.visibility = View.INVISIBLE
            }
        }

        // RGB → hex 문자열 변환
        fun rgbToHex(r: Int, g: Int, b: Int) = String.format("#%02X%02X%02X", r, g, b)

        // RGB 슬라이더 → 색상 동기화
        fun syncFromSliders() {
            val hex = rgbToHex(seekR.progress, seekG.progress, seekB.progress)
            updateColorPreview(hex)
        }

        // 스와치 → RGB 슬라이더 동기화
        fun setColorFromHex(hex: String?) {
            if (hex == null) {
                // 기본값: 초기 슬라이더 값 (기본 흰색 계열 #F0F0F5)
                seekR.progress = 240; seekG.progress = 240; seekB.progress = 245
                tvR.text = "240"; tvG.text = "240"; tvB.text = "245"
                updateColorPreview(null)
            } else {
                val c = Color.parseColor(hex)
                seekR.progress = Color.red(c)
                seekG.progress = Color.green(c)
                seekB.progress = Color.blue(c)
                tvR.text = Color.red(c).toString()
                tvG.text = Color.green(c).toString()
                tvB.text = Color.blue(c).toString()
                updateColorPreview(hex)
            }
        }

        // 초기 색상 로드
        setColorFromHex(msg.textColor)

        // RGB SeekBar 리스너
        val rgbListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, fromUser: Boolean) {
                if (!fromUser) return
                tvR.text = seekR.progress.toString()
                tvG.text = seekG.progress.toString()
                tvB.text = seekB.progress.toString()
                syncFromSliders()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        }
        seekR.setOnSeekBarChangeListener(rgbListener)
        seekG.setOnSeekBarChangeListener(rgbListener)
        seekB.setOnSeekBarChangeListener(rgbListener)

        // 색상 스와치
        view.findViewById<View>(R.id.swatchDefault).setOnClickListener { setColorFromHex(null) }
        view.findViewById<View>(R.id.swatchWhite).setOnClickListener   { setColorFromHex("#F0F0F5") }
        view.findViewById<View>(R.id.swatchYellow).setOnClickListener  { setColorFromHex("#FFE066") }
        view.findViewById<View>(R.id.swatchSky).setOnClickListener     { setColorFromHex("#7EC8E3") }
        view.findViewById<View>(R.id.swatchGreen).setOnClickListener   { setColorFromHex("#6BFF8A") }
        view.findViewById<View>(R.id.swatchPink).setOnClickListener    { setColorFromHex("#FFB3C6") }
        view.findViewById<View>(R.id.swatchOrange).setOnClickListener  { setColorFromHex("#FFAA55") }
        view.findViewById<View>(R.id.swatchRed).setOnClickListener     { setColorFromHex("#FF4444") }

        // ── 글자 굵기 ────────────────────────────────────────────
        switchBold.isChecked = msg.textBold
        switchBold.setOnCheckedChangeListener { _, checked -> pendingBold = checked }

        // ── 글자 크기 ─────────────────────────────────────────────
        val initSize = msg.textSizeSp ?: 14
        seekSize.progress = initSize
        tvSizeLabel.text = if (msg.textSizeSp == null) "기본(14sp)" else "${initSize}sp"

        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, fromUser: Boolean) {
                pendingSize = v
                tvSizeLabel.text = "${v}sp"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        btnResetSize.setOnClickListener {
            pendingSize = null
            seekSize.progress = 14
            tvSizeLabel.text = "기본(14sp)"
        }

        // ── 첨부 이미지 ───────────────────────────────────────────
        val ivMsgImage = view.findViewById<ImageView>(R.id.ivMsgImage)
        val tvNoImage = view.findViewById<android.widget.TextView>(R.id.tvNoImage)
        val btnPickImage = view.findViewById<android.widget.Button>(R.id.btnPickMsgImage)
        val btnClearImage = view.findViewById<android.widget.Button>(R.id.btnClearMsgImage)

        pendingEditImageView = ivMsgImage
        pendingNoImageView = tvNoImage

        if (msg.imageUri != null) {
            ivMsgImage.visibility = View.VISIBLE
            tvNoImage.visibility = View.GONE
            Glide.with(this).load(Uri.parse(msg.imageUri)).centerCrop().into(ivMsgImage)
        }

        btnPickImage.setOnClickListener { msgImagePickerLauncher.launch("image/*") }
        btnClearImage.setOnClickListener {
            pendingImageUri = null
            ivMsgImage.visibility = View.GONE
            tvNoImage.visibility = View.VISIBLE
            ivMsgImage.setImageDrawable(null)
        }

        AlertDialog.Builder(this)
            .setTitle("메세지 설정")
            .setView(view)
            .setPositiveButton("저장") { _, _ ->
                val delay = etDelay.text.toString().toIntOrNull()
                if (delay == null || delay < 0) {
                    Toast.makeText(this, "올바른 초를 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updated = msg.copy(
                    delaySeconds = delay,
                    iconUri = pendingIconUri,
                    textColor = pendingTextColor,
                    textBold = pendingBold,
                    textSizeSp = pendingSize,
                    imageUri = pendingImageUri
                )
                MessageStore.update(this, roomId, updated)
                refreshList()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun refreshList() {
        val all = MessageStore.getAll(this, roomId)
        adapter.refresh(all)
        binding.tvEmpty.visibility =
            if (all.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun loadRoomProfile() {
        val room = RoomStore.getAll(this).find { it.id == roomId }
        val iconUri = room?.iconUri
        if (iconUri != null) {
            Glide.with(this)
                .load(Uri.parse(iconUri))
                .circleCrop()
                .into(binding.ivRoomProfile)
        } else {
            binding.ivRoomProfile.setImageResource(android.R.drawable.ic_menu_camera)
        }
    }
}