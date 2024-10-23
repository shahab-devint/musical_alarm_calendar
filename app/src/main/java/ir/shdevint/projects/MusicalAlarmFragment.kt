package ir.shdevint.projects

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.MultiSelectListPreference
import ir.shdevint.projects.databinding.FragmentMusicalAlarmBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import ir.shdevint.projects.services.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor

class MusicalAlarmFragment : Fragment() {

    private var _binding: FragmentMusicalAlarmBinding? = null
    private val binding get() = _binding!!

    private lateinit var morningTimePicker: TimePicker
    private lateinit var nightTimePicker: TimePicker
    private lateinit var morningAlarmSwitch: SwitchMaterial
    private lateinit var nightAlarmSwitch: SwitchMaterial

    private var songUrls: List<String> = emptyList()
    private var nextSongIndex = 0

    private var morningAlarmPendingIntent: PendingIntent? = null
    private var nightAlarmPendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true) // Retain fragment instance
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentMusicalAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        morningTimePicker = binding.morningTimePicker
        nightTimePicker = binding.nightTimePicker
        morningAlarmSwitch = binding.morningAlarmSwitch
        nightAlarmSwitch = binding.nightAlarmSwitch

        // Fetch song URLs when the fragment is created
        fetchSongUrls()

        morningTimePicker.setOnTimeChangedListener { _, hourOfDay, _ ->
            if (hourOfDay in 0..11) { // Check if the selected time is in the AM range
                morningAlarmSwitch.setChecked(false) // Prevent multiple alarm sets by each change in time
            } else {
                // Show error message or reset the time picker to a valid AM time
                Toast.makeText(context,
                    getString(R.string.morning_time_range_error), Toast.LENGTH_SHORT).show()
                morningTimePicker.hour = 0 // Reset to 00:00 AM
                morningTimePicker.minute = 0
            }
        }

        nightTimePicker.setOnTimeChangedListener { _, hourOfDay, _ ->
            if (hourOfDay in 12..23) { // Check if the selected time is in the PM range
                nightAlarmSwitch.setChecked(false) // Prevent multiple alarm sets by each change in time
            } else {
                // Show error message or reset the time picker to a valid PM time
                Toast.makeText(context,
                    getString(R.string.night_time_range_error), Toast.LENGTH_SHORT).show()
                nightTimePicker.hour = 12 // Reset to 12:00 PM
                nightTimePicker.minute = 0
            }
        }

        morningAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setAlarm(morningTimePicker, songUrls[nextSongIndex])
                nextSongIndex = (nextSongIndex + 1) % songUrls.size
            } else {
                cancelAlarm()
            }
        }

        nightAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setAlarm(nightTimePicker, songUrls[nextSongIndex])
                nextSongIndex = (nextSongIndex + 1) % songUrls.size
            } else {
                cancelAlarm()
            }
        }
    }

    private fun fetchSongUrls() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://docs.google.com/spreadsheets/d/1q8DhGkD3YFKVcYMyrgNfr5U5lgE_NE0cmnM77-nAzws/export?format=csv&gid=0")
                val connection = url.openConnection()
                val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
                var line: String?
                val urls = mutableListOf<String>()
                val genres = mutableSetOf<String>() // To store unique genres

                // Skip the header line
                reader.readLine()

                while (reader.readLine().also { line = it } != null) {
                    val row = line!!.split(",")
                    if (row.size >= 3) { // Ensure there's a song URL
                        urls.add(row[2]) // Add the song URL to the list
                    }
                    if (row.size >= 5) { // Check for genre columns
                        genres.add(row[3]) // English genre
                        genres.add(row[4]) // Persian genre
                    }
                }

                withContext(Dispatchers.Main) {
                    songUrls = urls
                    // Now you have the song URLs in the songUrls list
                    // Alert the user if the list is fetched successfully
                    Toast.makeText(context, "${urls.size} songs added to list.", Toast.LENGTH_LONG).show()

                    // Populate the MultiSelectListPreference entries and values
                    val genreEntries = genres.toTypedArray()
                    val genreValues = genres.toTypedArray()

                    // Update the MultiSelectListPreference
                    val preferenceFragment =
                        requireActivity().supportFragmentManager.findFragmentById(R.id.settings_container)
                    if (preferenceFragment is SettingsActivity.SettingsFragment) {
                        val genrePreference =
                            preferenceFragment.findPreference<MultiSelectListPreference>("favorite_genres")
                        genrePreference?.entries = genreEntries
                        genrePreference?.entryValues = genreValues
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error fetching song URLs: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setAlarm(timePicker: TimePicker, songUrl: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
        calendar.set(Calendar.MINUTE, timePicker.minute)
        calendar.set(Calendar.SECOND, 0)

        // Cancel existing alarm of the same type
        if (timePicker == morningTimePicker) { // Check if it's a morning alarm
            morningAlarmPendingIntent?.cancel()
        } else { // It's a night alarm
            nightAlarmPendingIntent?.cancel()
        }

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("song_url", songUrl)
        }

        val alarmId = floor(Math.random() * 100) // assign a random ID to each alarm
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), alarmId.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        Toast.makeText(context, "Alarm set for ${timeFormat.format(calendar.time)}", Toast.LENGTH_SHORT).show()

        // Store the PendingIntent for future cancellation
        if (timePicker == morningTimePicker) {
            morningAlarmPendingIntent = pendingIntent
        } else {
            nightAlarmPendingIntent = pendingIntent
        }

        // Prefetch the song
        GlobalScope.launch(Dispatchers.IO) {
            if (isNetworkConnected(requireContext())) {
                try {
                    val fileName = getFileNameFromUrl(songUrl)
                    val cachedFile = requireContext().getFileStreamPath(fileName)

                    if (!cachedFile.exists()) {
                        // Download the song and save it to cache
                        val connection = URL(songUrl).openConnection()
                        connection.connect()
                        val inputStream = connection.getInputStream()
                        val outputStream = requireContext().openFileOutput(fileName, Context.MODE_PRIVATE)

                        inputStream.copyTo(outputStream)

                        inputStream.close()
                        outputStream.close()
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context,
                            getString(R.string.song_prefetched), Toast.LENGTH_SHORT).show()
                        // You can also use a notification here if preferred
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error prefetching song: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.connect_request), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun cancelAlarm() {
        morningAlarmPendingIntent?.cancel()
        nightAlarmPendingIntent?.cancel()
        morningAlarmPendingIntent = null
        nightAlarmPendingIntent = null

        Toast.makeText(context, getString(R.string.alarm_canceled), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Helper function to check internet connectivity
    private fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    // Helper function to extract filename from URL
    private fun getFileNameFromUrl(url: String): String {
        val lastSlashIndex = url.lastIndexOf('/')
        return if (lastSlashIndex != -1 && lastSlashIndex < url.length - 1) {
            url.substring(lastSlashIndex + 1)
        } else {
            "audio_file.mp3" // Default filename
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}