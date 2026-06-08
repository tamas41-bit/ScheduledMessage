package com.example.scheduledmessage

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scheduledmessage.databinding.ItemNotificationCardBinding

class NotificationCardAdapter : RecyclerView.Adapter<NotificationCardAdapter.VH>() {

    private val items = mutableListOf<NotificationCard>()

    inner class VH(val b: ItemNotificationCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNotificationCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val card = items[position]
        holder.b.tvCardRoomName.text = card.roomName
        holder.b.tvCardMessage.text = card.messageText
        holder.b.tvCardTime.text = card.time

        if (card.roomIconUri != null) {
            Glide.with(holder.b.root.context)
                .load(Uri.parse(card.roomIconUri))
                .circleCrop()
                .into(holder.b.ivCardIcon)
        } else {
            holder.b.ivCardIcon.setImageResource(R.drawable.ic_notification_icon)
        }

        holder.b.btnCardClose.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                items.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
    }

    fun addCard(card: NotificationCard) {
        items.add(0, card)
        notifyItemInserted(0)
    }
}
