package com.djangofiles.djangofiles

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WidgetProvider : AppWidgetProvider() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        //if (intent.action == "com.djangofiles.djangofiles.REFRESH_WIDGET") {
        //    val appWidgetManager = AppWidgetManager.getInstance(context)
        //    val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        //    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        //        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
        //    }
        //}
        Log.i("Widget[onReceive]", "intent: $intent")
        if (intent.action == "com.djangofiles.djangofiles.REFRESH_WIDGET") {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return
            }
            Log.d("Widget[onReceive]", "GlobalScope.launch")
            GlobalScope.launch(Dispatchers.IO) {
                updateStats(context)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                //val views = buildUpdatedViews(context, appWidgetId)
                //appWidgetManager.updateAppWidget(appWidgetId, views)
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
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

        appWidgetIds.forEach { appWidgetId ->
            Log.d("Widget[onUpdate]", "appWidgetId: $appWidgetId")
            // Create an Intent to launch ExampleActivity.
            //val pendingIntent: PendingIntent = PendingIntent.getActivity(
            //    /* context = */ context,
            //    /* requestCode = */  0,
            //    /* intent = */ Intent(context, MainActivity::class.java),
            //    /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            //)
            //val views: RemoteViews = RemoteViews(
            //    context.packageName,
            //    R.layout.widget_layout
            //).apply {
            //    Log.d("Widget[onUpdate]", "debug 1")
            //    //setOnClickPendingIntent(R.id.button, pendingIntent)
            //}
            ////views.setOnClickPendingIntent(R.id.widget_upload_button, pendingIntent)

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

            //val time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmm"))
            //Log.d("Widget[onUpdate]", "time: $time")
            //views.setTextViewText(R.id.files_count, time)

            val sharedPreferences = context.applicationContext.getSharedPreferences(
                "AppPreferences",
                Context.MODE_PRIVATE
            )
            val savedUrl = sharedPreferences.getString("saved_url", null).toString()
            Log.d("Widget[onUpdate]", "savedUrl: $savedUrl")

            GlobalScope.launch(Dispatchers.IO) {
                val dao: ServerDao =
                    ServerDatabase.getInstance(context.applicationContext).serverDao()
                Log.d("Widget[onUpdate]", "dao: $dao")
                val server = dao.getByUrl(savedUrl)
                Log.d("Widget[onUpdate]", "server: $server")
                if (server != null) {
                    Log.d("Widget[onUpdate]", "files_count: ${server.count.toString()}")
                    views.setTextViewText(R.id.files_count, server.count.toString())
                    Log.d("Widget[onUpdate]", "files_size: ${server.humanSize}")
                    views.setTextViewText(R.id.files_size, server.humanSize)
                    //Log.d("Widget[onUpdate]", "shorts_count: ${server.shorts.toString()}")
                    //views.setTextViewText(R.id.shorts_count, server.shorts.toString())
                    Log.d("Widget[onUpdate]", "server: DONE")
                }
                Log.d("Widget[onUpdate]", "updateAppWidget")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("Widget[onUpdate]", "updateAppWidget: DONE")
            }

            // Tell the AppWidgetManager to perform an update on the current
            // widget.
            //appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d("Widget[onUpdate]", "appWidgetIds.forEach: DONE")
        }
        Log.d("Widget[onUpdate]", "onUpdate: DONE")
    }
}
