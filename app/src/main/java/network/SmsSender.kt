package com.seudominio.smsforwarder.network

import android.os.Build
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log

object SmsSender {

    // OkHttpClient recomendado como singleton (evita overhead de criação múltipla)
    private val client by lazy { OkHttpClient() }

    /**
     * Envia dados do SMS para o endpoint especificado.
     *
     * @param url       Endpoint destino (POST)
     * @param login     Usuário para autenticação
     * @param password  Senha para autenticação
     * @param sender    Número/Rementente do SMS
     * @param body      Corpo da mensagem
     * @param timestamp Timestamp do recebimento
     */
    fun sendSmsToEndpoint(
        url: String,
        login: String,
        password: String,
        sender: String,
        body: String,
        timestamp: Long
    ) {
        try {
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

            client.newCall(request).execute().use { response ->
                // Pode tratar resposta ou erros específicos aqui se necessário
                // Para produção: usar log apenas em erros, se for imprescindível.
                // Não abrir response.body como string somente para log, pode ser stream único.
            }
        } catch (e: Exception) {
             Log.e("SmsSender", "Erro ao enviar SMS para endpoint", e)
        }
    }
}
