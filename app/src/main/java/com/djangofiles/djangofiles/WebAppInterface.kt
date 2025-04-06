package com.djangofiles.djangofiles

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.edit
import com.djangofiles.djangofiles.settings.SettingsFragment.ServerEntry
import org.json.JSONArray
import org.json.JSONObject


class WebAppInterface
internal constructor(private var context: Context) {
    companion object {
        private const val PREFS_NAME = "AppPreferences"
        private const val TOKEN_KEY = "auth_token"
        private const val URL_KEY = "saved_url"
    }

    @JavascriptInterface
    fun showToast(toast: String?) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun receiveAuthToken(authToken: String) {
        Log.d("receiveAuthToken", "Received auth token: $authToken")

        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentToken = preferences.getString(TOKEN_KEY, null) ?: ""
        Log.d("receiveAuthToken", "currentToken: $currentToken")
        val currentUrl = preferences.getString(URL_KEY, null) ?: ""
        Log.d("receiveAuthToken", "currentUrl: $currentUrl")

        if (currentToken != authToken) {
            preferences.edit { putString(TOKEN_KEY, authToken) }

            val servers = loadServers().toMutableList()
            val entry = servers.find { it.url == currentUrl }
            Log.d("receiveAuthToken", "entry: $entry")

            val index = servers.indexOfFirst { it.url == currentUrl }
            if (index != -1) {
                servers[index] = servers[index].copy(token = currentToken)
            } else {
                servers.add(ServerEntry(url = currentUrl, token = currentToken))
            }
            saveServers(servers)

            Log.d("receiveAuthToken", "Auth Token Updated.")
            val cookieManager = CookieManager.getInstance()
            cookieManager.flush()
            Log.d("receiveAuthToken", "Cookies Flushed (saved to disk).")
        } else {
            Log.d("receiveAuthToken", "Auth Token Not Changes.")
        }
    }

    // TODO: Duplication - SettingsFragment
    private fun loadServers(): List<ServerEntry> {
        val preferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val json = preferences?.getString("servers", "[]") ?: "[]"
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

    // TODO: Duplication - SettingsFragment
    private fun saveServers(list: List<ServerEntry>) {
        val preferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val array = JSONArray().apply {
            list.forEach {
                put(JSONObject().apply {
                    put("url", it.url)
                    put("token", it.token)
                })
            }
        }
        preferences?.edit() { putString("servers", array.toString()) }
    }
}
