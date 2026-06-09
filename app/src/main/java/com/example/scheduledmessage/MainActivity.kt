package com.example.scheduledmessage

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.EditText
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
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null)
        val etDelay = view.findViewById<EditText>(R.id.etDelaySeconds)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)

        tvTitle.text = "\"${msg.text.take(20)}${if (msg.text.length > 20) "…" else ""}\""
        etDelay.setText(msg.delaySeconds.toString())

        AlertDialog.Builder(this)
            .setTitle("알림 딜레이 설정")
            .setView(view)
            .setPositiveButton("저장") { _, _ ->
                val delay = etDelay.text.toString().toIntOrNull()
                if (delay == null || delay < 0) {
                    Toast.makeText(this, "올바른 초를 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updated = msg.copy(delaySeconds = delay)
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