package com.djangofiles.djangofiles.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioGroup
import androidx.core.content.edit
import com.djangofiles.djangofiles.R

class WidgetConfiguration : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_configure)

        window.statusBarColor = Color.TRANSPARENT

        setResult(RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish()

        val preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val bgColor = preferences.getString("widget_bg_color", null) ?: "transparent"
        Log.i("WidgetConfiguration", "bgColor: $bgColor")
        val textColor = preferences.getString("widget_text_color", null) ?: "white"
        Log.i("WidgetConfiguration", "textColor: $textColor")

        val bgColorId = mapOf(
            "white" to R.id.option_white,
            "black" to R.id.option_black,
            "liberty" to R.id.option_liberty,
            "transparent" to R.id.option_transparent
        )
        val textColorId = mapOf(
            "white" to R.id.text_white,
            "black" to R.id.text_black,
            "liberty" to R.id.text_liberty,
        )

        val backgroundOptions = findViewById<RadioGroup>(R.id.background_options)
        val bgSelected = bgColorId[bgColor]
        if (bgSelected != null) backgroundOptions.check(bgSelected)
        val textOptions = findViewById<RadioGroup>(R.id.text_options)
        val textSelected = textColorId[textColor]
        if (textSelected != null) textOptions.check(textSelected)

        val confirmButton = findViewById<Button>(R.id.confirm_button)
        confirmButton.setOnClickListener {
            val selectedBgColor = when (backgroundOptions.checkedRadioButtonId) {
                R.id.option_white -> "white"
                R.id.option_black -> "black"
                R.id.option_liberty -> "liberty"
                R.id.option_transparent -> "transparent"
                else -> "transparent"
            }
            Log.i("WidgetConfiguration", "selectedBgColor: $selectedBgColor")

            val selectedTextColor = when (textOptions.checkedRadioButtonId) {
                R.id.text_white -> "white"
                R.id.text_black -> "black"
                R.id.text_liberty -> "liberty"
                else -> "white"
            }
            Log.i("WidgetConfiguration", "selectedTextColor: $selectedTextColor")

            preferences.edit {
                putString("widget_bg_color", selectedBgColor)
                putString("widget_text_color", selectedTextColor)
            }

            val updateIntent = Intent(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                null,
                this,
                WidgetProvider::class.java
            )
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            sendBroadcast(updateIntent)

            val result = Intent()
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, result)
            finish()
        }
    }
}
