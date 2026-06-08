package com.example.scheduledmessage

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * 새 알림 카드가 위에서 아래로 슬라이드되며 등장하고,
 * 기존 카드들은 자연스럽게 아래로 밀리는 ItemAnimator.
 */
class SlideInTopAnimator : DefaultItemAnimator() {

    init {
        addDuration  = 350
        moveDuration = 300
        removeDuration = 250
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        // 시작 상태: 카드 높이만큼 위에서, 투명하게
        val startY = holder.itemView.context.resources.displayMetrics.density * 72f
        holder.itemView.translationY = -startY
        holder.itemView.alpha = 0f
        // super가 translationY → 0, alpha → 1 로 애니메이션
        return super.animateAdd(holder)
    }
}
