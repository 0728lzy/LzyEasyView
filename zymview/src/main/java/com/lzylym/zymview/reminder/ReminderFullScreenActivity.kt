package com.lzylym.zymview.reminder

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lzylym.zymview.R

class ReminderFullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_fullscreen)

        val title = intent.getStringExtra("title") ?: "提醒"
        val content = intent.getStringExtra("content") ?: "事件提醒"
        val notificationId=intent.getIntExtra("notificationId",0)

        findViewById<TextView>(R.id.tv_title).text = title
        findViewById<TextView>(R.id.tv_content).text = content

        findViewById<Button>(R.id.btn_close).setOnClickListener {
            ReminderReceiver.cancelReminder(this@ReminderFullScreenActivity,notificationId)
            finish()
        }
    }
}
