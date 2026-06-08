package com.example.scheduledmessage

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
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
        holder.b.tvCardMessage.text  = card.messageText
        holder.b.tvCardTime.text     = relativeTime(card.timestampMs)

        val cornerPx = (8 * holder.b.root.context.resources.displayMetrics.density).toInt()
        if (card.roomIconUri != null) {
            Glide.with(holder.b.root.context)
                .load(Uri.parse(card.roomIconUri))
                .apply(RequestOptions().centerCrop().transform(RoundedCorners(cornerPx)))
                .into(holder.b.ivCardIcon)
        } else {
            holder.b.ivCardIcon.setImageResource(R.drawable.ic_notification_icon)
        }
    }

    fun addCard(card: NotificationCard) {
        items.add(0, card)
        notifyItemInserted(0)
    }

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /** 수신 시각 → 상대 시간 문자열 ("방금", "3분 전", "1시간 전" …) */
    private fun relativeTime(timestampMs: Long): String {
        val diffSec = (System.currentTimeMillis() - timestampMs) / 1000L
        return when {
            diffSec < 60        -> "방금"
            diffSec < 3600      -> "${diffSec / 60}분 전"
            diffSec < 86400     -> "${diffSec / 3600}시간 전"
            else                -> "${diffSec / 86400}일 전"
        }
    }

    /** RecyclerView에 붙이면 왼쪽 스와이프로 카드 삭제 */
    fun attachSwipeToDismiss(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                removeAt(vh.adapterPosition)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }
}
