package com.example.smsforwarder

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.webkit.URLUtil.isValidUrl
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.seudominio.smsforwarder.util.PreferencesHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var etEndpointUrl: EditText
    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var btnTestEndpoint: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvSmsInfo: TextView
    private lateinit var prefs: PreferencesHelper

    private val handler = Handler(Looper.getMainLooper())
    private val historyRunnable = object : Runnable {
        override fun run() {
            showHistory()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PreferencesHelper(this)
        etEndpointUrl = findViewById(R.id.etEndpointUrl)
        etLogin = findViewById(R.id.etLogin)
        etPassword = findViewById(R.id.etPassword)
        btnSave = findViewById(R.id.btnSave)
        btnTestEndpoint = findViewById(R.id.btnTestEndpoint)
        tvStatus = findViewById(R.id.tvStatus)
        tvSmsInfo = findViewById(R.id.tvSmsInfo)

        etEndpointUrl.setText(prefs.getEndpointUrl())
        etLogin.setText(prefs.getEndpointLogin())
        etPassword.setText(prefs.getEndpointPassword())

        btnSave.setOnClickListener {
            val url = etEndpointUrl.text.toString()
            if (!isValidUrl(url)) {
                tvStatus.text = "URL inválida! Use formato: https://exemplo.com"
                tvStatus.setTextColor(Color.RED)
                return@setOnClickListener
            }
            prefs.setEndpointUrl(url)
                .setEndpointLogin(etLogin.text.toString())
                .setEndpointPassword(etPassword.text.toString())

            tvStatus.text = "✅ Configurações salvas!"
            tvStatus.setTextColor(Color.parseColor("#4CAF50"))
        }

        btnTestEndpoint.setOnClickListener {
            testEndpoint()
        }

        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnShowPassword = findViewById<ImageButton>(R.id.btnShowPassword)
        var isPasswordVisible = false
        btnShowPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnShowPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnShowPassword.setImageResource(android.R.drawable.ic_menu_view)
            }
            etPassword.setSelection(etPassword.text.length) // mantém cursor ao final
        }


        checkAndRequestSmsPermissions()
        checkAndRequestNotificationPermission()
        requestDefaultSmsApp()
        updateSmsInfoMessage()
    }

    override fun onResume() {
        super.onResume()
        updateSmsInfoMessage()
        handler.post(historyRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(historyRunnable)
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }

    private fun testEndpoint() {
        val url = etEndpointUrl.text.toString().trim()
        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (url.isEmpty()) {
            tvStatus.text = "❌ Informe a URL do endpoint primeiro"
            tvStatus.setTextColor(android.graphics.Color.RED)
            return
        }

        if (!isValidUrl(url)) {
            tvStatus.text = "❌ URL inválida! Use formato: https://exemplo.com"
            tvStatus.setTextColor(android.graphics.Color.RED)
            return
        }

        // Desabilita o botão durante o teste
        btnTestEndpoint.isEnabled = false
        btnTestEndpoint.text = "🔄 Testando..."
        tvStatus.text = "🔄 Testando conectividade..."
        tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))

        Thread {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                // Primeiro testa conectividade básica
                val headRequest = okhttp3.Request.Builder()
                    .url(url)
                    .head()
                    .build()

                client.newCall(headRequest).execute().use { response ->
                    runOnUiThread {
                        if (response.isSuccessful) {
                            // Se conectividade OK, testa o endpoint completo
                            testFullEndpoint(client, url, login, password)
                        } else {
                            showTestResult(
                                "⚠️ Endpoint respondeu com código: ${response.code}",
                                android.graphics.Color.parseColor("#FF9800")
                            )
                        }
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                runOnUiThread {
                    showTestResult(
                        "❌ Erro: Host não encontrado. Verifique a URL.",
                        android.graphics.Color.RED
                    )
                }
            } catch (e: java.net.SocketTimeoutException) {
                runOnUiThread {
                    showTestResult(
                        "❌ Timeout: Servidor não respondeu em 10s",
                        android.graphics.Color.RED
                    )
                }
            } catch (e: javax.net.ssl.SSLException) {
                runOnUiThread {
                    showTestResult(
                        "❌ Erro SSL: Certificado inválido",
                        android.graphics.Color.RED
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showTestResult(
                        "❌ Erro de conexão: ${e.message}",
                        android.graphics.Color.RED
                    )
                }
            }
        }.start()
    }

    // NOVO: Teste completo do endpoint com dados simulados
    private fun testFullEndpoint(client: okhttp3.OkHttpClient, url: String, login: String, password: String) {
        Thread {
            try {
                val json = org.json.JSONObject().apply {
                    put("usuario", login.ifEmpty { "teste" })
                    put("senha", password.ifEmpty { "teste" })
                    put("remetente", "TESTE")
                    put("mensagem", "Teste de conectividade do app SMS Redirection")
                    put("timestamp", System.currentTimeMillis())
                    put("device_id", android.os.Build.MODEL ?: "android")
                    put("teste", true) // Indica que é um teste
                }

                val requestBody = json.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    runOnUiThread {
                        when (response.code) {
                            200, 201 -> showTestResult(
                                "✅ Endpoint funcionando perfeitamente!",
                                android.graphics.Color.parseColor("#4CAF50")
                            )
                            401, 403 -> showTestResult(
                                "⚠️ Endpoint OK, mas credenciais inválidas",
                                android.graphics.Color.parseColor("#FF9800")
                            )
                            404 -> showTestResult(
                                "❌ Endpoint não encontrado (404)",
                                android.graphics.Color.RED
                            )
                            500 -> showTestResult(
                                "⚠️ Erro interno do servidor (500)",
                                android.graphics.Color.parseColor("#FF9800")
                            )
                            else -> showTestResult(
                                "⚠️ Resposta inesperada: ${response.code}",
                                android.graphics.Color.parseColor("#FF9800")
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showTestResult(
                        "❌ Erro ao testar endpoint: ${e.message}",
                        android.graphics.Color.RED
                    )
                }
            }
        }.start()
    }

    // NOVO: Exibe resultado do teste
    private fun showTestResult(message: String, color: Int) {
        tvStatus.text = message
        tvStatus.setTextColor(color)

        // Reabilita o botão
        btnTestEndpoint.isEnabled = true
        btnTestEndpoint.text = "🔗 Testar Endpoint"
    }

    private fun showHistory() {
        val historyBox = findViewById<LinearLayout>(R.id.historyBox)
        historyBox.removeAllViews()
        val historyList = prefs.getSmsHistory()
        if (historyList.isEmpty()) {
            historyBox.addView(TextView(this).apply { text = "Nenhum código recebido ainda." })
        } else {
            for (item in historyList) {
                historyBox.addView(TextView(this).apply {
                    text = item
                    textSize = 16f
                    setPadding(6, 4, 6, 4)
                    setTextColor(android.graphics.Color.parseColor("#37474F"))
                })
            }
        }
    }

    // Exibe o alerta fixo para o usuário
    private fun updateSmsInfoMessage() {
        tvSmsInfo.text = if (isDefaultSmsApp() && hasAllSmsPermissions()) {
            "Este app está pronto para capturar automaticamente qualquer SMS recebido e encaminhá-lo ao endpoint configurado."
        } else {
            "Para funcionar corretamente, defina este app como aplicativo padrão de SMS e conceda as permissões solicitadas."
        }
    }

    private fun isDefaultSmsApp(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        } else true

    private fun hasAllSmsPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
        )
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun checkAndRequestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            requestPermissionsLauncher.launch(notGranted.toTypedArray())
        } else {
            tvStatus.text = "Permissões de SMS concedidas."
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            tvStatus.text = if (result.all { it.value }) {
                "Permissões de SMS concedidas."
            } else {
                "Permissões de SMS negadas."
            }
        }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 230)
        }
    }

    private fun requestDefaultSmsApp() {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            startActivityForResult(intent, 1001)
        }
    }
}
