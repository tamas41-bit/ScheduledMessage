package com.example.scheduledmessage

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * 새 알림 카드: Scale(0.8→1.05→1.0) + Fade-in + Y진입(-10px→0)  — Spring 효과
 * 기존 알림 카드: Y축 밀림 + 미세 Scale(1.0→0.95→1.0)  — 입체 스택 느낌
 * add / move 완전히 동기화(딜레이 없음)
 */
class NotificationCardAnimator : DefaultItemAnimator() {

    init {
        addDuration    = 380
        moveDuration   = 320
        removeDuration = 220
    }

    // ── 새 카드 등장 ──────────────────────────────────────────────
    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        val density = view.context.resources.displayMetrics.density

        // 시작 상태
        view.alpha       = 0f
        view.scaleX      = 0.80f
        view.scaleY      = 0.80f
        view.translationY = -10f * density   // 10dp 위에서 시작

        dispatchAddStarting(holder)

        // 1단계: 1.05 배로 확장하며 페이드인 + Y 안착
        view.animate()
            .alpha(1f)
            .scaleX(1.05f)
            .scaleY(1.05f)
            .translationY(0f)
            .setDuration(260)
            .setInterpolator(SpringInterpolator())   // cubic-bezier(0.25,1,0.5,1) 근사
            .withEndAction {
                // 2단계: 살짝 커진 것을 다시 1.0 으로 복귀 (스프링 감쇠)
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(160)
                    .setInterpolator(DecelerateInterpolator(2f))
                    .withEndAction {
                        dispatchAddFinished(holder)
                        // 혹시 남은 속성 초기화
                        view.alpha       = 1f
                        view.scaleX      = 1f
                        view.scaleY      = 1f
                        view.translationY = 0f
                    }
                    .start()
            }
            .start()

        return true
    }

    // ── 기존 카드 아래로 밀림 ──────────────────────────────────────
    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int, fromY: Int,
        toX: Int,   toY: Int
    ): Boolean {
        val view = holder.itemView
        val deltaY = (fromY - toY).toFloat()

        // 레이아웃이 이미 toY에 위치했으므로, translationY로 fromY에서 시작하게 함
        ViewCompat.offsetTopAndBottom(view, 0)   // 기존 offset 초기화
        view.translationY = deltaY

        dispatchMoveStarting(holder)

        view.animate()
            .translationY(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(moveDuration)
            .setInterpolator(FastOutSlowInInterpolator())   // ease-in-out
            .withEndAction {
                // 이동 완료 후 원래 크기로 복귀
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        dispatchMoveFinished(holder)
                        view.translationY = 0f
                        view.scaleX = 1f
                        view.scaleY = 1f
                    }
                    .start()
            }
            .start()

        return true
    }

    // ── 카드 제거 (스와이프 시) ────────────────────────────────────
    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        dispatchRemoveStarting(holder)

        view.animate()
            .alpha(0f)
            .translationX(-view.width.toFloat() * 0.3f)
            .scaleY(0.85f)
            .setDuration(removeDuration)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                view.alpha         = 1f
                view.translationX  = 0f
                view.scaleY        = 1f
                dispatchRemoveFinished(holder)
            }
            .start()

        return true
    }

    /**
     * cubic-bezier(0.25, 1, 0.5, 1) 근사 — 초반 빠르고 끝에 부드럽게 감속
     * Android API 21+ PathInterpolator 없이 수식으로 구현
     */
    private class SpringInterpolator : android.view.animation.Interpolator {
        override fun getInterpolation(t: Float): Float {
            // approximation of cubic-bezier(0.25, 1, 0.5, 1)
            val t2 = t - 1f
            return t2 * t2 * t2 + 1f   // ease-out cubic
        }
    }
}
