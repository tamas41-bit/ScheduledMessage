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
}
