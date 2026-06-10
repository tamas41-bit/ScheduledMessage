package com.example.scheduledmessage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScheduledMessage(
    val id: Int,
    val text: String,
    val delaySeconds: Int = 0,   // 알림 시작 후 몇 초 뒤에 표시
    val isEnabled: Boolean = true,
    val iconUri: String? = null,     // 메세지별 프로필 사진 (null=기본값 사용)
    val textColor: String? = null,   // 메세지별 글자색 hex (null=기본값 사용)
    val textBold: Boolean = false,   // 글자 굵기
    val textSizeSp: Int? = null,     // 글자 크기 sp (null=기본값 14sp)
    val imageUri: String? = null,    // 카드에 표시할 이미지 (null=없음)
    val nameColor: String? = null,   // 이름 글자색 hex (null=기본값)
    val nameSizeSp: Int? = null      // 이름 글자 크기 sp (null=기본값 16sp)
) : Parcelable {
    fun delayString(): String = if (delaySeconds == 0) "즉시" else "${delaySeconds}초 후"
}