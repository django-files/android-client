package com.djangofiles.djangofiles.settings

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

//import android.util.Patterns
//import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    private val client = OkHttpClient()

    private val serverKey = "servers"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "AppPreferences"
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
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", null)
//                .setPositiveButton("Add") { _, _ ->
//                    val url = editText.text.toString().trim()
//                    if (url.isNotEmpty()) {
//                        val servers = loadServers().toMutableList()
//                        if (servers.none { it.url == url }) {
//                            servers.add(ServerEntry(url, ""))
//                            saveServers(servers)
//                            buildServerList()
//                        }
//                    }
//                }
                .show().apply {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        var url = editText.text.toString().trim()

//                        if (url.isNotEmpty()) {
//                            val servers = loadServers().toMutableList()
//                            if (servers.none { it.url == url }) {
//                                servers.add(ServerEntry(url, ""))
//                                saveServers(servers)
//                                buildServerList()
//                            }
//                        }

                        Log.d("showSettingsDialog", "setPositiveButton: url: $url")
                        if (url.isEmpty()) {
                            Log.d("showSettingsDialog", "URL is Empty")
                            editText.error = "This field is required."
                        } else {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://$url"
                            }
                            if (url.endsWith("/")) {
                                url = url.substring(0, url.length - 1)
                            }

                            Log.d("showSettingsDialog", "Processed URL: $url")
                            Log.d("showSettingsDialog", "Saving New URL...")

                            val servers = loadServers().toMutableList()
                            if (servers.any { it.url == url }) {
                                editText.error = "Server Exists"
                            } else {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val authUrl = "${url}/api/auth/methods/"
                                    Log.d("showSettingsDialog", "Auth URL: $authUrl")
                                    val response = checkUrl(authUrl)
                                    Log.d("showSettingsDialog", "response: $response")
                                    withContext(Dispatchers.Main) {
                                        if (response) {
                                            Log.d("showSettingsDialog", "SUCCESS")

                                            //val servers = loadServers().toMutableList()
//                                servers[index] = servers[index].copy(url = url)
//                                saveServers(servers)
//                                buildServerList()
//                                dialog?.dismiss()
                                            servers.add(ServerEntry(url, ""))
                                            saveServers(servers)
                                            buildServerList()
                                            cancel()
                                        } else {
                                            Log.d("showSettingsDialog", "FAILURE")
                                            editText.error = "Invalid URL"
                                        }
                                    }
                                }
                                //preferences.edit { putString(URL_KEY, url) }
                                //webView.loadUrl(url)
                                //dismiss()
                            }
                        }
                    }
                }

            true
        }
    }

    private fun checkUrl(url: String): Boolean {
        Log.d("checkUrl", "url: $url")
        // TODO: Change this to HEAD or use response data...
        val request = Request.Builder().header("User-Agent", "DF").url(url).get().build()
        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun buildServerList() {
        val category = findPreference<PreferenceCategory>("server_list") ?: return
        category.removeAll()

        val servers = loadServers()

        val savedUrl = preferenceManager.sharedPreferences?.getString("saved_url", "")
        Log.d("buildServerList", "savedUrl: $savedUrl")

        servers.forEachIndexed { index, entry ->
            val pref = ServerPreference(
                requireContext(),
                index,
                entry,
                onEdit = { i, e -> showEditDialog(i, e, savedUrl) },
                onDelete = { i -> showDeleteDialog(i) },
                savedUrl = savedUrl
            )
            category.addPreference(pref)
        }
    }


    private fun showEditDialog(index: Int, entry: ServerEntry, savedUrl: String?) {
        //val editText = EditText(requireContext()).apply {
        //    inputType = InputType.TYPE_TEXT_VARIATION_URI
        //    setText(entry.url)
        //}

        Log.d("showEditDialog", "entry.url: ${entry.url}")
        Log.d("showEditDialog", "entry.token: ${entry.token}")

        if (entry.url == savedUrl) {
            Log.d("showEditDialog", "ENTRY ALREADY ACTIVE - RETURN")
            return
        }

        val sharedPreferences = preferenceManager.sharedPreferences
        sharedPreferences?.edit()?.apply {
            putString("saved_url", entry.url)
            putString("auth_token", entry.token)
            apply()
        }
        buildServerList()

        //val servers = loadServers().toMutableList()
        //val token = servers.find { it.url == entry.url }?.token ?: ""
        //Log.d("showEditDialog", "token: $token")


        //AlertDialog.Builder(requireContext())
        //    .setTitle("Edit Server")
        //    .setView(editText)
        //    .setPositiveButton("Save") { _, _ ->
        //        val newUrl = editText.text.toString().trim()
        //        if (newUrl.isNotEmpty()) {
        //            val servers = loadServers().toMutableList()
        //            servers[index] = servers[index].copy(url = newUrl)
        //            saveServers(servers)
        //            buildServerList()
        //        }
        //    }
        //    .setNegativeButton("Cancel", null)
        //    .show()
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

    data class ServerEntry(val url: String, val token: String)

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
