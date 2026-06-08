package com.example.scheduledmessage

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
    private val messages get() = MessageStore.getAll(this)

    private val addLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra("message", ScheduledMessage::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra("message")
            } ?: return@registerForActivityResult

            val existing = MessageStore.getAll(this).find { it.id == msg.id }
            if (existing != null) {
                MessageStore.update(this, msg)
                AlarmScheduler.cancel(this, msg.id)
            } else {
                MessageStore.add(this, msg)
            }
            if (msg.isEnabled) AlarmScheduler.schedule(this, msg)
            refreshList()
            Toast.makeText(this, "메세지가 등록되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

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

        binding.fabAdd.setOnClickListener {
            addLauncher.launch(Intent(this, AddMessageActivity::class.java))
        }

        binding.btnChangeBg.setOnClickListener {
            bgPickerLauncher.launch("image/*")
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            messages.toMutableList(),
            onToggle = { msg ->
                val updated = msg.copy(isEnabled = !msg.isEnabled)
                MessageStore.update(this, updated)
                if (updated.isEnabled) AlarmScheduler.schedule(this, updated)
                else AlarmScheduler.cancel(this, updated.id)
                refreshList()
            },
            onDelete = { msg ->
                AlarmScheduler.cancel(this, msg.id)
                MessageStore.remove(this, msg.id)
                refreshList()
                Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show()
            },
            onEdit = { msg ->
                val intent = Intent(this, AddMessageActivity::class.java).putExtra("edit_message", msg)
                addLauncher.launch(intent)
            }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter
    }

    private fun refreshList() {
        adapter.refresh(MessageStore.getAll(this))
        val isEmpty = MessageStore.getAll(this).isEmpty()
        binding.tvEmpty.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
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
