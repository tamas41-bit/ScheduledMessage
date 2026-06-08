package com.example.scheduledmessage

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ActivityAddMessageBinding
import java.util.Calendar

class AddMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMessageBinding
    private var selectedHour = 8
    private var selectedMinute = 0
    private var selectedSecond = 0
    private var selectedIconUri: String? = null
    private var editingMsg: ScheduledMessage? = null

    private val iconPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // URI 권한 영구 유지
            contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedIconUri = it.toString()
            Glide.with(this).load(it).circleCrop().into(binding.ivIconPreview)
            binding.tvIconHint.text = "아이콘 선택됨"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingMsg = intent.getParcelableExtra("edit_message")
        editingMsg?.let { msg ->
            binding.etMessage.setText(msg.text)
            selectedHour = msg.hour
            selectedMinute = msg.minute
            selectedSecond = msg.second
            selectedIconUri = msg.iconUri
            binding.switchRepeat.isChecked = msg.isRepeating
            binding.btnSave.text = "수정 완료"
            msg.iconUri?.let { uri ->
                Glide.with(this).load(Uri.parse(uri)).circleCrop().into(binding.ivIconPreview)
                binding.tvIconHint.text = "아이콘 선택됨"
            }
        } ?: run {
            val cal = Calendar.getInstance()
            selectedHour = cal.get(Calendar.HOUR_OF_DAY)
            selectedMinute = cal.get(Calendar.MINUTE)
            selectedSecond = cal.get(Calendar.SECOND)
        }

        updateTimeDisplay()

        binding.btnPickTime.setOnClickListener { showTimePickerDialog() }
        binding.btnPickIcon.setOnClickListener { iconPickerLauncher.launch("image/*") }
        binding.btnRemoveIcon.setOnClickListener {
            selectedIconUri = null
            binding.ivIconPreview.setImageResource(R.drawable.ic_notification_icon)
            binding.tvIconHint.text = "기본 아이콘 사용"
        }

        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun showTimePickerDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_time_picker, null)
        val npHour = view.findViewById<NumberPicker>(R.id.npHour)
        val npMinute = view.findViewById<NumberPicker>(R.id.npMinute)
        val npSecond = view.findViewById<NumberPicker>(R.id.npSecond)

        npHour.minValue = 0
        npHour.maxValue = 23
        npHour.value = selectedHour
        npHour.setFormatter { i -> i.toString().padStart(2, '0') }

        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.value = selectedMinute
        npMinute.setFormatter { i -> i.toString().padStart(2, '0') }

        npSecond.minValue = 0
        npSecond.maxValue = 59
        npSecond.value = selectedSecond
        npSecond.setFormatter { i -> i.toString().padStart(2, '0') }

        AlertDialog.Builder(this)
            .setTitle("시간 선택")
            .setView(view)
            .setPositiveButton("확인") { _, _ ->
                selectedHour = npHour.value
                selectedMinute = npMinute.value
                selectedSecond = npSecond.value
                updateTimeDisplay()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateTimeDisplay() {
        val ampm = if (selectedHour < 12) "오전" else "오후"
        val h = if (selectedHour % 12 == 0) 12 else selectedHour % 12
        binding.tvSelectedTime.text = "$ampm ${h}:${selectedMinute.toString().padStart(2, '0')}:${selectedSecond.toString().padStart(2, '0')}"
    }

    private fun save() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "메세지를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val editing = editingMsg
        val msg = if (editing != null) {
            editing.copy(
                text = text,
                hour = selectedHour,
                minute = selectedMinute,
                second = selectedSecond,
                isRepeating = binding.switchRepeat.isChecked,
                iconUri = selectedIconUri
            )
        } else {
            ScheduledMessage(
                id = MessageStore.nextId(this),
                text = text,
                hour = selectedHour,
                minute = selectedMinute,
                second = selectedSecond,
                isRepeating = binding.switchRepeat.isChecked,
                iconUri = selectedIconUri
            )
        }

        val result = Intent().putExtra("message", msg)
        setResult(RESULT_OK, result)
        finish()
    }
}
