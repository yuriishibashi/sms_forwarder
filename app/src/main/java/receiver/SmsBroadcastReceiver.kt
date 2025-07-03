package com.example.smsforwarder.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import com.seudominio.smsforwarder.network.SmsSender
import com.seudominio.smsforwarder.util.PreferencesHelper
import java.text.SimpleDateFormat
import java.util.*

class SmsBroadcastReceiver : BroadcastReceiver() {
    private val patternList = listOf(
        SmsPattern(
            label = "Cód. Vivo",
            regex = Regex("""phone access code is: ([A-Za-z0-9]+)""")
        ),
        SmsPattern(
            label = "Cód. Test.",
            regex = Regex("""transaction code: ([A-Za-z0-9\-]+)""")
        )
        // Adicione quantos quiser nesta lista
    )

    data class SmsPattern(val label: String, val regex: Regex)

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
            val message = if (format != null)
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            else
                SmsMessage.createFromPdu(pdu as ByteArray)

            val sender = message.displayOriginatingAddress
            val body = message.displayMessageBody
            val timestamp = message.timestampMillis

            var matchedLabel: String? = null
            var matchedValue: String? = null

            for (pattern in patternList) {
                val match = pattern.regex.find(body)
                if (match != null) {
                    matchedLabel = pattern.label
                    matchedValue = match.groupValues[1]
                    break // Para no primeiro padrão encontrado
                }
            }

            if (matchedLabel != null && matchedValue != null) {
                val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                val dataHora = sdf.format(Date(timestamp))
                val entry = "$dataHora - $matchedLabel: $matchedValue"
                val oldList = prefs.getSmsHistory().toMutableList()
                oldList.add(0, entry)
                prefs.saveSmsHistory(oldList.take(10))
                context.sendBroadcast(Intent("com.example.smsforwarder.UPDATE_HISTORY"))

                if (url.isNotEmpty()) {
                    Thread {
                        SmsSender.sendSmsToEndpoint(
                            url, login, password, sender, body, timestamp
                        )
                    }.start()
                }
            }
//            notifySmsReceived(context, sender, body)
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