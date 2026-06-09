package com.example.scheduledmessage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scheduledmessage.databinding.ItemMessageBinding

class MessageAdapter(
    private val items: MutableList<ScheduledMessage>,
    private val onEdit: (ScheduledMessage) -> Unit,
    private val onDelete: (ScheduledMessage) -> Unit
) : RecyclerView.Adapter<MessageAdapter.VH>() {

    inner class VH(val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = items[position]
        holder.b.tvText.text = msg.text
        holder.b.tvTime.text = msg.delayString()
        holder.b.btnEdit.setOnClickListener { onEdit(msg) }
        holder.b.btnDelete.setOnClickListener { onDelete(msg) }
    }

    fun refresh(newItems: List<ScheduledMessage>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}