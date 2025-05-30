package com.djangofiles.djangofiles.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.djangofiles.djangofiles.DailyWorker
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.api.FeedbackApi
import com.djangofiles.djangofiles.db.Server
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var dao: ServerDao

    @SuppressLint("BatteryLife")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "AppPreferences"
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Files Per Page
        val filesPerPage = preferenceManager.sharedPreferences?.getInt("files_per_page", 25)
        Log.d("onCreatePreferences", "filesPerPage: $filesPerPage")
        val seekBar = findPreference<SeekBarPreference>("files_per_page")
        seekBar?.summary = "Current Value: $filesPerPage"
        seekBar?.apply {
            setOnPreferenceChangeListener { pref, newValue ->
                val intValue = (newValue as Int)
                var stepped = ((intValue + 2) / 5) * 5
                if (stepped < 10) stepped = 10
                Log.d("onCreatePreferences", "stepped: $stepped")
                value = stepped
                pref.summary = "Current Value: $stepped"
                false
            }
        }

        // Background Update Interval
        val workInterval = findPreference<ListPreference>("work_interval")
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        workInterval?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("setOnPreferenceClickListener", "workInterval.value: ${workInterval.value}")
            Log.d("setOnPreferenceClickListener", "newValue: $newValue")
            if (workInterval.value != newValue) {
                Log.i("setOnPreferenceClickListener", "RESCHEDULING WORK")
                val interval = (newValue as? String)?.toLongOrNull()
                Log.i("setOnPreferenceClickListener", "interval: $interval")
                if (interval != null) {
                    val newRequest =
                        PeriodicWorkRequestBuilder<DailyWorker>(interval, TimeUnit.MINUTES)
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiresBatteryNotLow(true)
                                    .setRequiresCharging(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                    .build()
                            )
                            .build()
                    WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                        "daily_worker",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        newRequest
                    )
                } else {
                    Log.i("setOnPreferenceClickListener", "DISABLING WORK")
                    WorkManager.getInstance(requireContext()).cancelUniqueWork("daily_worker")
                }
                Log.d("setOnPreferenceClickListener", "true: ACCEPTED")
                true
            } else {
                Log.d("setOnPreferenceClickListener", "false: REJECTED")
                false
            }
        }

        // Background Restriction
        val packageName = requireContext().packageName
        Log.i("onCreatePreferences", "packageName: $packageName")
        val pm = requireContext().getSystemService(PowerManager::class.java)
        val batteryRestrictedButton = findPreference<Preference>("battery_unrestricted")
        fun checkBackground(): Boolean {
            val isIgnoring = pm.isIgnoringBatteryOptimizations(packageName)
            Log.i("onCreatePreferences", "isIgnoring: $isIgnoring")
            if (isIgnoring) {
                Log.i("onCreatePreferences", "DISABLING BACKGROUND BUTTON")
                batteryRestrictedButton?.setSummary("Permission Already Granted")
                batteryRestrictedButton?.isEnabled = false
            }
            return isIgnoring
        }
        checkBackground()
        batteryRestrictedButton?.setOnPreferenceClickListener {
            Log.d("onCreatePreferences", "batteryRestrictedButton?.setOnPreferenceClickListener")
            if (!checkBackground()) {
                val uri = "package:$packageName".toUri()
                Log.d("onCreatePreferences", "uri: $uri")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = uri
                }
                Log.d("onCreatePreferences", "intent: $intent")
                startActivity(intent)
            }
            false
        }

        // Add Server Button
        findPreference<Preference>("add_server_btn")?.setOnPreferenceClickListener {
            Log.d("onCreatePreferences", "addServerBtn: $it")
            findNavController().navigate(R.id.nav_item_settings_action_login)
            false
        }

        // Server List
        dao = ServerDatabase.getInstance(requireContext()).serverDao()
        parentFragmentManager.setFragmentResultListener("servers_updated", this) { _, _ ->
            buildServerList()
        }
        buildServerList()

        // Toggle Analytics
        val toggleAnalytics = findPreference<SwitchPreferenceCompat>("analytics_enabled")
        toggleAnalytics?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("toggleAnalytics", "analytics_enabled: $newValue")
            if (newValue as Boolean) {
                Log.d("toggleAnalytics", "ENABLE Analytics")
                Firebase.analytics.setAnalyticsCollectionEnabled(true)
                toggleAnalytics.isChecked = true
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Please Reconsider")
                    .setMessage("Analytics are only used to fix bugs and make improvements.")
                    .setPositiveButton("Disable Anyway") { _, _ ->
                        Log.d("toggleAnalytics", "DISABLE Analytics")
                        Firebase.analytics.logEvent("disable_analytics", null)
                        Firebase.analytics.setAnalyticsCollectionEnabled(false)
                        toggleAnalytics.isChecked = false
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            false
        }

        // Send Feedback
        val sendFeedback = findPreference<Preference>("send_feedback")
        sendFeedback?.setOnPreferenceClickListener {
            Log.d("sendFeedback", "setOnPreferenceClickListener")
            requireContext().showFeedbackDialog()
            false
        }

        // Show App Info
        findPreference<Preference>("app_info")?.setOnPreferenceClickListener {
            Log.d("app_info", "showAppInfoDialog")
            requireContext().showAppInfoDialog()
            false
        }

        // Open App Settings
        findPreference<Preference>("android_settings")?.setOnPreferenceClickListener {
            Log.d("android_settings", "setOnPreferenceClickListener")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
            false
        }
    }

    private fun buildServerList() {
        lifecycleScope.launch {
            val servers = withContext(Dispatchers.IO) {
                dao.getAll()
            }
            Log.d("buildServerList", "servers: $servers")

            val category = findPreference<PreferenceCategory>("server_list") ?: return@launch
            category.removeAll()

            val savedUrl = preferenceManager.sharedPreferences?.getString("saved_url", "")
            Log.d("buildServerList", "savedUrl: $savedUrl")

            servers.forEach { server ->
                val pref = ServerPreference(
                    requireContext(),
                    server = server,
                    onEdit = { s -> activateServer(s, savedUrl) },
                    onDelete = { s -> showDeleteDialog(s) },
                    savedUrl = savedUrl
                )
                category.addPreference(pref)
            }
        }
    }

    private fun activateServer(server: Server, savedUrl: String?) {
        Log.d("activateServer", "server.url: ${server.url}")
        Log.d("activateServer", "server.token: ${server.token}")
        if (server.url == savedUrl) {
            Log.d("activateServer", "server ALREADY ACTIVE - RETURN")
            return
        }
        preferenceManager.sharedPreferences?.edit()?.apply {
            putString("saved_url", server.url)
            putString("auth_token", server.token)
            apply()
        }
        buildServerList()
    }

    private fun showDeleteDialog(server: Server) {
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Delete Server?")
            .setIcon(R.drawable.md_delete_24px)
            .setMessage("Are you sure you want to delete this server?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    dao.delete(server)
                    val servers = dao.getAll()
                    if (!servers.isEmpty()) {
                        Log.d("showDeleteDialog", "ACTIVATE FIRST SERVER")
                        val newServer = servers.first()
                        Log.d("showDeleteDialog", "newServer: $newServer")
                        dao.activate(newServer.url)
                        preferenceManager.sharedPreferences!!.edit().apply {
                            putString("saved_url", newServer.url)
                            putString("auth_token", newServer.token)
                            apply()
                        }
                    } else {
                        Log.d("showDeleteDialog", "NO SERVERS - LOCK OUT")
                        // TODO: Confirm this removes history and locks user to login
                        findNavController().navigate(
                            R.id.nav_item_login, null, NavOptions.Builder()
                                .setPopUpTo(R.id.nav_item_settings, true)
                                .build()
                        )
                    }
                    buildServerList()
                }
            }
            .show()
    }

    fun Context.showFeedbackDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_feedback, null)
        val input = view.findViewById<EditText>(R.id.feedback_input)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send", null)
            .create()

        dialog.setOnShowListener {
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            sendButton.setOnClickListener {
                sendButton.isEnabled = false
                val message = input.text.toString().trim()
                Log.d("showFeedbackDialog", "message: $message")
                if (message.isNotEmpty()) {
                    val api = FeedbackApi(this)
                    lifecycleScope.launch {
                        val response = withContext(Dispatchers.IO) { api.sendFeedback(message) }
                        Log.d("showFeedbackDialog", "response: $response")
                        val msg = if (response.isSuccessful) {
                            findPreference<Preference>("send_feedback")?.isEnabled = false
                            dialog.dismiss()
                            "Feedback Sent. Thank You!"
                        } else {
                            sendButton.isEnabled = true
                            //val params = Bundle().apply {
                            //    putString("message", response.message())
                            //    putString("code", response.code().toString())
                            //}
                            //Firebase.analytics.logEvent("feedback_failed", params)
                            "Error: ${response.code()}"
                        }
                        Log.d("showFeedbackDialog", "msg: $msg")
                        Toast.makeText(this@showFeedbackDialog, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    sendButton.isEnabled = true
                    input.error = "Feedback is Required"
                }
            }

            input.requestFocus()

            val link = view.findViewById<TextView>(R.id.github_link)
            val linkText = getString(R.string.github_link, link.tag)
            link.text = Html.fromHtml(linkText, Html.FROM_HTML_MODE_LEGACY)
            link.movementMethod = LinkMovementMethod.getInstance()

            //val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Send") { _, _ -> }
        dialog.show()
    }

    fun Context.showAppInfoDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_app_info, null)
        val appId = view.findViewById<TextView>(R.id.app_identifier)
        val appVersion = view.findViewById<TextView>(R.id.app_version)
        val sourceLink = view.findViewById<TextView>(R.id.source_link)
        val websiteLink = view.findViewById<TextView>(R.id.website_link)
        //val appInfo = view.findViewById<TextView>(R.id.open_app_info)

        val sourceText = getString(R.string.github_link, sourceLink.tag)
        Log.d("showAppInfoDialog", "sourceText: $sourceText")

        val websiteText = getString(R.string.website_link, websiteLink.tag)
        Log.d("showAppInfoDialog", "websiteText: $websiteText")

        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d("showAppInfoDialog", "versionName: $versionName")

        val formattedVersion = getString(R.string.version_string, versionName)
        Log.d("showAppInfoDialog", "formattedVersion: $formattedVersion")

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()

        dialog.setOnShowListener {
            appId.text = this.packageName
            appVersion.text = formattedVersion

            sourceLink.text = Html.fromHtml(sourceText, Html.FROM_HTML_MODE_LEGACY)
            sourceLink.movementMethod = LinkMovementMethod.getInstance()
            websiteLink.text = Html.fromHtml(websiteText, Html.FROM_HTML_MODE_LEGACY)
            websiteLink.movementMethod = LinkMovementMethod.getInstance()

            //appInfo.setOnClickListener {
            //    Log.d("appInfo", "setOnClickListener")
            //    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            //        data = Uri.fromParts("package", packageName, null)
            //    }
            //    startActivity(intent)
            //}
        }
        dialog.show()
    }
}

//val pm = requireContext().getSystemService(PowerManager::class.java)
//val isIgnoring = pm.isIgnoringBatteryOptimizations(packageName)
//Log.d("Main[onCreate]", "isIgnoring: $isIgnoring")
//val batteryRestrictedButton = findPreference<Preference>("battery_unrestricted")
//val category = findPreference<PreferenceCategory>("app_options")
//if (batteryRestrictedButton != null && isIgnoring) {
//    Log.i("Main[onCreate]", "REMOVING IT")
//    category?.removePreference(batteryRestrictedButton)
//}
//batteryRestrictedButton?.setOnPreferenceClickListener {
//    val uri = "package:$packageName".toUri()
//    Log.i("Main[onCreate]", "uri: $uri")
//    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
//        data = uri
//    }
//    Log.i("Main[onCreate]", "intent: $intent")
//    startActivity(intent)
//    false
//}
