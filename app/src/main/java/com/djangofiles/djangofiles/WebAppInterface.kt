package com.djangofiles.djangofiles

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface
internal constructor(private var context: Context) {
    companion object {
        private const val PREFS_NAME = "AppPreferences"
        private const val TOKEN_KEY = "auth_token"
    }

    @JavascriptInterface
    fun showToast(toast: String?) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun receiveAuthToken(authToken: String) {
        Log.d("receiveAuthToken", "Received auth token: $authToken")

        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentToken = preferences.getString(TOKEN_KEY, null)

        if (currentToken != authToken) {
            preferences.edit().putString(TOKEN_KEY, authToken).apply()
            Log.d("receiveAuthToken", "Auth Token Updated.")
        } else {
            Log.d("receiveAuthToken", "Auth Token Not Changes.")
        }
    }

}
