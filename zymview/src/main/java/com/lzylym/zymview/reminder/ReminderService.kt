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
import android.os.*
import androidx.core.app.NotificationCompat

class ReminderService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtone: android.media.Ringtone? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val title = intent.getStringExtra(KEY_TITLE) ?: "提醒"
        val content = intent.getStringExtra(KEY_CONTENT) ?: "事件提醒"
        val notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, System.currentTimeMillis().toInt())
        val rawSoundRes = intent.getIntExtra(KEY_SOUND_RES, 0)

        val soundUri: Uri = if (rawSoundRes != 0) {
            Uri.parse("android.resource://${packageName}/$rawSoundRes")
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        val channelId = if (rawSoundRes != 0) "reminder_channel_$rawSoundRes" else "reminder_channel_default"
        createChannelIfNeeded(channelId, soundUri)

        val fullIntent = Intent(this, ReminderFullScreenActivity::class.java).apply {
            putExtra("title", title)
            putExtra("content", content)
        }
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
            .setOngoing(true)
            .setContentIntent(contentPi)
            .build()
        startForeground(notificationId, notification)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reminder:wake")
        wakeLock?.acquire(30_000)

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)

        ringtone = RingtoneManager.getRingtone(this, soundUri)
        ringtone?.play()

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 200, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            stopReminder(audio)
        }, 30_000)

        return START_NOT_STICKY
    }

    private fun stopReminder(audio: AudioManager) {
        try { ringtone?.stop() } catch (_: Throwable) {}
        audio.abandonAudioFocus(null)
        wakeLock?.release()
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
            setSound(soundUri, audioAttr)
        }
        nm.createNotificationChannel(ch)
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_CONTENT = "content"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_SOUND_RES = "sound_res"
    }
}
