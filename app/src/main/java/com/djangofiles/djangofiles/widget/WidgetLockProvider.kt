package com.djangofiles.djangofiles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager
import com.djangofiles.djangofiles.MainActivity
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.db.ServerDatabase
import com.djangofiles.djangofiles.work.updateStats
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class WidgetLockProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.i("LockWidget[onReceive]", "intent: $intent")

        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return
            }
            Log.d("LockWidget[onReceive]", "refresh START")
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                context.updateStats()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
                Log.d("LockWidget[onReceive]", "refresh DONE")
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i("LockWidget[onUpdate]", "BEGIN - ${appWidgetIds.contentToString()}")

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val savedUrl = preferences.getString("saved_url", null).toString()
        val bgColor = preferences.getString("widget_bg_color", null) ?: "black"
        val textColor = preferences.getString("widget_text_color", null) ?: "white"
        val bgOpacity = preferences.getInt("widget_bg_opacity", 35)
        val showUpdateTime = preferences.getBoolean("widget_show_update_time", true)
        val showRefresh = preferences.getBoolean("widget_show_refresh", true)
        val showUpload = preferences.getBoolean("widget_show_upload", true)
        val showText = preferences.getBoolean("widget_show_text", false)

        val colorMap = mapOf(
            "white" to Color.WHITE,
            "black" to Color.BLACK,
            "liberty" to "#565AA9".toColorInt(),
        )
        val selectedBgColor = colorMap[bgColor] ?: Color.BLACK
        val selectedTextColor = colorMap[textColor] ?: Color.WHITE
        val alpha = (bgOpacity * 255 / 100).coerceIn(1, 255)
        val finalBgColor = ColorUtils.setAlphaComponent(selectedBgColor, alpha)

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_lock_layout)
            val rootClick = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply { action = Intent.ACTION_MAIN },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_lock_root, rootClick)
            views.setInt(R.id.widget_lock_root, "setBackgroundColor", finalBgColor)

            val pillBackground = when (textColor) {
                "black" -> R.drawable.bg_lock_ripple_black
                "liberty" -> R.drawable.bg_lock_ripple_liberty
                else -> R.drawable.bg_lock_ripple_white
            }
            views.setInt(R.id.lock_text_pill, "setBackgroundResource", pillBackground)
            views.setInt(R.id.lock_upload_pill, "setBackgroundResource", pillBackground)
            views.setInt(R.id.lock_refresh_pill, "setBackgroundResource", pillBackground)

            views.setTextColor(R.id.lock_files_summary, selectedTextColor)
            views.setTextColor(R.id.lock_shorts_summary, selectedTextColor)
            views.setTextColor(R.id.lock_separator_text, selectedTextColor)
            views.setTextColor(R.id.lock_albums_summary, selectedTextColor)
            views.setTextColor(R.id.lock_update_time, selectedTextColor)
            views.setTextColor(R.id.lock_text_pill_label, selectedTextColor)
            views.setTextColor(R.id.lock_upload_pill_label, selectedTextColor)
            views.setTextColor(R.id.lock_legend0_name, selectedTextColor)
            views.setTextColor(R.id.lock_legend1_name, selectedTextColor)
            views.setTextColor(R.id.lock_legend2_name, selectedTextColor)
            views.setTextColor(R.id.lock_legend3_name, selectedTextColor)
            views.setTextColor(R.id.lock_legend4_name, selectedTextColor)
            views.setTextColor(R.id.lock_legend5_name, selectedTextColor)

            views.setInt(R.id.lock_refresh_button, "setColorFilter", selectedTextColor)
            views.setInt(R.id.lock_text_button, "setColorFilter", selectedTextColor)
            views.setInt(R.id.lock_upload_button, "setColorFilter", selectedTextColor)

            views.setViewVisibility(
                R.id.lock_refresh_pill,
                if (showRefresh) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.lock_text_pill,
                if (showText) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.lock_upload_pill,
                if (showUpload) View.VISIBLE else View.GONE
            )

            val refreshIntent = Intent(context, WidgetLockProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.lock_refresh_button, refreshPending)
            views.setOnClickPendingIntent(R.id.lock_refresh_pill, refreshPending)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            val uploadIntent = Intent(context, MainActivity::class.java).apply {
                action = "UPLOAD_FILE"
            }
            val uploadPending = PendingIntent.getActivity(
                context, 0, uploadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.lock_upload_button, uploadPending)
            views.setOnClickPendingIntent(R.id.lock_upload_pill, uploadPending)

            val textIntent = Intent(context, MainActivity::class.java).apply {
                action = "UPLOAD_TEXT"
            }
            val textPending = PendingIntent.getActivity(
                context, 0, textIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.lock_text_button, textPending)
            views.setOnClickPendingIntent(R.id.lock_text_pill, textPending)

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val dao = ServerDatabase.getInstance(context).serverDao()
                val server = dao.getByUrl(savedUrl)
                if (server != null) {
                    val count = server.count ?: 0
                    val humanSize = server.humanSize ?: "0 B"
                    val shorts = server.shorts ?: 0
                    val albums = server.albums ?: 0
                    views.setTextViewText(R.id.lock_files_summary, "Files: $count ($humanSize)")
                    views.setTextViewText(R.id.lock_shorts_summary, "Shorts: $shorts")
                    views.setTextViewText(R.id.lock_albums_summary, "Albums: $albums")

                    val types = parseTypes(server.typesJson)
                    if (types.isNotEmpty()) {
                        val (_, legend) = buildPieModel(types)
                        setLegend(views, legend)
                        val bitmap = withContext(Dispatchers.Main) {
                            renderPieBitmap(context, types, selectedTextColor)
                        }
                        if (bitmap != null) {
                            views.setViewVisibility(R.id.lock_chart, View.VISIBLE)
                            views.setImageViewBitmap(R.id.lock_chart, bitmap)
                        } else {
                            views.setViewVisibility(R.id.lock_chart, View.GONE)
                        }
                    } else {
                        // Server known but no type breakdown cached yet (sync never
                        // succeeded on this app variant). Clear stale placeholders.
                        setLegend(views, emptyList())
                        views.setViewVisibility(R.id.lock_chart, View.GONE)
                    }
                } else {
                    // No cached server (fresh install / logged out on this app
                    // variant): say so instead of showing placeholder values.
                    views.setTextViewText(R.id.lock_files_summary, "Open app and log in")
                    views.setTextViewText(R.id.lock_shorts_summary, "")
                    views.setTextViewText(R.id.lock_albums_summary, "")
                    setLegend(views, emptyList())
                    views.setViewVisibility(R.id.lock_chart, View.GONE)
                }

                val time = DateFormat.getTimeFormat(context).format(Date())
                views.setViewVisibility(
                    R.id.lock_update_time,
                    if (showUpdateTime) View.VISIBLE else View.GONE
                )
                views.setTextViewText(R.id.lock_update_time, "Updated $time")

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
        Log.i("LockWidget[onUpdate]", "END")
    }

    companion object {
        const val ACTION_REFRESH = "com.djangofiles.djangofiles.REFRESH_LOCK_WIDGET"

        // Nightfall palette from the README gallery (MPChartExample Nightfall.kt):
        // teal, coral, amber, violet, green. Slate 6th for the grouped "other" slice.
        private val CHART_COLORS = listOf(
            "#2FB4B6".toColorInt(),
            "#F97066".toColorInt(),
            "#F5B041".toColorInt(),
            "#8B7CF6".toColorInt(),
            "#5DD39E".toColorInt(),
            "#94A3B8".toColorInt(),
        )

        fun parseTypes(typesJson: String?): List<ServerApi.TypeStats> {
            if (typesJson.isNullOrBlank()) return emptyList()
            return try {
                val type = object : TypeToken<List<ServerApi.TypeStats>>() {}.type
                GsonBuilder().create().fromJson(typesJson, type) ?: emptyList()
            } catch (e: Exception) {
                Log.e("LockWidget", "parseTypes failed: ${e.message}")
                emptyList()
            }
        }

        data class LegendItem(val color: Int, val name: String)

        fun centerLabel(mime: String): String {
            if (mime == "Other") return "other"
            if (mime == "unknown" || mime.isBlank()) return "other"
            val sub = mime.substringAfter("/", mime)
            if (sub.startsWith("vnd.") || sub.startsWith("x-") || sub.length > 12) {
                return mime.substringBefore("/", mime).take(12)
            }
            return sub.take(12)
        }

        fun legendLabel(mime: String): String {
            if (mime == "Other") return "other"
            return mime
        }

        fun buildPieModel(types: List<ServerApi.TypeStats>): Pair<List<PieEntry<Nothing>>, List<LegendItem>> {
            val sorted = types.sortedByDescending { it.count }.filter { it.count > 0 }
            if (sorted.isEmpty()) return emptyList<PieEntry<Nothing>>() to emptyList()
            val top = sorted.take(5)
            val rest = sorted.drop(5)
            val restCount = rest.sumOf { it.count }
            val slices = top.toMutableList()
            if (restCount > 0) {
                slices.add(
                    ServerApi.TypeStats(
                        mime = "Other",
                        count = restCount,
                        size = rest.sumOf { it.size },
                        humanSize = ""
                    )
                )
            }
            val entries: List<PieEntry<Nothing>> =
                slices.map { PieEntry(it.count.toFloat().coerceAtLeast(1f), "") }
            val legend = slices.mapIndexed { index, t ->
                val label = if (t.mime == "Other") "other" else legendLabel(t.mime)
                LegendItem(
                    color = CHART_COLORS[index % CHART_COLORS.size],
                    name = label
                )
            }
            return entries to legend
        }

        fun setLegend(views: RemoteViews, legend: List<LegendItem>) {
            val colorIds = listOf(
                R.id.lock_legend0_color,
                R.id.lock_legend1_color,
                R.id.lock_legend2_color,
                R.id.lock_legend3_color,
                R.id.lock_legend4_color,
                R.id.lock_legend5_color,
            )
            val nameIds = listOf(
                R.id.lock_legend0_name,
                R.id.lock_legend1_name,
                R.id.lock_legend2_name,
                R.id.lock_legend3_name,
                R.id.lock_legend4_name,
                R.id.lock_legend5_name,
            )
            for (i in colorIds.indices) {
                val item = legend.getOrNull(i)
                if (item == null) {
                    views.setViewVisibility(colorIds[i], View.GONE)
                    views.setViewVisibility(nameIds[i], View.GONE)
                } else {
                    views.setViewVisibility(colorIds[i], View.VISIBLE)
                    views.setViewVisibility(nameIds[i], View.VISIBLE)
                    views.setInt(colorIds[i], "setBackgroundColor", item.color)
                    views.setTextViewText(nameIds[i], item.name)
                }
            }
        }

        fun renderPieBitmap(
            context: Context,
            types: List<ServerApi.TypeStats>,
            textColor: Int,
        ): android.graphics.Bitmap? {
            return try {
                val (entries, _) = buildPieModel(types)
                if (entries.isEmpty()) return null
                val ranked = types.sortedByDescending { it.count }.filter { it.count > 0 }
                val total = ranked.sumOf { it.count }.toFloat()
                val top = ranked.firstOrNull()
                val holeText = if (top != null && total > 0) {
                    val pct = ((top.count / total) * 100).toInt()
                    "$pct%\n${centerLabel(top.mime)}"
                } else {
                    ""
                }
                val dataSet = PieDataSet(entries, "").apply {
                    colors = CHART_COLORS
                    sliceSpace = 3f
                    selectionShift = 0f
                    isDrawValuesEnabled = false
                }
                val pieData = PieData(dataSet)
                val chart = PieChart(context).apply {
                    data = pieData
                    description.isEnabled = false
                    legend.isEnabled = false
                    isDrawEntryLabelsEnabled = false
                    isDrawHoleEnabled = true
                    holeRadius = 72f
                    transparentCircleRadius = 0f
                    isDrawRoundedSlicesEnabled = true
                    isDrawCenterTextEnabled = holeText.isNotEmpty()
                    centerText = holeText
                    centerTextSize = 24f
                    centerTextColor = textColor
                    holeColor = Color.TRANSPARENT
                    setBackgroundColor(Color.TRANSPARENT)
                    isRotationEnabled = false
                    isHighlightPerTapEnabled = false
                }
                // 600x375 (~900KB) stays under the ~1MB RemoteViews bitmap budget
                // while supersampling the displayed size for crisper edges.
                val width = 600
                val height = 375
                chart.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                chart.layout(0, 0, width, height)
                chart.invalidate()
                chart.toBitmap()
            } catch (e: Exception) {
                Log.e("LockWidget", "renderPieBitmap failed: ${e.message}")
                try {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                        .recordException(e)
                } catch (_: Exception) {
                }
                null
            }
        }
    }
}
