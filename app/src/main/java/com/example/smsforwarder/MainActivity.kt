package com.example.smsforwarder

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.seudominio.smsforwarder.util.PreferencesHelper

class MainActivity : AppCompatActivity() {

    private lateinit var etEndpointUrl: EditText
    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvSmsInfo: TextView
    private lateinit var prefs: PreferencesHelper

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

        etEndpointUrl.setText(prefs.getEndpointUrl())
        etLogin.setText(prefs.getEndpointLogin())
        etPassword.setText(prefs.getEndpointPassword())

        btnSave.setOnClickListener {
            prefs.setEndpointUrl(etEndpointUrl.text.toString())
            prefs.setEndpointLogin(etLogin.text.toString())
            prefs.setEndpointPassword(etPassword.text.toString())
            tvStatus.text = "Configurações salvas com sucesso!"
        }

        // Solicita permissões BÁSICAS de SMS
        checkAndRequestSmsPermissions()
        // Solicita permissão de NOTIFICAÇÃO
        checkAndRequestNotificationPermission()
        // Solicita para ser o app padrão de SMS
        requestDefaultSmsApp()
        updateSmsInfoMessage() // <--- Importante!
    }

    override fun onResume() {
        super.onResume()
        updateSmsInfoMessage()
    }

    // Exibe o alerta fixo para o usuário
    private fun updateSmsInfoMessage() {
        if (isDefaultSmsApp() && hasAllSmsPermissions()) {
            tvSmsInfo.text = "Este app está pronto para capturar automaticamente qualquer SMS recebido e encaminhá-lo ao endpoint configurado."
        } else {
            tvSmsInfo.text = "Para funcionar corretamente, defina este app como aplicativo padrão de SMS e conceda as permissões solicitadas."
        }
    }

    private fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
            packageName == defaultSmsPackage
        } else {
            true // Em Android <4.4 não existe app padrão de SMS
        }
    }

    private fun hasAllSmsPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Permissões SMS:
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
            val granted = result.all { it.value }
            if (granted) {
                tvStatus.text = "Permissões de SMS concedidas."
            } else {
                tvStatus.text = "Permissões de SMS negadas."
            }
        }

    // Permissão de notificação:
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 230)
            }
        }
    }

    // Solicita ser app padrão de SMS:
    private fun requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                startActivityForResult(intent, 1001)
            }
        } else {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
            if (defaultSmsPackage != packageName) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivity(intent)
            }
        }
    }
}
