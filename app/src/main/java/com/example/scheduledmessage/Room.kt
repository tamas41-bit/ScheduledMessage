package com.example.scheduledmessage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Room(
    val id: Int,
    val name: String,
    val iconUri: String? = null   // 방 아이콘 (프로필 사진)
) : Parcelable
