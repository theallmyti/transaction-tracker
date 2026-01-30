package com.example.finance.data.parser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.finance.data.db.AppDatabase
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val repository = FinanceRepository(AppDatabase.getDatabase(context).transactionDao())
            val notificationHelper = NotificationHelper(context)

            messages.forEach { sms ->
                val sender = sms.displayOriginatingAddress ?: return@forEach
                val body = sms.displayMessageBody ?: return@forEach
                val date = sms.timestampMillis

                val transaction = SmsParser.parseSms(sender, body, date)

                if (transaction != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repository.insert(transaction)
                            notificationHelper.showTransactionNotification(transaction)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
