package com.example.scheduledmessage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityNotificationScreenBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationScreenBinding
    private lateinit var cardAdapter: NotificationCardAdapter
    private val clockHandler = Handler(Looper.getMainLooper())

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

        supportActionBar?.hide()

        setupRecyclerView()
        val savedBg = getSharedPreferences("notif_screen_prefs", MODE_PRIVATE).getString("bg_uri", null)
        loadBackground(savedBg)
        startClock()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnChangeBg.setOnClickListener { bgPickerLauncher.launch("image/*") }

        // 알람으로 시작된 경우 카드 추가
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 이미 화면이 열려있을 때 새 알람이 오면 카드만 추가
        handleNotificationIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
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
        binding.rvCards.adapter = cardAdapter
    }

    private fun loadBackground(uriString: String?) {
        if (uriString != null) {
            Glide.with(this).load(uriString).centerCrop().into(binding.ivBackground)
        }
    }

    private fun startClock() {
        val clockRunnable = object : Runnable {
            override fun run() {
                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateFmt = SimpleDateFormat("M월 d일 EEEE", Locale("ko"))
                binding.tvTime.text = timeFmt.format(Date())
                binding.tvDate.text = dateFmt.format(Date())
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)
    }
}
