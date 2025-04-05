package com.djangofiles.djangofiles.settings

import android.util.Log
//import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder
//import androidx.preference.PreferenceManager
import com.djangofiles.djangofiles.MainActivity.Companion.PREFS_NAME
import com.djangofiles.djangofiles.MainActivity.Companion.URL_KEY
import com.djangofiles.djangofiles.R

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.json.JSONArray
import org.json.JSONObject


class SettingsFragment : PreferenceFragmentCompat() {

    private val serverKey = "servers"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "app_preferences"
        setPreferencesFromResource(R.xml.pref_root, rootKey)
        buildServerList()
        setupAddServer()
    }

    private fun setupAddServer() {
        findPreference<Preference>("add_server")?.setOnPreferenceClickListener {
            val editText = EditText(requireContext()).apply {
                inputType = InputType.TYPE_TEXT_VARIATION_URI
                hint = "https://example.com"
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Add Server")
                .setView(editText)
                .setPositiveButton("Add") { _, _ ->
                    val url = editText.text.toString().trim()
                    if (url.isNotEmpty()) {
                        val servers = loadServers().toMutableList()
                        if (servers.none { it.url == url }) {
                            servers.add(ServerEntry(url, ""))
                            saveServers(servers)
                            buildServerList()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()

            true
        }
    }

    private fun buildServerList() {
        val category = findPreference<PreferenceCategory>("server_list") ?: return
        Log.d("buildServerList", "category: $category")
        category.removeAll()

        val servers = loadServers()
        Log.d("buildServerList", "servers: $servers")

        servers.forEachIndexed { index, entry ->
            Log.d("buildServerList", "index: $index - entry: $entry")
            val pref = Preference(requireContext()).apply {
                title = entry.url
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.fa_pen_to_square)
                setOnPreferenceClickListener {
                    showEditDialog(index, entry)
                    //showDeleteDialog(index)
                    true
                }
                //setOnPreferenceLongClickListener {
                //    showDeleteDialog(index)
                //    true
                //}
            }
            category.addPreference(pref)
        }
    }

    //private fun setOnPreferenceLongClickListener(listener: (Preference) -> Boolean) {
    //    listener(preference)
    //}

    private fun showEditDialog(index: Int, entry: ServerEntry) {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setText(entry.url)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Server")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = editText.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    val servers = loadServers().toMutableList()
                    servers[index] = servers[index].copy(url = newUrl)
                    saveServers(servers)
                    buildServerList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(index: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Server?")
            .setMessage("Are you sure you want to delete this server?")
            .setPositiveButton("Delete") { _, _ ->
                val servers = loadServers().toMutableList()
                servers.removeAt(index)
                saveServers(servers)
                buildServerList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadServers(): List<ServerEntry> {
        val json = preferenceManager.sharedPreferences?.getString(serverKey, "[]") ?: "[]"
        return try {
            JSONArray(json).let { array ->
                List(array.length()) {
                    val obj = array.getJSONObject(it)
                    ServerEntry(
                        url = obj.getString("url"),
                        token = obj.optString("token", "")
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveServers(list: List<ServerEntry>) {
        val array = JSONArray().apply {
            list.forEach {
                put(JSONObject().apply {
                    put("url", it.url)
                    put("token", it.token)
                })
            }
        }
        preferenceManager.sharedPreferences?.edit() { putString(serverKey, array.toString()) }
    }

    private data class ServerEntry(val url: String, val token: String)

//    private fun Preference.setOnPreferenceLongClickListener(listener: (Preference) -> Boolean) {
//        this.viewLifecycleOwnerLiveData.observe(viewLifecycleOwner) { viewLifecycleOwner ->
//            if (viewLifecycleOwner != null) {
//                this.setOnPreferenceClickListener(null)
//                this.setOnPreferenceClickListener {
//                    false
//                }
//                this.preferenceView?.setOnLongClickListener {
//                    listener(this)
//                }
//            }
//        }
//    }

//    private fun setOnPreferenceLongClickListener(preference: Preference, listener: (Preference) -> Boolean) {
//        preference.setOnLongClickListener {
//            listener(preference)
//        }
//    }

//    private val Preference.preferenceView: View?
//        get() = (listView?.findViewHolderForAdapterPosition(preferenceScreen.indexOfPreference(this)) as? PreferenceViewHolder)?.itemView

}



//class SettingsFragment : PreferenceFragmentCompat() {
//    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//        Log.d("SettingsFragment", "onCreatePreferences rootKey: $rootKey")
//
//        setPreferencesFromResource(R.xml.preferences, rootKey)
//
//        val savedUrlPref = findPreference<EditTextPreference>("saved_url")
//        Log.d("SettingsFragment", "savedUrlPref: $savedUrlPref")
//
//        val preferences = context?.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
//        Log.d("SettingsFragment", "preferences: $preferences")
//
//        var savedUrl = preferences?.getString(URL_KEY, "")
//
//        savedUrlPref?.let {
//            //val savedUrl = PreferenceManager.getDefaultSharedPreferences(requireContext())
//            //    .getString("saved_url", "")
//            Log.d("SettingsFragment", "savedUrl: $savedUrl")
//            it.text = savedUrl
//        }
//
//        savedUrlPref?.setOnPreferenceChangeListener { _, newValue ->
//            val newUrl = newValue as String
//            Log.d("SettingsFragment", "newUrl: $newUrl")
//            val url = parseUrl(newUrl)
//            Log.d("SettingsFragment", "url: $url")
//            if (url.isNullOrEmpty()) {
//                Log.d("SettingsFragment", "ERROR CHANGING URL!!")
//                Toast.makeText(context, "Invalid URL!", Toast.LENGTH_SHORT).show()
//                false
//            } else {
//                if (url == savedUrl) {
//                    Toast.makeText(context, "URL Not Changed!", Toast.LENGTH_SHORT).show()
//                    false
//                } else {
//                    preferences?.edit { putString(URL_KEY, url) }
//                    savedUrl = url
//                    Log.d("SettingsFragment", "URL CHANGED")
//                    true
//                }
//            }
//
//            //PreferenceManager.getDefaultSharedPreferences(requireContext())
//            //    .edit { putString("saved_url", newUrl) }
//
//        }
//    }
//
//    private fun parseUrl(urlString: String): String? {
//        var url = urlString.trim { it <= ' ' }
//        if (url.isEmpty()) {
//            Log.d("parseUrl", "url.isEmpty()")
//            return null
//        }
//        if (!url.lowercase().startsWith("http")) {
//            url = "https://$url"
//        }
//        if (url.endsWith("/")) {
//            url = url.substring(0, url.length - 1)
//        }
//        Log.d("parseUrl", "matching: $url")
//        //if (!Patterns.WEB_URL.matcher(url).matches()) {
//        //    Log.d("parseUrl", "Patterns.WEB_URL.matcher Failed")
//        //    return null
//        //}
//        return url
//    }
//}
