package ir.shdevint.projects.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import ir.shdevint.projects.R

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION_STOP_ALARM = "ir.shdevint.projects.ACTION_STOP_ALARM"
        private const val CHANNEL_ID = "ALARM_CHANNEL_ID"
        private const val NOTIFICATION_ID = 1001
    }


    override fun onReceive(context: Context, intent: Intent) {
        val songUrl = intent.getStringExtra("song_url")

        // Play the song using MediaPlayer or any other audio playback method
        // You can use songUrl to access the online audio file
        Toast.makeText(context, "Alarm went off! Playing song from: $songUrl", Toast.LENGTH_LONG).show()

        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setDataSource(songUrl)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener { mp ->
                mp.start()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error playing audio: ${e.message}", Toast.LENGTH_LONG).show()
        }

        createNotificationChannel(context) // Create notification channel

        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
        }

        val stopPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Alarm is Ringing")
            .setContentText("Your song is playing")
            .setSmallIcon(R.drawable.ic_alarm) // Replace with your icon
            .addAction(R.drawable.stop_circle, "Stop", stopPendingIntent) // Stop action
            .build()

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        when (intent.action) {
            ACTION_STOP_ALARM -> {
                // Stop playback here
                mediaPlayer.stop()
                mediaPlayer.release() // Release resources

                // Also, remove the notification
                val notifManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifManager.cancel(NOTIFICATION_ID)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}