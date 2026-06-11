package com.lzylym.zymview.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TRIGGER_REMINDER) return

        val title = intent.getStringExtra(ReminderService.KEY_TITLE) ?: "提醒"
        val content = intent.getStringExtra(ReminderService.KEY_CONTENT) ?: "事件提醒"
        val notificationId = intent.getIntExtra(ReminderService.KEY_NOTIFICATION_ID, 0)
        val rawSoundRes = intent.getIntExtra(ReminderService.KEY_SOUND_RES, 0)

        try {
            ContextCompat.startForegroundService(
                context,
                ReminderService.createStartIntent(
                    context,
                    title,
                    content,
                    notificationId,
                    rawSoundRes
                )
            )
        } catch (_: Throwable) {
            context.startActivity(
                ReminderFullScreenActivity.createIntent(context, title, content, notificationId).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
    }

    companion object {
        private const val ACTION_TRIGGER_REMINDER = "com.lzylym.zymview.reminder.ACTION_TRIGGER_REMINDER"

        fun setReminder(
            context: Context,
            triggerAtMillis: Long,
            title: String,
            content: String,
            notificationId: Int,
            rawSoundRes: Int = 0
        ) {
            val alarmIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_TRIGGER_REMINDER
                putExtra(ReminderService.KEY_TITLE, title)
                putExtra(ReminderService.KEY_CONTENT, content)
                putExtra(ReminderService.KEY_NOTIFICATION_ID, notificationId)
                putExtra(ReminderService.KEY_SOUND_RES, rawSoundRes)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (_: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        }

        fun cancelReminder(context: Context, notificationId: Int) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_TRIGGER_REMINDER
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            ReminderService.stop(context, notificationId)
        }
    }
}
