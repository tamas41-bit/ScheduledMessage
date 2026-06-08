package com.example.scheduledmessage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Room(
    val id: Int,
    val name: String
) : Parcelable
