package com.djangofiles.djangofiles

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.edit
import com.djangofiles.djangofiles.settings.ServerDao
import com.djangofiles.djangofiles.settings.ServerDatabase


class WebAppInterface(private var context: Context) {
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
            val dao: ServerDao = ServerDatabase.getInstance(context).serverDao()
            dao.setToken(currentUrl, authToken)
            Log.d("receiveAuthToken", "dao.setToken: $authToken")

            preferences.edit { putString(TOKEN_KEY, authToken) }
            val cookieManager = CookieManager.getInstance()
            cookieManager.flush()
            Log.d("receiveAuthToken", "Cookies Flushed (saved to disk).")
        } else {
            Log.d("receiveAuthToken", "Auth Token Not Changes.")
        }
    }
}
