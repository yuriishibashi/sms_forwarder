package com.example.smsforwarder

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope // Importar para usar coroutines com o ciclo de vida da Activity
import com.seudominio.smsforwarder.network.SmsSender // Importar seu SmsSender
import com.seudominio.smsforwarder.util.PreferencesHelper
import kotlinx.coroutines.launch // Importar para lançar coroutines
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var etEndpointUrl: EditText
    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvSmsInfo: TextView
    private lateinit var prefs: PreferencesHelper

    // Novos elementos para o histórico de SMS enviados
    private lateinit var btnSendTestSms: Button // Botão para simular o envio
    private lateinit var sentHistoryBox: LinearLayout
    private lateinit var tvSentStatus: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val historyRunnable = object : Runnable {
        override fun run() {
            showReceivedHistory() // Atualizado para ser mais específico
            showSentHistory() // Novo: Atualiza o histórico de envios
            handler.postDelayed(this, 1000)
        }
    }

    // Launcher para a requisição de permissões de SMS
    private val requestSmsPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            tvStatus.text = if (result.all { it.value }) {
                "Permissões de SMS concedidas."
            } else {
                "Permissões de SMS negadas."
            }
            updateSmsInfoMessage() // Atualiza a mensagem de status após o resultado da permissão
        }

    // Launcher para a requisição de permissão de Notificação (para Android 13+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permissão de notificação concedida
                // Pode adicionar feedback visual aqui se desejar
            } else {
                // Permissão de notificação negada
                // Pode adicionar feedback visual aqui se desejar
            }
            // Não é necessário atualizar tvSmsInfo aqui, pois ela lida com permissões de SMS
        }

    // Launcher para a requisição de aplicativo de SMS padrão
    private val requestRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // O resultado pode ser verificado aqui se necessário (result.resultCode)
        updateSmsInfoMessage() // Atualiza a mensagem de status após o usuário interagir com a requisição de app padrão
    }

    // NOVO: BroadcastReceiver para atualizar o histórico de SMS enviados
    private val updateSentHistoryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.smsforwarder.UPDATE_SENT_HISTORY") {
                showSentHistory() // Chama a função para atualizar a lista na tela
            }
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
        tvStatus = findViewById(R.id.tvStatus)
        tvSmsInfo = findViewById(R.id.tvSmsInfo)

        // Inicialização dos novos elementos
        btnSendTestSms = findViewById(R.id.btnSendTestSms)
        sentHistoryBox = findViewById(R.id.sentHistoryBox)
        tvSentStatus = findViewById(R.id.tvSentStatus)

        etEndpointUrl.setText(prefs.getEndpointUrl())
        etLogin.setText(prefs.getEndpointLogin())
        etPassword.setText(prefs.getEndpointPassword())

        btnSave.setOnClickListener {
            prefs.setEndpointUrl(etEndpointUrl.text.toString())
            prefs.setEndpointLogin(etLogin.text.toString())
            prefs.setEndpointPassword(etPassword.text.toString())
            tvStatus.text = "Configurações salvas com sucesso!"
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

        // Listener para o novo botão de envio de teste
        btnSendTestSms.setOnClickListener {
            // Adapte os dados de teste conforme necessário
            val url = prefs.getEndpointUrl()
            val login = prefs.getEndpointLogin()
            val password = prefs.getEndpointPassword()
            val sender = "5551234" // Número de teste
            val messageBody = "TESTE_SMS_${System.currentTimeMillis() % 10000}" // Mensagem de teste
            // CORREÇÃO: Converte o timestamp de milissegundos para segundos
            val timestamp = System.currentTimeMillis() / 1000 // Envia timestamp em segundos

            // Lança uma coroutine para enviar o SMS e atualizar o status
            lifecycleScope.launch {
                tvSentStatus.text = "Enviando SMS de teste..." // Feedback imediato
                val success = SmsSender.sendSmsToEndpoint(url, login, password, sender, messageBody, timestamp)

                val logEntry = if (success) {
                    // Para o log, ainda usamos o timestamp em milissegundos para formatar a hora local
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis()))
                    "[$time] Enviado: '$messageBody' (SUCESSO)"
                } else {
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis()))
                    "[$time] Falha ao enviar: '$messageBody' (ERRO)"
                }
                prefs.addSentSmsLog(logEntry) // Salva no histórico de envios

                runOnUiThread {
                    tvSentStatus.text = if (success) "Último envio: SUCESSO" else "Último envio: FALHA"
                    showSentHistory() // Atualiza a lista de histórico de envios
                }
            }
        }

        checkAndRequestSmsPermissions()
        checkAndRequestNotificationPermission()
        requestDefaultSmsApp()
        updateSmsInfoMessage()

        // NOVO: Registrar o BroadcastReceiver local para atualizações do histórico de envios
        val intentFilterSentHistory = IntentFilter("com.example.smsforwarder.UPDATE_SENT_HISTORY")
        // Usar ContextCompat.registerReceiver para compatibilidade com Android 14 (API 34)
        // e RECEIVER_NOT_EXPORTED para segurança, pois é um receiver interno
        ContextCompat.registerReceiver(this, updateSentHistoryReceiver, intentFilterSentHistory, ContextCompat.RECEIVER_NOT_EXPORTED)
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

    override fun onDestroy() {
        super.onDestroy()
        // NOVO: Desregistrar o BroadcastReceiver
        unregisterReceiver(updateSentHistoryReceiver)
    }

    // Renomeado para clareza
    private fun showReceivedHistory() {
        val historyBox = findViewById<LinearLayout>(R.id.historyBox)
        historyBox.removeAllViews()
        val historyList = prefs.getSmsHistory() // Assumindo que este é o histórico de recebidos
        if (historyList.isEmpty()) {
            historyBox.addView(TextView(this).apply { text = "Nenhum código recebido ainda." })
        } else {
            for (item in historyList) {
                historyBox.addView(TextView(this).apply {
                    text = item
                    textSize = 16f
                    setPadding(6, 4, 6, 4)
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
                })
            }
        }
    }

    /**
     * Exibe o histórico de SMS enviados na UI.
     */
    private fun showSentHistory() {
        sentHistoryBox.removeAllViews()
        val sentHistoryList = prefs.getSentSmsHistory() // Novo método no PreferencesHelper
        if (sentHistoryList.isEmpty()) {
            sentHistoryBox.addView(TextView(this).apply { text = "Nenhum SMS enviado ainda." })
        } else {
            for (item in sentHistoryList) {
                sentHistoryBox.addView(TextView(this).apply {
                    text = item
                    textSize = 16f
                    setPadding(6, 4, 6, 4)
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
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
            requestSmsPermissionsLauncher.launch(notGranted.toTypedArray()) // Usando o launcher correto
        } else {
            tvStatus.text = "Permissões de SMS concedidas."
        }
    }

    // Refatorado para usar ActivityResultLauncher
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Refatorado para usar ActivityResultLauncher em vez de startActivityForResult
    private fun requestDefaultSmsApp() {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // RoleManager.ROLE_SMS requires API 29+
            if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                requestRoleLauncher.launch(intent) // Usando o launcher
            }
        } else {
            // Para APIs anteriores ao 29, a mudança de app padrão é feita via Intent
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            requestRoleLauncher.launch(intent) // Usando o launcher
        }
    }

    // O método onActivityResult sobrescrito não é mais necessário para a requisição de RoleManager
    // se você estiver usando o ActivityResultLauncher.
    // Se houver outras chamadas startActivityForResult no seu código que ainda o utilizam,
    // você precisará mantê-lo ou refatorá-las também.
    // @Deprecated("Deprecated in Java", ReplaceWith("requestRoleLauncher.launch(intent)", "androidx.activity.result.contract.ActivityResultContracts"))
    // override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    //     super.onActivityResult(requestCode, resultCode, data)
    //     if (requestCode == 1001) { // Código para a requisição de app de SMS padrão
    //         updateSmsInfoMessage() // Atualiza a mensagem de status após o usuário interagir
    //     }
    // }
}
