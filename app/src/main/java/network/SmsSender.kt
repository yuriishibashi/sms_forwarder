package com.seudominio.smsforwarder.network

import android.os.Build
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SmsSender {

    private val client by lazy { OkHttpClient() }

    fun sendSmsToEndpoint(
        url: String,
        login: String,
        password: String,
        sender: String,
        body: String,
        timestamp: Long
    ): Boolean {
        return try {
            val json = JSONObject().apply {
                put("usuario", login)
                put("senha", password)
                put("remetente", sender)
                put("mensagem", body)
                put("timestamp", timestamp)
                put("device_id", Build.MODEL ?: "android")
            }

            val requestBody = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            Log.d("SmsSender", "⏳ Enviando SMS para endpoint: $url")
            Log.d("SmsSender", "Payload JSON: $json")

            client.newCall(request).execute().use { response ->
                val isSuccessful = response.isSuccessful
                val statusCode = response.code
                val responseText = response.body?.string()

                if (isSuccessful) {
                    Log.i("SmsSender", "✅ Sucesso: Status $statusCode")
                    Log.i("SmsSender", "Resposta do servidor: $responseText")
                } else {
                    Log.e("SmsSender", "❌ Falha: Status $statusCode")
                    Log.e("SmsSender", "Erro retornado: $responseText")
                }

                return isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SmsSender", "❌ Erro ao enviar SMS para endpoint", e)
            return false
        }
    }
}
