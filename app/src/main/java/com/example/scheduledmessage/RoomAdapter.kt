package com.example.scheduledmessage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scheduledmessage.databinding.ItemRoomBinding

class RoomAdapter(
    private val items: MutableList<Room>,
    private val onClick: (Room) -> Unit,
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
        holder.b.root.setOnClickListener { onClick(room) }
        holder.b.btnRename.setOnClickListener { onRename(room) }
        holder.b.btnDeleteRoom.setOnClickListener { onDelete(room) }
    }

    fun refresh(newItems: List<Room>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
