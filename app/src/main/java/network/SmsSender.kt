package com.seudominio.smsforwarder.network

import android.os.Build
import androidx.annotation.OptIn
import android.util.Log
import androidx.media3.common.util.UnstableApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SmsSender {
    @OptIn(UnstableApi::class)
    fun sendSmsToEndpoint(
        url: String,
        login: String,
        password: String,
        sender: String,
        body: String,
        timestamp: Long
    ) {
        try {
            Log.d("SmsSender_LOG", "Chamou sendSmsToEndpoint!")
            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("usuario", login)
                put("senha", password)
                put("remetente", sender)
                put("mensagem", body)
                put("timestamp", timestamp)
                put("device_id", Build.MODEL ?: "android") // Identificação simples do aparelho
            }

            val requestBody = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            Log.d("SmsSender_LOG", "POST status: ${response.code}, resposta: ${response.body?.string()}")
            response.close()
        } catch (e: Exception) {
            Log.e("SmsSender_LOG", "Erro ao enviar SMS para endpoint", e)
        }

    }
}
