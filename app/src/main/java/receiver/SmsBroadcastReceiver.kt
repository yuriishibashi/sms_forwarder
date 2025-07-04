package com.example.smsforwarder.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.seudominio.smsforwarder.network.SmsSender
import com.seudominio.smsforwarder.util.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SmsBroadcastReceiver : BroadcastReceiver() {
    // A patternList e SmsPattern não são mais necessárias para o envio,
    // mas podem ser mantidas se ainda forem usadas para outras lógicas (ex: notificações específicas).
    // Se não forem usadas para mais nada, podem ser removidas.
    // Para este pedido, vamos removê-las para simplificar.
    /*
    private val patternList = listOf(
        SmsPattern(
            label = "Cód. Vivo",
            regex = Regex("""phone access code is: ([A-Za-z0-9]+)""")
        ),
        SmsPattern(
            label = "Cód. Test.",
            regex = Regex("""transaction code: ([A-Za-z0-9\-]+)""")
        )
    )
    data class SmsPattern(val label: String, val regex: Regex)
    */

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return
        val bundle = intent.extras ?: return
        val pdus = bundle["pdus"] as? Array<*> ?: return
        val prefs = PreferencesHelper(context.applicationContext)
        val url = prefs.getEndpointUrl()
        val login = prefs.getEndpointLogin()
        val password = prefs.getEndpointPassword()

        for (pdu in pdus) {
            val format = bundle.getString("format")
            // Usando o método createFromPdu com formato para evitar o aviso de depreciação
            val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && format != null) {
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } else {
                @Suppress("DEPRECATION") // Suprime o aviso para versões antigas
                SmsMessage.createFromPdu(pdu as ByteArray)
            }

            val sender = message.displayOriginatingAddress
            val body = message.displayMessageBody // Mensagem original completa
            val timestampMillis = message.timestampMillis // Timestamp em milissegundos

            Log.d("SMS_RECEBIDO", "Remetente: $sender | Mensagem: $body | Timestamp (ms): $timestampMillis")

            // Não há mais lógica de regex para extrair matchedLabel/matchedValue
            // O SMS completo será enviado e registrado.

            val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val dataHora = sdf.format(Date(timestampMillis))
            // O entry agora usa o corpo da mensagem completo
            val entry = "$dataHora - De: $sender - Mensagem: \"$body\""
            val currentReceivedHistory = prefs.getSmsHistory().toMutableList()
            currentReceivedHistory.add(0, entry)
            // Limita o histórico de recebidos e salva
            prefs.addSmsHistory(currentReceivedHistory.take(10).toSet().toList())
            context.sendBroadcast(Intent("com.example.smsforwarder.UPDATE_HISTORY"))

            if (url.isNotEmpty()) {
                // Lançando uma coroutine para a operação de rede
                // Dispatchers.IO é adequado para operações de I/O como requisições de rede
                CoroutineScope(Dispatchers.IO).launch {
                    // CORREÇÃO: Converte o timestamp de milissegundos para segundos
                    val timestampSeconds = timestampMillis / 1000

                    val enviado = SmsSender.sendSmsToEndpoint(
                        url, login, password, sender, body, timestampSeconds // Envia o 'body' original
                    )

                    val logEntry = if (enviado) {
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis()))
                        "[$time] Enviado: '$body' (SUCESSO)"
                    } else {
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis()))
                        "[$time] Falha ao enviar: '$body' (ERRO)"
                    }
                    prefs.addSentSmsLog(logEntry) // Salva no histórico de envios
                    // Envia um broadcast para a MainActivity atualizar o histórico de envios
                    context.sendBroadcast(Intent("com.example.smsforwarder.UPDATE_SENT_HISTORY"))

                    if (enviado) {
                        Log.i("SMS_ENVIADO", "✅ Mensagem enviada com sucesso para o endpoint")
                    } else {
                        Log.e("SMS_ENVIADO", "❌ Falha ao enviar mensagem para o endpoint")
                    }
                }
            } else {
                Log.w("SMS_ENVIADO", "⚠️ URL do endpoint está vazia")
                val logEntry = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis())) +
                               " - Falha ao enviar: '$body' (URL VAZIA)"
                prefs.addSentSmsLog(logEntry)
                context.sendBroadcast(Intent("com.example.smsforwarder.UPDATE_SENT_HISTORY"))
            }

            // notifySmsReceived(context, sender, body) // Comentado, descomente se quiser notificações
        }
    }

    private fun notifySmsReceived(context: Context, sender: String, message: String) {
        val channelId = "sms_received_channel"
        val channelName = "SMS Recebido"
        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setContentTitle("Novo SMS recebido")
            .setContentText("De: $sender - ${message.take(40)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("De: $sender\n$message"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(notificationId, notification)
    }
}
