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

    // --- API para dados do endpoint ---
    fun getEndpointUrl(): String =
        prefs.getString("endpoint_url", "").orEmpty()

    fun setEndpointUrl(url: String): PreferencesHelper =
        apply { prefs.edit().putString("endpoint_url", url).apply() }

    fun getEndpointLogin(): String =
        prefs.getString("endpoint_login", "").orEmpty()

    fun setEndpointLogin(login: String): PreferencesHelper =
        apply { prefs.edit().putString("endpoint_login", login).apply() }

    fun getEndpointPassword(): String =
        prefs.getString("endpoint_password", "").orEmpty()

    fun setEndpointPassword(password: String): PreferencesHelper =
        apply { prefs.edit().putString("endpoint_password", password).apply() }

    // --- API para histórico de SMS ---
    fun getSmsHistory(): List<String> =
        prefs.getString("sms_history", "")
            ?.takeIf { it.isNotBlank() }
            ?.split("|")
            ?: emptyList()

    fun saveSmsHistory(list: List<String>): PreferencesHelper =
        apply { prefs.edit().putString("sms_history", list.joinToString("|")).apply() }

    fun getEndpointPhone(): String =
        prefs.getString("endpoint_phone", "").orEmpty()

    fun setEndpointPhone(phone: String): PreferencesHelper =
        apply { prefs.edit().putString("endpoint_phone", phone).apply() }

}
