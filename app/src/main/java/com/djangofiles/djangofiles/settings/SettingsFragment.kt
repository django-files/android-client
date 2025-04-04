package com.djangofiles.djangofiles.settings

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
//import androidx.preference.PreferenceManager
import com.djangofiles.djangofiles.MainActivity.Companion.PREFS_NAME
import com.djangofiles.djangofiles.MainActivity.Companion.URL_KEY
import com.djangofiles.djangofiles.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d("SettingsFragment", "onCreatePreferences rootKey: $rootKey")

        setPreferencesFromResource(R.xml.preferences, rootKey)

        val savedUrlPref = findPreference<EditTextPreference>("saved_url")
        Log.d("SettingsFragment", "savedUrlPref: $savedUrlPref")

        val preferences = context?.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        Log.d("SettingsFragment", "preferences: $preferences")

        savedUrlPref?.let {
            //val savedUrl = PreferenceManager.getDefaultSharedPreferences(requireContext())
            //    .getString("saved_url", "")
            val savedUrl = preferences?.getString(URL_KEY, "")
            Log.d("SettingsFragment", "savedUrl: $savedUrl")
            it.text = savedUrl
        }

        savedUrlPref?.setOnPreferenceChangeListener { _, newValue ->
            val newUrl = newValue as String
            Log.d("SettingsFragment", "newUrl: $newUrl")

            //PreferenceManager.getDefaultSharedPreferences(requireContext())
            //    .edit { putString("saved_url", newUrl) }
            preferences?.edit { putString(URL_KEY, newUrl) }
            true
        }
    }
}
