package ir.shdevint.projects.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {

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
    }
}