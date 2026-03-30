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

    companion object {
        private const val KEY_QR_ENABLED = "qr_checkin_enabled"
        private const val KEY_PHONE_ENABLED = "phone_checkin_enabled"
        private const val KEY_STAFF_PHONE = "staff_phone_number"
    }
}
