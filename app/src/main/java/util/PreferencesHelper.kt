package com.seudominio.smsforwarder.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesHelper(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sms_forwarder_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getEndpointUrl(): String = prefs.getString("endpoint_url", "") ?: ""
    fun setEndpointUrl(url: String) = prefs.edit().putString("endpoint_url", url).apply()

    fun getEndpointLogin(): String = prefs.getString("endpoint_login", "") ?: ""
    fun setEndpointLogin(login: String) = prefs.edit().putString("endpoint_login", login).apply()

    fun getEndpointPassword(): String = prefs.getString("endpoint_password", "") ?: ""
    fun setEndpointPassword(password: String) = prefs.edit().putString("endpoint_password", password).apply()
}
