package com.bodyswitch.checkin.data.session

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckinSettingsManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("checkin_settings", Context.MODE_PRIVATE)

    var qrCheckinEnabled: Boolean
        get() = prefs.getBoolean(KEY_QR_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_QR_ENABLED, value).apply()

    var phoneCheckinEnabled: Boolean
        get() = prefs.getBoolean(KEY_PHONE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PHONE_ENABLED, value).apply()

    var staffPhoneNumber: String
        get() = prefs.getString(KEY_STAFF_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STAFF_PHONE, value).apply()

    // 체크인 성공 시 출입문 자동 열림 여부
    var doorOpenEnabled: Boolean
        get() = prefs.getBoolean(KEY_DOOR_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DOOR_ENABLED, value).apply()

    // 열어줄 출입문 센서 ID
    var doorSensorId: String
        get() = prefs.getString(KEY_DOOR_SENSOR_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DOOR_SENSOR_ID, value).apply()

    // 선택한 출입문 이름 (설정 화면 표시용)
    var doorRoomName: String
        get() = prefs.getString(KEY_DOOR_ROOM_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DOOR_ROOM_NAME, value).apply()

    // 체크인 화면에서 만료된 수강권/이용권 섹션을 숨길지 여부
    var hideExpiredTicketsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIDE_EXPIRED, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_EXPIRED, value).apply()

    // 재입장 시 이용권을 다시 확인할지 여부.
    // true면 무차감 재입장을 건너뛰고 일반 이용권 선택 화면을 띄운다.
    var recheckTicketOnReentry: Boolean
        get() = prefs.getBoolean(KEY_RECHECK_ON_REENTRY, false)
        set(value) = prefs.edit().putBoolean(KEY_RECHECK_ON_REENTRY, value).apply()

    // 오늘 입장 가능한 이용권만 표시할지 여부.
    // 서버로 todayOnly 파라미터를 보내며, 판정(오늘 이 지점 예약 여부)은 서버가 한다.
    var todayOnlyTicketsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TODAY_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_TODAY_ONLY, value).apply()

    companion object {
        private const val KEY_QR_ENABLED = "qr_checkin_enabled"
        private const val KEY_PHONE_ENABLED = "phone_checkin_enabled"
        private const val KEY_STAFF_PHONE = "staff_phone_number"
        private const val KEY_DOOR_ENABLED = "door_open_enabled"
        private const val KEY_DOOR_SENSOR_ID = "door_sensor_id"
        private const val KEY_DOOR_ROOM_NAME = "door_room_name"
        private const val KEY_HIDE_EXPIRED = "hide_expired_tickets"
        private const val KEY_RECHECK_ON_REENTRY = "recheck_ticket_on_reentry"
        private const val KEY_TODAY_ONLY = "today_only_tickets"
    }
}
