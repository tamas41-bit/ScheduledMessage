package com.example.scheduledmessage

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scheduledmessage.databinding.ActivityRoomsBinding

class RoomsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoomsBinding
    private lateinit var adapter: RoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        refreshList()

        binding.btnAddRoom.setOnClickListener { showAddRoomDialog() }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupRecyclerView() {
        adapter = RoomAdapter(
            RoomStore.getAll(this),
            onClick = { room ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("room_id", room.id)
                    putExtra("room_name", room.name)
                }
                startActivity(intent)
            },
            onRename = { room -> showRenameDialog(room) },
            onDelete = { room -> showDeleteDialog(room) }
        )
        binding.rvRooms.layoutManager = LinearLayoutManager(this)
        binding.rvRooms.adapter = adapter
    }

    private fun showAddRoomDialog() {
        val et = EditText(this).apply {
            hint = "대화창 이름"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("새 대화창")
            .setView(et)
            .setPositiveButton("만들기") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val room = Room(id = RoomStore.nextId(this), name = name)
                RoomStore.add(this, room)
                refreshList()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showRenameDialog(room: Room) {
        val et = EditText(this).apply {
            setText(room.name)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("이름 변경")
            .setView(et)
            .setPositiveButton("저장") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isNotEmpty()) {
                    RoomStore.update(this, room.copy(name = name))
                    refreshList()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteDialog(room: Room) {
        AlertDialog.Builder(this)
            .setTitle("대화창 삭제")
            .setMessage("\"${room.name}\" 대화창과 모든 예약 메세지를 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                // 해당 방의 알람 전부 취소
                MessageStore.getAll(this, room.id).forEach {
                    AlarmScheduler.cancel(this, it.id)
                }
                MessageStore.removeAll(this, room.id)
                RoomStore.remove(this, room.id)
                refreshList()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun refreshList() {
        val rooms = RoomStore.getAll(this)
        adapter.refresh(rooms)
        binding.tvEmpty.visibility =
            if (rooms.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
