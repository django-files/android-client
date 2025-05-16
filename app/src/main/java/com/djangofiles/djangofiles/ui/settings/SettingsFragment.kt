package com.djangofiles.djangofiles.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.djangofiles.djangofiles.DailyWorker
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.db.Server
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var dao: ServerDao
    //private lateinit var versionName: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "AppPreferences"
        setPreferencesFromResource(R.xml.settings, rootKey)

        //val packageName = requireContext().packageName
        //Log.i("Main[onCreate]", "packageName: $packageName")

        //versionName = requireContext()
        //    .packageManager
        //    .getPackageInfo(packageName, 0)
        //    .versionName ?: "Invalid Version"

        val workInterval = findPreference<ListPreference>("work_interval")
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        //workInterval?.setOnPreferenceClickListener {
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

        dao = ServerDatabase.getInstance(requireContext()).serverDao()

        parentFragmentManager.setFragmentResultListener("servers_updated", this) { _, _ ->
            buildServerList()
        }
        buildServerList()

        val filesPerPage = preferenceManager.sharedPreferences?.getInt("files_per_page", 0)
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

        findPreference<Preference>("add_server_btn")?.setOnPreferenceClickListener {
            Log.d("onCreatePreferences", "addServerBtn: $it")
            findNavController().navigate(R.id.nav_item_settings_action_login)
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
                        Log.d("processLogout", "ACTIVATE FIRST SERVER")
                        val newServer = servers.first()
                        Log.d("processLogout", "newServer: $newServer")
                        dao.activate(newServer.url)
                        preferenceManager.sharedPreferences!!.edit().apply {
                            putString("saved_url", newServer.url)
                            putString("auth_token", newServer.token)
                            apply()
                        }
                    } else {
                        Log.d("processLogout", "NO SERVERS - LOCK OUT")
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
}
