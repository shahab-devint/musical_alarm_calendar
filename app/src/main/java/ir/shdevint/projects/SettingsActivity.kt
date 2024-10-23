package ir.shdevint.projects

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setTitle(R.string.settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment()) // Add this line
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)

            // Register preference change listener
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        }

        override fun onResume() {
            super.onResume()
            updatePreferenceSummaries()
        }

        override fun onDestroy() {
            super.onDestroy()
            // Unregister preference change listener
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }

        private val preferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "app_language") {
                    updateInterfaceLanguage()
                } else if (key == "favorite_genres") {
                    updatePreferenceSummaries()
                }
            }

        private fun updatePreferenceSummaries() {
            val appLanguagePreference = findPreference<ListPreference>("app_language")
            appLanguagePreference?.summary = appLanguagePreference?.entry

            val favoriteGenresPreference = findPreference<MultiSelectListPreference>("favorite_genres")
            val selectedGenres = favoriteGenresPreference?.values?.joinToString(", ")
            favoriteGenresPreference?.summary = selectedGenres
        }

        private fun updateInterfaceLanguage() {
            // Update the configuration and recreate the activity here
            // Access requireContext() here, as it's now valid
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val language = sharedPrefs.getString("app_language", "en")

            val locale = Locale(language!!)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // Refresh the current activity
            requireActivity().recreate()
        }
    }
}