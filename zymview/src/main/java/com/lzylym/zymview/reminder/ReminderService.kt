package com.lzylym.zymview.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class ReminderService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null
    private var stopHandler: Handler? = null
    private var stopRunnable: Runnable? = null
    private var activeNotificationId: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        if (intent.action == ACTION_STOP_REMINDER) {
            stopReminder(intent.getIntExtra(KEY_NOTIFICATION_ID, activeNotificationId))
            return START_NOT_STICKY
        }

        val title = intent.getStringExtra(KEY_TITLE) ?: "提醒"
        val content = intent.getStringExtra(KEY_CONTENT) ?: "事件提醒"
        val notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, System.currentTimeMillis().toInt())
        val rawSoundRes = intent.getIntExtra(KEY_SOUND_RES, 0)
        activeNotificationId = notificationId

        val soundUri: Uri = if (rawSoundRes != 0) {
            Uri.parse("android.resource://$packageName/$rawSoundRes")
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        val channelId = if (rawSoundRes != 0) "reminder_channel_$rawSoundRes" else "reminder_channel_default"
        createChannelIfNeeded(channelId, soundUri)

        val fullIntent = ReminderFullScreenActivity.createIntent(this, title, content, notificationId)
        val contentPi = PendingIntent.getActivity(
            this,
            notificationId,
            fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPi)
            .setFullScreenIntent(contentPi, true)
            .build()
        startForeground(notificationId, notification)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reminder:wake").apply {
            acquire(30_000)
        }

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)

        ringtone = RingtoneManager.getRingtone(this, soundUri)
        ringtone?.play()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 200, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }

        stopHandler?.removeCallbacksAndMessages(null)
        stopHandler = Handler(Looper.getMainLooper())
        stopRunnable = Runnable { stopReminder(notificationId, audio) }
        stopHandler?.postDelayed(stopRunnable!!, 30_000)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopReminder(activeNotificationId)
        super.onDestroy()
    }

    private fun stopReminder(notificationId: Int, audio: AudioManager? = null) {
        try { ringtone?.stop() } catch (_: Throwable) {}
        ringtone = null
        try { vibrator?.cancel() } catch (_: Throwable) {}
        vibrator = null
        try { audio?.abandonAudioFocus(null) } catch (_: Throwable) {}
        try {
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocus(null)
        } catch (_: Throwable) {}
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {}
        wakeLock = null
        stopHandler?.removeCallbacksAndMessages(null)
        stopHandler = null
        stopRunnable = null
        if (notificationId != 0) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        }
        stopForeground(true)
        stopSelf()
    }

    private fun createChannelIfNeeded(id: String, soundUri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val ch = NotificationChannel(id, "提醒通知", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(soundUri, audioAttr)
        }
        nm.createNotificationChannel(ch)
    }

    companion object {
        private const val ACTION_START_REMINDER = "com.lzylym.zymview.reminder.ACTION_START_REMINDER"
        private const val ACTION_STOP_REMINDER = "com.lzylym.zymview.reminder.ACTION_STOP_REMINDER"

        const val KEY_TITLE = "title"
        const val KEY_CONTENT = "content"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_SOUND_RES = "sound_res"

        fun createStartIntent(
            context: Context,
            title: String,
            content: String,
            notificationId: Int,
            rawSoundRes: Int
        ): Intent {
            return Intent(context, ReminderService::class.java).apply {
                action = ACTION_START_REMINDER
                putExtra(KEY_TITLE, title)
                putExtra(KEY_CONTENT, content)
                putExtra(KEY_NOTIFICATION_ID, notificationId)
                putExtra(KEY_SOUND_RES, rawSoundRes)
            }
        }

        fun stop(context: Context, notificationId: Int) {
            try {
                context.startService(
                    Intent(context, ReminderService::class.java).apply {
                        action = ACTION_STOP_REMINDER
                        putExtra(KEY_NOTIFICATION_ID, notificationId)
                    }
                )
            } catch (_: Throwable) {
            }
        }
    }
}
