    package com.seudominio.smsforwarder.util

    import android.content.Context
    import android.content.SharedPreferences

    class PreferencesHelper(context: Context) {

        private val PREFS_NAME = "SmsForwarderPrefs"
        private val ENDPOINT_URL_KEY = "endpoint_url"
        private val ENDPOINT_LOGIN_KEY = "endpoint_login"
        private val ENDPOINT_PASSWORD_KEY = "endpoint_password"
        private val SMS_HISTORY_KEY = "sms_history" // Para SMS recebidos
        private val SENT_SMS_HISTORY_KEY = "sent_sms_history" // NOVO: Para SMS enviados

        private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun getEndpointUrl(): String = prefs.getString(ENDPOINT_URL_KEY, "") ?: ""
        fun setEndpointUrl(url: String) = prefs.edit().putString(ENDPOINT_URL_KEY, url).apply()

        fun getEndpointLogin(): String = prefs.getString(ENDPOINT_LOGIN_KEY, "") ?: ""
        fun setEndpointLogin(login: String) = prefs.edit().putString(ENDPOINT_LOGIN_KEY, login).apply()

        fun getEndpointPassword(): String = prefs.getString(ENDPOINT_PASSWORD_KEY, "") ?: ""
        fun setEndpointPassword(password: String) = prefs.edit().putString(ENDPOINT_PASSWORD_KEY, password).apply()

        // Método para adicionar SMS recebido (adaptado para usar List<String> diretamente)
        fun addSmsHistory(historyList: List<String>) {
            // Converte para Set para remover duplicatas e depois para List para salvar como StringSet
            val historySet = historyList.toSet()
            prefs.edit().putStringSet(SMS_HISTORY_KEY, historySet).apply()
        }

        fun getSmsHistory(): List<String> =
            prefs.getStringSet(SMS_HISTORY_KEY, emptySet())?.toList()?.sortedDescending() ?: emptyList() // Opcional: ordenar para manter ordem

        // NOVO: Métodos para histórico de SMS enviados
        fun addSentSmsLog(logEntry: String) {
            val currentSentHistory = getSentSmsHistory().toMutableList()
            currentSentHistory.add(0, logEntry) // Adiciona no início
            if (currentSentHistory.size > 15) { // Limita a 15 entradas
                currentSentHistory.removeAt(currentSentHistory.size - 1)
            }
            val historySet = currentSentHistory.toSet()
            prefs.edit().putStringSet(SENT_SMS_HISTORY_KEY, historySet).apply()
        }

        fun getSentSmsHistory(): List<String> =
            prefs.getStringSet(SENT_SMS_HISTORY_KEY, emptySet())?.toList()?.sortedDescending() ?: emptyList() // Opcional: ordenar
    }
    