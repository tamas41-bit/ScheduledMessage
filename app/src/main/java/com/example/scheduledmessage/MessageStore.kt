package com.example.scheduledmessage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MessageStore {
    private const val PREF = "msg_store"
    private const val KEY_MESSAGES = "messages"
    private const val KEY_BG = "background_uri"
    private val gson = Gson()

    fun getAll(context: Context): MutableList<ScheduledMessage> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MESSAGES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<ScheduledMessage>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun save(context: Context, list: List<ScheduledMessage>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_MESSAGES, gson.toJson(list)).apply()
    }

    fun add(context: Context, msg: ScheduledMessage) {
        val list = getAll(context)
        list.add(msg)
        save(context, list)
    }

    fun remove(context: Context, id: Int) {
        val list = getAll(context).filter { it.id != id }
        save(context, list)
    }

    fun update(context: Context, msg: ScheduledMessage) {
        val list = getAll(context).map { if (it.id == msg.id) msg else it }
        save(context, list)
    }

    fun nextId(context: Context): Int {
        val list = getAll(context)
        return if (list.isEmpty()) 1 else list.maxOf { it.id } + 1
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
