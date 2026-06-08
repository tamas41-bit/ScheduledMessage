package com.example.scheduledmessage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MessageStore {
    private const val PREF = "msg_store"
    private val gson = Gson()

    private fun messagesKey(roomId: Int) = "messages_$roomId"

    fun getAll(context: Context, roomId: Int): MutableList<ScheduledMessage> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = prefs.getString(messagesKey(roomId), null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<ScheduledMessage>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun save(context: Context, roomId: Int, list: List<ScheduledMessage>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(messagesKey(roomId), gson.toJson(list)).apply()
    }

    fun add(context: Context, roomId: Int, msg: ScheduledMessage) {
        val list = getAll(context, roomId)
        list.add(msg)
        save(context, roomId, list)
    }

    fun remove(context: Context, roomId: Int, id: Int) {
        save(context, roomId, getAll(context, roomId).filter { it.id != id })
    }

    fun update(context: Context, roomId: Int, msg: ScheduledMessage) {
        save(context, roomId, getAll(context, roomId).map { if (it.id == msg.id) msg else it })
    }

    fun nextId(context: Context, roomId: Int): Int {
        val list = getAll(context, roomId)
        return if (list.isEmpty()) 1 else list.maxOf { it.id } + 1
    }

    fun removeAll(context: Context, roomId: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .remove(messagesKey(roomId)).apply()
    }

    // ── 알림 화면 설정 (per-room) ──────────────────────────────────────
    private fun p(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // 배경 이미지
    fun saveNotifBgUri(context: Context, roomId: Int, uri: String?) =
        p(context).edit().putString("notif_bg_$roomId", uri).apply()

    fun getNotifBgUri(context: Context, roomId: Int): String? =
        p(context).getString("notif_bg_$roomId", null)

    // 시계 위치
    fun saveClockPosition(context: Context, roomId: Int, xPct: Float, yPct: Float) =
        p(context).edit().putFloat("clock_x_$roomId", xPct).putFloat("clock_y_$roomId", yPct).apply()

    fun getClockXPct(context: Context, roomId: Int) =
        p(context).getFloat("clock_x_$roomId", 0.5f)

    fun getClockYPct(context: Context, roomId: Int) =
        p(context).getFloat("clock_y_$roomId", 0.2f)

    // 시계 크기
    fun saveClockSizeSp(context: Context, roomId: Int, sp: Int) =
        p(context).edit().putInt("clock_size_$roomId", sp).apply()

    fun getClockSizeSp(context: Context, roomId: Int) =
        p(context).getInt("clock_size_$roomId", 72)

    // 폰트
    fun saveClockFont(context: Context, roomId: Int, font: String) =
        p(context).edit().putString("clock_font_$roomId", font).apply()

    fun getClockFont(context: Context, roomId: Int) =
        p(context).getString("clock_font_$roomId", "기본체") ?: "기본체"

    // 커스텀 시간
    fun saveCustomTime(context: Context, roomId: Int, useCustom: Boolean, hour: Int, minute: Int) =
        p(context).edit()
            .putBoolean("clock_use_custom_$roomId", useCustom)
            .putInt("clock_custom_h_$roomId", hour)
            .putInt("clock_custom_m_$roomId", minute)
            .apply()

    fun getUseCustomTime(context: Context, roomId: Int) =
        p(context).getBoolean("clock_use_custom_$roomId", false)

    fun getCustomHour(context: Context, roomId: Int) =
        p(context).getInt("clock_custom_h_$roomId", 12)

    fun getCustomMinute(context: Context, roomId: Int) =
        p(context).getInt("clock_custom_m_$roomId", 0)

    // 날짜 표시
    fun saveShowDate(context: Context, roomId: Int, show: Boolean) =
        p(context).edit().putBoolean("clock_show_date_$roomId", show).apply()

    fun getShowDate(context: Context, roomId: Int) =
        p(context).getBoolean("clock_show_date_$roomId", true)

    // 알림 카드 색상 / 투명도
    fun saveCardColor(context: Context, roomId: Int, color: String) =
        p(context).edit().putString("card_color_$roomId", color).apply()

    fun getCardColor(context: Context, roomId: Int): String =
        p(context).getString("card_color_$roomId", "#232323") ?: "#232323"

    fun saveCardAlpha(context: Context, roomId: Int, alpha: Int) =
        p(context).edit().putInt("card_alpha_$roomId", alpha).apply()

    /** 0–255; 기본값 184 ≈ 72% */
    fun getCardAlpha(context: Context, roomId: Int): Int =
        p(context).getInt("card_alpha_$roomId", 184)
}
