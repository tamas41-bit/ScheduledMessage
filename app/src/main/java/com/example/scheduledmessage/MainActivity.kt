package com.example.scheduledmessage

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MessageAdapter
    private var roomId: Int = 0
    private var roomName: String = "예약 메세지"

    private val bgPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            MessageStore.saveBackgroundUri(this, it.toString())
            loadBackground(it.toString())
            Toast.makeText(this, "배경 이미지가 변경되었습니다", Toast.LENGTH_SHORT).show()
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

        requestPermissions()
        setupRecyclerView()
        loadBackground(MessageStore.getBackgroundUri(this))
        refreshList()

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.btnChangeBg.setOnClickListener { bgPickerLauncher.launch("image/*") }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun sendMessage() {
        val text = binding.etInput.text.toString().trim()
        if (text.isEmpty()) return

        // 기본 알림 시간 = 지금 + 5분
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }

        val msg = ScheduledMessage(
            id = MessageStore.nextId(this, roomId),
            text = text,
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            second = cal.get(Calendar.SECOND)
        )
        MessageStore.add(this, roomId, msg)
        AlarmScheduler.schedule(this, msg, roomId)
        binding.etInput.setText("")
        refreshList()
        binding.rvMessages.scrollToPosition(MessageStore.getAll(this, roomId).size - 1)
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            MessageStore.getAll(this, roomId).toMutableList(),
            onEdit = { msg -> showEditTimeDialog(msg) },
            onDelete = { msg ->
                AlarmScheduler.cancel(this, msg.id)
                MessageStore.remove(this, roomId, msg.id)
                refreshList()
            }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun showEditTimeDialog(msg: ScheduledMessage) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null)
        val etHour = view.findViewById<EditText>(R.id.etHour)
        val etMinute = view.findViewById<EditText>(R.id.etMinute)
        val etSecond = view.findViewById<EditText>(R.id.etSecond)
        val switchRepeat = view.findViewById<Switch>(R.id.switchRepeat)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)

        tvTitle.text = "\"${msg.text.take(20)}${if (msg.text.length > 20) "…" else ""}\""

        val h = if (msg.hour >= 0) msg.hour else Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val m = if (msg.hour >= 0) msg.minute else Calendar.getInstance().also { it.add(Calendar.MINUTE, 5) }.get(Calendar.MINUTE)
        val s = if (msg.hour >= 0) msg.second else 0

        etHour.setText(h.toString())
        etMinute.setText(m.toString())
        etSecond.setText(s.toString())
        switchRepeat.isChecked = msg.isRepeating

        AlertDialog.Builder(this)
            .setTitle("알림 시간 설정")
            .setView(view)
            .setPositiveButton("저장") { _, _ ->
                val hour = etHour.text.toString().toIntOrNull()
                val minute = etMinute.text.toString().toIntOrNull()
                val second = etSecond.text.toString().toIntOrNull() ?: 0
                if (hour == null || hour !in 0..23 || minute == null || minute !in 0..59 || second !in 0..59) {
                    Toast.makeText(this, "올바른 시간을 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updated = msg.copy(
                    hour = hour,
                    minute = minute,
                    second = second,
                    isRepeating = switchRepeat.isChecked,
                    isEnabled = true
                )
                MessageStore.update(this, roomId, updated)
                AlarmScheduler.cancel(this, msg.id)
                AlarmScheduler.schedule(this, updated, roomId)
                refreshList()
                Toast.makeText(this, "알림이 설정되었습니다", Toast.LENGTH_SHORT).show()
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

    private fun loadBackground(uriString: String?) {
        if (uriString != null) {
            Glide.with(this).load(uriString).centerCrop().into(binding.ivBgPreview)
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (perms.isNotEmpty()) requestPermissions(perms.toTypedArray(), 100)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { startActivity(it) }
            }
        }
    }
}
