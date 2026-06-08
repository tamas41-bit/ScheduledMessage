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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MessageAdapter

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

        requestPermissions()
        setupRecyclerView()
        loadBackground(MessageStore.getBackgroundUri(this))
        refreshList()

        // 전송 버튼
        binding.btnSend.setOnClickListener { sendMessage() }

        // 배경 변경
        binding.btnChangeBg.setOnClickListener {
            bgPickerLauncher.launch("image/*")
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun sendMessage() {
        val text = binding.etInput.text.toString().trim()
        if (text.isEmpty()) return

        val msg = ScheduledMessage(
            id = MessageStore.nextId(this),
            text = text,
            hour = -1  // 시간 미설정 상태로 추가
        )
        MessageStore.add(this, msg)
        binding.etInput.setText("")
        refreshList()

        // 스크롤 최하단으로
        binding.rvMessages.scrollToPosition(MessageStore.getAll(this).size - 1)
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            MessageStore.getAll(this).toMutableList(),
            onEdit = { msg -> showEditTimeDialog(msg) },
            onDelete = { msg ->
                AlarmScheduler.cancel(this, msg.id)
                MessageStore.remove(this, msg.id)
                refreshList()
            }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true  // 최신 메세지가 아래에
        }
        binding.rvMessages.adapter = adapter
    }

    private fun showEditTimeDialog(msg: ScheduledMessage) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null)
        val etTime = view.findViewById<EditText>(R.id.etTime)
        val switchRepeat = view.findViewById<Switch>(R.id.switchRepeat)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)

        tvTitle.text = "\"${msg.text.take(20)}${if (msg.text.length > 20) "…" else ""}\""
        if (msg.hour >= 0) {
            etTime.setText("${msg.hour.toString().padStart(2,'0')}:${msg.minute.toString().padStart(2,'0')}:${msg.second.toString().padStart(2,'0')}")
        }
        switchRepeat.isChecked = msg.isRepeating

        AlertDialog.Builder(this)
            .setTitle("알림 시간 설정")
            .setView(view)
            .setPositiveButton("저장") { _, _ ->
                val timeText = etTime.text.toString().trim()
                val parsed = parseTime(timeText)
                if (parsed == null) {
                    Toast.makeText(this, "시간 형식: HH:MM:SS (예: 08:30:00)", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val updated = msg.copy(
                    hour = parsed.first,
                    minute = parsed.second,
                    second = parsed.third,
                    isRepeating = switchRepeat.isChecked,
                    isEnabled = true
                )
                MessageStore.update(this, updated)
                AlarmScheduler.cancel(this, msg.id)
                AlarmScheduler.schedule(this, updated)
                refreshList()
                Toast.makeText(this, "알림이 설정되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // "HH:MM:SS" 또는 "HH:MM" 파싱
    private fun parseTime(input: String): Triple<Int, Int, Int>? {
        return try {
            val parts = input.split(":")
            val h = parts[0].trim().toInt()
            val m = parts[1].trim().toInt()
            val s = if (parts.size >= 3) parts[2].trim().toInt() else 0
            if (h !in 0..23 || m !in 0..59 || s !in 0..59) null
            else Triple(h, m, s)
        } catch (e: Exception) {
            null
        }
    }

    private fun refreshList() {
        val all = MessageStore.getAll(this)
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
