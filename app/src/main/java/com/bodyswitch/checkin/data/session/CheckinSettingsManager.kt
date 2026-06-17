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

    companion object {
        private const val KEY_QR_ENABLED = "qr_checkin_enabled"
        private const val KEY_PHONE_ENABLED = "phone_checkin_enabled"
        private const val KEY_STAFF_PHONE = "staff_phone_number"
        private const val KEY_DOOR_ENABLED = "door_open_enabled"
        private const val KEY_DOOR_SENSOR_ID = "door_sensor_id"
        private const val KEY_DOOR_ROOM_NAME = "door_room_name"
    }
}
