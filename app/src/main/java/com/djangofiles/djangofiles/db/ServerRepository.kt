package com.djangofiles.djangofiles.db

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * In-memory cache of the saved server list.
 *
 * Reading ServerDao directly costs a Dispatchers.IO round trip whose result is only
 * handed back on the main thread. When the main thread is already busy inflating a
 * screen that resumption is queued behind the in-flight traversals, so a Room read
 * issued from onCreatePreferences can never land in the first frame.
 *
 * Warming this from MainActivity.onCreate moves the read to app start, where it
 * overlaps work the main thread is already doing. Callers then read [servers]
 * synchronously and render in the same frame.
 *
 * Room's InvalidationTracker re-runs observeAll on any write to the server table,
 * so the cache cannot go stale and callers need no manual refresh after a write.
 */
object ServerRepository {

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private var scope: CoroutineScope? = null

    fun initialize(context: Context) {
        if (scope != null) {
            Log.d("ServerRepository", "ALREADY INITIALIZED")
            return
        }
        val dao = ServerDatabase.getInstance(context).serverDao()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { s ->
            s.launch {
                dao.observeAll().collect { list ->
                    Log.d("ServerRepository", "servers updated: ${list.size}")
                    _servers.value = list
                }
            }
        }
        Log.d("ServerRepository", "INITIALIZED")
    }
}
