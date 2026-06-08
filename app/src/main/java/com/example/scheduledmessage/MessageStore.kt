package com.example.scheduledmessage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MessageStore {
    private const val PREF = "msg_store"
    private const val KEY_BG = "background_uri"
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

    fun getBackgroundUri(context: Context): String? {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_BG, null)
    }

    fun saveBackgroundUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_BG, uri).apply()
    }

    // 시계 설정
    private const val KEY_CLOCK_X = "clock_x_pct"
    private const val KEY_CLOCK_Y = "clock_y_pct"
    private const val KEY_CLOCK_SIZE = "clock_size_sp"
    private const val KEY_CLOCK_FONT = "clock_font"
    private const val KEY_CLOCK_CUSTOM_H = "clock_custom_hour"
    private const val KEY_CLOCK_CUSTOM_M = "clock_custom_minute"
    private const val KEY_CLOCK_USE_CUSTOM = "clock_use_custom"
    private const val KEY_CLOCK_SHOW_DATE = "clock_show_date"

    fun saveClockPosition(context: Context, xPct: Float, yPct: Float) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_CLOCK_X, xPct).putFloat(KEY_CLOCK_Y, yPct).apply()
    }

    fun getClockXPct(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getFloat(KEY_CLOCK_X, 0.5f)

    fun getClockYPct(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getFloat(KEY_CLOCK_Y, 0.2f)

    fun saveClockSizeSp(context: Context, sp: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt(KEY_CLOCK_SIZE, sp).apply()
    }

    fun getClockSizeSp(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_CLOCK_SIZE, 72)

    fun saveClockFont(context: Context, font: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_CLOCK_FONT, font).apply()
    }

    fun getClockFont(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_CLOCK_FONT, "DEFAULT") ?: "DEFAULT"

    fun saveCustomTime(context: Context, useCustom: Boolean, hour: Int, minute: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_CLOCK_USE_CUSTOM, useCustom)
            .putInt(KEY_CLOCK_CUSTOM_H, hour)
            .putInt(KEY_CLOCK_CUSTOM_M, minute)
            .apply()
    }

    fun getUseCustomTime(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_CLOCK_USE_CUSTOM, false)

    fun getCustomHour(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_CLOCK_CUSTOM_H, 12)

    fun getCustomMinute(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_CLOCK_CUSTOM_M, 0)

    fun saveShowDate(context: Context, show: Boolean) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_CLOCK_SHOW_DATE, show).apply()
    }

    fun getShowDate(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_CLOCK_SHOW_DATE, true)
}
