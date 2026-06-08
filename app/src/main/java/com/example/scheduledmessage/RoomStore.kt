package com.example.scheduledmessage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RoomStore {
    private const val PREF = "room_store"
    private const val KEY_ROOMS = "rooms"
    private val gson = Gson()

    fun getAll(context: Context): MutableList<Room> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ROOMS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Room>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun save(context: Context, list: List<Room>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_ROOMS, gson.toJson(list)).apply()
    }

    fun add(context: Context, room: Room) {
        val list = getAll(context)
        list.add(room)
        save(context, list)
    }

    fun remove(context: Context, id: Int) {
        save(context, getAll(context).filter { it.id != id })
    }

    fun update(context: Context, room: Room) {
        save(context, getAll(context).map { if (it.id == room.id) room else it })
    }

    fun nextId(context: Context): Int {
        val list = getAll(context)
        return if (list.isEmpty()) 1 else list.maxOf { it.id } + 1
    }
}
