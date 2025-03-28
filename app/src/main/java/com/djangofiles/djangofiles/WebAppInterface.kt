package com.djangofiles.djangofiles

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface
internal constructor(var mContext: Context) {
    @JavascriptInterface
    fun showToast(toast: String?) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun receiveAuthToken(authToken: String) {
        Log.d("receiveAuthToken", "Received auth token: $authToken")
        Log.d("receiveAuthToken", "PREFS_NAME: $PREFS_NAME")
        val preferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d("receiveAuthToken", "TOKEN_KEY: $TOKEN_KEY")
        preferences.edit().putString(TOKEN_KEY, authToken).apply()
        // SharedPreferences.Editor editor = preferences.edit();
        // editor.putString(TOKEN_KEY, authToken);
        // editor.apply();
        Log.d("receiveAuthToken", "Auth Token Saved.")
    }

    companion object {
        private const val PREFS_NAME = "AppPreferences"
        private const val TOKEN_KEY = "auth_token"
    }
}
