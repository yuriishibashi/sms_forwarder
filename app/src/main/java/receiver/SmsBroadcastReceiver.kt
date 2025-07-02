package com.example.smsforwarder.receiver

import android.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import com.seudominio.smsforwarder.network.SmsSender
import com.seudominio.smsforwarder.util.PreferencesHelper

// Para notificação
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                val pdus = bundle["pdus"] as Array<*>
                for (pdu in pdus) {
                    val format = bundle.getString("format")
                    val message = SmsMessage.createFromPdu(pdu as ByteArray, format)
                    val sender = message.displayOriginatingAddress
                    val body = message.displayMessageBody
                    val timestamp = message.timestampMillis

                    val prefs = PreferencesHelper(context)
                    val url = prefs.getEndpointUrl()
                    val login = prefs.getEndpointLogin()
                    val password = prefs.getEndpointPassword()
                    if (url.isNotEmpty()) {
                        Thread {
                            SmsSender.sendSmsToEndpoint(
                                url,
                                login,
                                password,
                                sender,
                                body,
                                timestamp
                            )
                        }.start()
                    }

                    // CHAMA AQUI A EXIBIÇÃO DA NOTIFICAÇÃO!
                    notifySmsReceived(context, sender, body)
                }
            }
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
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.sym_action_email)
            .setContentTitle("Novo SMS recebido")
            .setContentText("De: $sender - ${message.take(40)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("De: $sender\n$message"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(notificationId, notification)
    }
}
