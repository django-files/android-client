package com.djangofiles.djangofiles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.text.format.DateFormat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.graphics.toColorInt
import com.djangofiles.djangofiles.MainActivity
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import com.djangofiles.djangofiles.updateStats
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Date

class WidgetProvider : AppWidgetProvider() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.i("Widget[onReceive]", "intent: $intent")

        //if (intent.action == "com.djangofiles.djangofiles.REFRESH_WIDGET") {
        //    val appWidgetManager = AppWidgetManager.getInstance(context)
        //    val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        //    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        //        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
        //    }
        //}

        if (intent.action == "com.djangofiles.djangofiles.REFRESH_WIDGET") {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return
            }
            Log.d("Widget[onReceive]", "GlobalScope.launch: START")
            GlobalScope.launch(Dispatchers.IO) {
                context.updateStats()
                // TODO: WidgetUpdate: Consolidate to a function...
                val appWidgetManager = AppWidgetManager.getInstance(context)
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
                Log.d("Widget[onReceive]", "GlobalScope.launch: DONE")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i("Widget[onUpdate]", "appWidgetIds: $appWidgetIds")

        val sharedPreferences = context.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val bgColor = sharedPreferences.getString("widget_bg_color", null) ?: "transparent"
        Log.i("Widget[onUpdate]", "bgColor: $bgColor")
        val textColor = sharedPreferences.getString("widget_text_color", null) ?: "transparent"
        Log.i("Widget[onUpdate]", "textColor: $textColor")

        val colorMap = mapOf(
            "white" to Color.WHITE,
            "black" to Color.BLACK,
            "liberty" to "#565AA9".toColorInt(),
            "transparent" to Color.TRANSPARENT
        )

        val selectedBgColor = colorMap[bgColor] ?: Color.TRANSPARENT
        Log.d("Widget[onUpdate]", "selectedBgColor: $selectedBgColor")
        val selectedTextColor = colorMap[textColor] ?: Color.WHITE
        Log.d("Widget[onUpdate]", "selectedTextColor: $selectedTextColor")

        appWidgetIds.forEach { appWidgetId ->
            Log.d("Widget[onUpdate]", "appWidgetId: $appWidgetId")

            // Widget Root
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val pendingIntent0: PendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply { action = Intent.ACTION_MAIN },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent0)

            // Refresh
            val intent1 = Intent(context, WidgetProvider::class.java).apply {
                action = "com.djangofiles.djangofiles.REFRESH_WIDGET"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent1 = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, pendingIntent1)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Set Colors
            views.setInt(R.id.widget_root, "setBackgroundColor", selectedBgColor)

            views.setTextColor(R.id.files_count, selectedTextColor)
            views.setTextColor(R.id.files_size, selectedTextColor)
            views.setTextColor(R.id.files_unit, selectedTextColor)
            views.setTextColor(R.id.update_time, selectedTextColor)

            views.setInt(R.id.files_icon, "setColorFilter", selectedTextColor)
            views.setInt(R.id.size_icon, "setColorFilter", selectedTextColor)

            views.setInt(R.id.widget_refresh_button, "setColorFilter", selectedTextColor)
            views.setInt(R.id.widget_upload_button, "setColorFilter", selectedTextColor)
            views.setInt(R.id.file_list_button, "setColorFilter", selectedTextColor)

            // Upload File
            val intent2 = Intent(context, MainActivity::class.java).apply {
                action = "UPLOAD_FILE"
            }
            val pendingIntent2 = PendingIntent.getActivity(
                context,
                0,
                intent2,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_upload_button, pendingIntent2)

            // File List
            val intent3 = Intent(context, MainActivity::class.java).apply {
                action = "FILE_LIST"
            }
            val pendingIntent3 = PendingIntent.getActivity(
                context,
                0,
                intent3,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.file_list_button, pendingIntent3)

            val sharedPreferences = context.applicationContext.getSharedPreferences(
                "AppPreferences",
                MODE_PRIVATE
            )
            val savedUrl = sharedPreferences.getString("saved_url", null).toString()
            Log.d("Widget[onUpdate]", "savedUrl: $savedUrl")

            GlobalScope.launch(Dispatchers.IO) {
                val dao: ServerDao =
                    ServerDatabase.Companion.getInstance(context.applicationContext).serverDao()
                Log.d("Widget[onUpdate]", "dao: $dao")
                val server = dao.getByUrl(savedUrl)
                Log.d("Widget[onUpdate]", "server: $server")
                if (server != null) {
                    Log.d("Widget[onUpdate]", "server.count: ${server.count}")
                    val filesCount =
                        if (server.count == null) "Unknown" else server.count.toString()
                    Log.d("Widget[onUpdate]", "filesCount: $filesCount")
                    views.setTextViewText(R.id.files_count, filesCount)

                    Log.d("Widget[onUpdate]", "server.humanSize: ${server.humanSize}")

                    val split =
                        server.humanSize?.split(' ')?.filter { it.isNotEmpty() } ?: emptyList()
                    Log.d("Widget[onUpdate]", "split: $split")
                    views.setTextViewText(R.id.files_size, split.getOrElse(0) { "Unknown" })
                    views.setTextViewText(R.id.files_unit, split.getOrElse(1) { "" })
                }

                val time = DateFormat.getTimeFormat(context).format(Date())
                Log.d("Widget[onUpdate]", "time: $time")
                views.setTextViewText(R.id.update_time, time)

                Log.d("Widget[onUpdate]", "updateAppWidget")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("Widget[onUpdate]", "updateAppWidget: DONE")
            }

            // This is done at the end of the GlobalScope above
            //appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d("Widget[onUpdate]", "appWidgetIds.forEach: DONE")
        }
        Log.d("Widget[onUpdate]", "onUpdate: DONE")
    }
}
