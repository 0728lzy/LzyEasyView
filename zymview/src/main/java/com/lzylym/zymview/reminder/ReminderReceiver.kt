package com.lzylym.zymview.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        fun setReminder(
            context: Context,
            triggerAtMillis: Long,
            title: String,
            content: String,
            notificationId: Int,
            rawSoundRes: Int = 0
        ) {
            val serviceIntent = Intent(context, ReminderService::class.java).apply {
                putExtra(ReminderService.KEY_TITLE, title)
                putExtra(ReminderService.KEY_CONTENT, content)
                putExtra(ReminderService.KEY_NOTIFICATION_ID, notificationId)
                putExtra(ReminderService.KEY_SOUND_RES, rawSoundRes)
            }

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    notificationId,
                    serviceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    notificationId,
                    serviceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val showIntent = Intent(context,ReminderFullScreenActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                triggerAtMillis,
                showPendingIntent
            )

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            am.setAlarmClock(alarmClockInfo, pendingIntent)

        }
    }

    fun cancelReminder(context: Context, notificationId: Int) {
        val serviceIntent = Intent(context, ReminderService::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                notificationId,
                serviceIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                notificationId,
                serviceIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (pendingIntent == null) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override fun onReceive(context: Context, intent: Intent?) {

    }
}