package com.example.scheduledmessage

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ItemRoomBinding

class RoomAdapter(
    private val items: MutableList<Room>,
    private val onClick: (Room) -> Unit,
    private val onIconClick: (Room) -> Unit,
    private val onRename: (Room) -> Unit,
    private val onDelete: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.VH>() {

    inner class VH(val b: ItemRoomBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val room = items[position]
        holder.b.tvRoomName.text = room.name

        // 방 아이콘 — 설정된 이미지 or 기본 아이콘
        if (room.iconUri != null) {
            Glide.with(holder.b.root.context)
                .load(Uri.parse(room.iconUri))
                .circleCrop()
                .into(holder.b.ivRoomIcon)
        } else {
            holder.b.ivRoomIcon.setImageResource(android.R.drawable.ic_menu_camera)
        }

        holder.b.root.setOnClickListener { onClick(room) }
        holder.b.ivRoomIcon.setOnClickListener { onIconClick(room) }
        holder.b.btnRename.setOnClickListener { onRename(room) }
        holder.b.btnDeleteRoom.setOnClickListener { onDelete(room) }
    }

    fun refresh(newItems: List<Room>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
