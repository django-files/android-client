package com.djangofiles.djangofiles.settings

import android.os.Bundle
import android.util.Log
//import android.util.Patterns
import android.widget.Toast
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

        var savedUrl = preferences?.getString(URL_KEY, "")

        savedUrlPref?.let {
            //val savedUrl = PreferenceManager.getDefaultSharedPreferences(requireContext())
            //    .getString("saved_url", "")
            Log.d("SettingsFragment", "savedUrl: $savedUrl")
            it.text = savedUrl
        }

        savedUrlPref?.setOnPreferenceChangeListener { _, newValue ->
            val newUrl = newValue as String
            Log.d("SettingsFragment", "newUrl: $newUrl")
            val url = parseUrl(newUrl)
            Log.d("SettingsFragment", "url: $url")
            if (url.isNullOrEmpty()) {
                Log.d("SettingsFragment", "ERROR CHANGING URL!!")
                Toast.makeText(context, "Invalid URL!", Toast.LENGTH_SHORT).show()
                false
            } else {
                if (url == savedUrl) {
                    Toast.makeText(context, "URL Not Changed!", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    preferences?.edit { putString(URL_KEY, url) }
                    savedUrl = url
                    Log.d("SettingsFragment", "URL CHANGED")
                    true
                }
            }

            //PreferenceManager.getDefaultSharedPreferences(requireContext())
            //    .edit { putString("saved_url", newUrl) }
        }
    }

    private fun parseUrl(urlString: String): String? {
        var url = urlString.trim { it <= ' ' }
        if (url.isEmpty()) {
            Log.d("parseUrl", "url.isEmpty()")
            return null
        }
        if (!url.lowercase().startsWith("http")) {
            url = "https://$url"
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        Log.d("parseUrl", "matching: $url")
        //if (!Patterns.WEB_URL.matcher(url).matches()) {
        //    Log.d("parseUrl", "Patterns.WEB_URL.matcher Failed")
        //    return null
        //}
        return url
    }
}
