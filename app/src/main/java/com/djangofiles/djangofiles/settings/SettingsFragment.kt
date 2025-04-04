package com.djangofiles.djangofiles.settings

import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.djangofiles.djangofiles.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d("SettingsFragment", "onCreatePreferences rootKey: $rootKey")

        setPreferencesFromResource(R.xml.preferences, rootKey)

        val savedUrlPref = findPreference<EditTextPreference>("saved_url")
        Log.d("SettingsFragment", "savedUrlPref: $savedUrlPref")

        savedUrlPref?.let {
            val savedUrl = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("saved_url", "")

            // Log the saved URL
            Log.d("SettingsFragment", "Loaded saved URL: $savedUrl")

            it.text = savedUrl
        }

        savedUrlPref?.setOnPreferenceChangeListener { _, newValue ->
            val newUrl = newValue as String
            // Log the updated URL
            Log.d("SettingsFragment", "Updated saved URL: $newUrl")

            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit { putString("saved_url", newUrl) }
            true
        }
    }
}
