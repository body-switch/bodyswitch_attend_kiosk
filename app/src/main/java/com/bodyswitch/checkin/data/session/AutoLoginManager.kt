package com.bodyswitch.checkin.data.session

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoLoginManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auto_login", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    fun save(username: String, password: String) {
        this.isEnabled = true
        this.username = username
        this.password = password
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ENABLED)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}
