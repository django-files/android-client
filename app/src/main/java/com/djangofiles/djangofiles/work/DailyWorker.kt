package com.djangofiles.djangofiles.work

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import com.djangofiles.djangofiles.ui.files.getAlbums
import com.djangofiles.djangofiles.widget.WidgetProvider

class DailyWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("DailyWorker", "doWork: START")
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val savedUrl = preferences.getString("saved_url", null).toString()
        Log.d("DailyWorker", "savedUrl: $savedUrl")

        Log.d("DailyWorker", "--- Update Albums")
        try {
            applicationContext.getAlbums(savedUrl)
            Log.d("DailyWorker", "getAlbums: DONE")
        } catch (e: Exception) {
            Log.e("DailyWorker", "getAlbums: Exception: $e")
        }

        Log.d("DailyWorker", "--- Update Stats")
        try {
            applicationContext.updateStats()
        } catch (e: Exception) {
            Log.e("DailyWorker", "updateStats: Exception: $e")
        }

        //// Update Widget (Old Unknown Block - Test or Remove)
        //Log.d("DailyWorker", "updateAppWidget")
        //val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        //val componentName = ComponentName(applicationContext, WidgetProvider::class.java)
        //Log.d("DailyWorker", "componentName: $componentName")
        //val remoteViews = RemoteViews(applicationContext.packageName, R.layout.widget_layout)
        //appWidgetManager.updateAppWidget(componentName, remoteViews)

        // Update Widget
        // TODO: WidgetUpdate: Consolidate to a function...
        Log.d("DailyWorker", "--- Update Widget")
        val componentName = ComponentName(applicationContext, WidgetProvider::class.java)
        Log.d("DailyWorker", "componentName: $componentName")
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setClassName(
            applicationContext.packageName,
            "com.djangofiles.djangofiles.widget.WidgetProvider"
        ).apply {
            val ids =
                AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(componentName)
            Log.d("DailyWorker", "ids: $ids")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        Log.d("DailyWorker", "sendBroadcast: $intent")
        applicationContext.sendBroadcast(intent)

        //// Debugging Only
        //Log.d("DailyWorker", "--- Send Discord Message")
        //try {
        //    val url = BuildConfig.DISCORD_WEBHOOK
        //    Log.d("DailyWorker", "url: $url")
        //    if (url.isNotEmpty()) {
        //        val discordApi = DiscordApi(applicationContext, url)
        //        val uniqueID = preferences.getString("unique_id", null)
        //        Log.d("DailyWorker", "uniqueID: $uniqueID")
        //        val response = discordApi.sendMessage("DAILY WORK: `$uniqueID`")
        //        Log.d("DailyWorker", "response: $response")
        //    }
        //} catch (e: Exception) {
        //    Log.e("DailyWorker", "discordApi: Exception: $e")
        //}

        return Result.success()
    }
}

suspend fun Context.updateStats(): Boolean {
    Log.d("updateStats", "updateStats")
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val savedUrl = preferences.getString("saved_url", null).toString()
    Log.d("updateStats", "savedUrl: $savedUrl")
    val api = ServerApi(this, savedUrl)
    val statsResponse = api.current()
    Log.d("updateStats", "statsResponse: $statsResponse")
    if (statsResponse.isSuccessful) {
        val stats = statsResponse.body()
        Log.d("updateStats", "stats: $stats")
        if (stats != null) {
            val dao: ServerDao = ServerDatabase.getInstance(this).serverDao()
            // TODO: Add a helper function for this...
            dao.updateStats(
                url = savedUrl,
                size = stats.size,
                count = stats.count,
                shorts = stats.shorts,
                humanSize = stats.humanSize,
            )
            Log.d("updateStats", "dao.addOrUpdate: DONE")

            // TODO: WidgetUpdate: Consolidate to a function...
            //  This seems to run a double update, disabling until turned into a function
            //Log.i("updateStats", "Updating Widget")
            //val appWidgetManager = AppWidgetManager.getInstance(this)
            //val widgetComponent = ComponentName(this, WidgetProvider::class.java)
            //val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
            //val intent = Intent(this, WidgetProvider::class.java).apply {
            //    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            //    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            //}
            //this.sendBroadcast(intent)

            return true
        }
    }
    return false
}
