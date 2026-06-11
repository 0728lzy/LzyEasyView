package com.lzylym.zymview.reminder

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lzylym.zymview.R

class ReminderFullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        setContentView(R.layout.activity_reminder_fullscreen)

        val title = intent.getStringExtra("title") ?: "提醒"
        val content = intent.getStringExtra("content") ?: "事件提醒"
        val notificationId = intent.getIntExtra("notificationId", 0)

        findViewById<TextView>(R.id.tv_title).text = title
        findViewById<TextView>(R.id.tv_content).text = content

        findViewById<Button>(R.id.btn_close).setOnClickListener {
            closeReminder(notificationId)
        }
    }

    private fun closeReminder(notificationId: Int) {
        ReminderService.stop(this, notificationId)
        ReminderReceiver.cancelReminder(this, notificationId)
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(notificationId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            title: String,
            content: String,
            notificationId: Int
        ): Intent {
            return Intent(context, ReminderFullScreenActivity::class.java).apply {
                putExtra("title", title)
                putExtra("content", content)
                putExtra("notificationId", notificationId)
            }
        }
    }
}
