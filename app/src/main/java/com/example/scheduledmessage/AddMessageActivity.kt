package com.example.scheduledmessage

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scheduledmessage.databinding.ActivityAddMessageBinding
import java.util.Calendar

class AddMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMessageBinding
    private var selectedHour = 8
    private var selectedMinute = 0
    private var editingMsg: ScheduledMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingMsg = intent.getParcelableExtra("edit_message")
        editingMsg?.let { msg ->
            binding.etMessage.setText(msg.text)
            selectedHour = msg.hour
            selectedMinute = msg.minute
            binding.switchRepeat.isChecked = msg.isRepeating
            binding.btnSave.text = "수정 완료"
        } ?: run {
            val cal = Calendar.getInstance()
            selectedHour = cal.get(Calendar.HOUR_OF_DAY)
            selectedMinute = cal.get(Calendar.MINUTE)
        }

        updateTimeDisplay()

        binding.btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                selectedHour = h
                selectedMinute = m
                updateTimeDisplay()
            }, selectedHour, selectedMinute, false).show()
        }

        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun updateTimeDisplay() {
        val ampm = if (selectedHour < 12) "오전" else "오후"
        val h = if (selectedHour % 12 == 0) 12 else selectedHour % 12
        binding.tvSelectedTime.text = "$ampm ${h}:${selectedMinute.toString().padStart(2, '0')}"
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
                isRepeating = binding.switchRepeat.isChecked
            )
        } else {
            ScheduledMessage(
                id = MessageStore.nextId(this),
                text = text,
                hour = selectedHour,
                minute = selectedMinute,
                isRepeating = binding.switchRepeat.isChecked
            )
        }

        val result = Intent().putExtra("message", msg)
        setResult(RESULT_OK, result)
        finish()
    }
}
