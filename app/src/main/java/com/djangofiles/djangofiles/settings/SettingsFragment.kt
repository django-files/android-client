package com.djangofiles.djangofiles.settings

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.djangofiles.djangofiles.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject


//import android.util.Patterns
//import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var dao: ServerDao

    private val client = OkHttpClient()

    private val serverKey = "servers"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "AppPreferences"
        setPreferencesFromResource(R.xml.pref_root, rootKey)

        val db = Room.databaseBuilder(requireContext(), ServerDatabase::class.java, "server-database")
            .build()
        dao = db.serverDao()

        buildServerList()
        setupAddServer()

        CoroutineScope(Dispatchers.IO).launch {
            val serverList = dao.getAll() // Fetch data in background
            Log.d("onCreatePreferences", "serverList: $serverList")
            withContext(Dispatchers.Main) {
                // Update the UI on the main thread
                Log.d("onCreatePreferences", "IM ON THE UI BABY")
            }
        }
    }

    private fun setupAddServer() {
        findPreference<Preference>("add_server")?.setOnPreferenceClickListener {
            val editText = EditText(requireContext()).apply {
                inputType = InputType.TYPE_TEXT_VARIATION_URI
                hint = "https://example.com"
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Add Server")
                .setView(editText)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", null)
                .show().apply {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        var url = editText.text.toString().trim()
                        Log.d("showSettingsDialog", "setPositiveButton URL: $url")
                        if (url.isEmpty()) {
                            Log.d("showSettingsDialog", "URL is Empty")
                            editText.error = "This field is required."
                        } else {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://$url"
                            }
                            if (url.endsWith("/")) {
                                url = url.substring(0, url.length - 1)
                            }

                            val servers = loadServers().toMutableList()
                            Log.d("showSettingsDialog", "servers: $servers")

                            CoroutineScope(Dispatchers.IO).launch {
                                    val response = checkUrl(url)
                                    Log.d("showSettingsDialog", "response: $response")
                                    withContext(Dispatchers.Main) {
                                        if (response) {
                                            Log.d("showSettingsDialog", "SUCCESS")
                                            //saveServers(servers)
                                            buildServerList()
                                            cancel()
                                        } else {
                                            Log.d("showSettingsDialog", "FAILURE")
                                            editText.error = "Invalid URL"
                                        }
                                    }
                            }
                        }
                    }
                }

            true
        }
    }

    private fun checkUrl(url: String): Boolean {
        Log.d("checkUrl", "checkUrl URL: $url")
        val existingServer = dao.getByUrl(url)
        Log.d("checkUrl", "existingServer: $existingServer")
        if (existingServer != null) {
            Log.d("checkUrl", "Error: Server Exists!")
            return false
        }

        val authUrl = "${url}/api/auth/methods/"
        Log.d("showSettingsDialog", "Auth URL: $authUrl")

        // TODO: Change this to HEAD or use response data...
        val request = Request.Builder().header("User-Agent", "DF").url(authUrl).get().build()
        return try {
            val response = client.newCall(request).execute()
            Log.d("checkUrl", "Success: Remote OK.")
            dao.add(Server(url = url))
            response.isSuccessful
        } catch (e: Exception) {
            Log.d("checkUrl", "Error: Remote Failed!")
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
                    onEdit = { s -> showEditDialog(s, savedUrl) },
                    onDelete = { s -> showDeleteDialog(s) },
                    savedUrl = savedUrl
                )
                category.addPreference(pref)
            }
        }
    }



    private fun showEditDialog(server: Server, savedUrl: String?) {
        //val editText = EditText(requireContext()).apply {
        //    inputType = InputType.TYPE_TEXT_VARIATION_URI
        //    setText(entry.url)
        //}

        Log.d("showEditDialog", "server.url: ${server.url}")
        Log.d("showEditDialog", "server.token: ${server.token}")

        if (server.url == savedUrl) {
            Log.d("showEditDialog", "server ALREADY ACTIVE - RETURN")
            return
        }

        val sharedPreferences = preferenceManager.sharedPreferences
        sharedPreferences?.edit()?.apply {
            putString("saved_url", server.url)
            putString("auth_token", server.token)
            apply()
        }
        buildServerList()

        //val servers = loadServers().toMutableList()
        //val token = servers.find { it.url == entry.url }?.token ?: ""
        //Log.d("showEditDialog", "token: $token")


        //AlertDialog.Builder(requireContext())
        //    .setTitle("Edit Server")
        //    .setView(editText)
        //    .setPositiveButton("Save") { _, _ ->
        //        val newUrl = editText.text.toString().trim()
        //        if (newUrl.isNotEmpty()) {
        //            val servers = loadServers().toMutableList()
        //            servers[index] = servers[index].copy(url = newUrl)
        //            saveServers(servers)
        //            buildServerList()
        //        }
        //    }
        //    .setNegativeButton("Cancel", null)
        //    .show()
    }

    private fun showDeleteDialog(server: Server) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Server?")
            .setMessage("Are you sure you want to delete this server?")
            .setPositiveButton("Delete") { _, _ ->
                //val servers = loadServers().toMutableList()
                //servers.removeAt(index)
                //saveServers(servers)
                CoroutineScope(Dispatchers.IO).launch {
                    dao.delete(server)
                }
                buildServerList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadServers(): List<ServerEntry> {
        val json = preferenceManager.sharedPreferences?.getString(serverKey, "[]") ?: "[]"
        return try {
            JSONArray(json).let { array ->
                List(array.length()) {
                    val obj = array.getJSONObject(it)
                    ServerEntry(
                        url = obj.getString("url"),
                        token = obj.optString("token", "")
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

//    private fun saveServers(list: List<ServerEntry>) {
//        val array = JSONArray().apply {
//            list.forEach {
//                put(JSONObject().apply {
//                    put("url", it.url)
//                    put("token", it.token)
//                })
//            }
//        }
//        preferenceManager.sharedPreferences?.edit() { putString(serverKey, array.toString()) }
//    }

    data class ServerEntry(val url: String, val token: String)

//    private fun Preference.setOnPreferenceLongClickListener(listener: (Preference) -> Boolean) {
//        this.viewLifecycleOwnerLiveData.observe(viewLifecycleOwner) { viewLifecycleOwner ->
//            if (viewLifecycleOwner != null) {
//                this.setOnPreferenceClickListener(null)
//                this.setOnPreferenceClickListener {
//                    false
//                }
//                this.preferenceView?.setOnLongClickListener {
//                    listener(this)
//                }
//            }
//        }
//    }

//    private fun setOnPreferenceLongClickListener(preference: Preference, listener: (Preference) -> Boolean) {
//        preference.setOnLongClickListener {
//            listener(preference)
//        }
//    }

//    private val Preference.preferenceView: View?
//        get() = (listView?.findViewHolderForAdapterPosition(preferenceScreen.indexOfPreference(this)) as? PreferenceViewHolder)?.itemView

}


//class SettingsFragment : PreferenceFragmentCompat() {
//    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//        Log.d("SettingsFragment", "onCreatePreferences rootKey: $rootKey")
//
//        setPreferencesFromResource(R.xml.preferences, rootKey)
//
//        val savedUrlPref = findPreference<EditTextPreference>("saved_url")
//        Log.d("SettingsFragment", "savedUrlPref: $savedUrlPref")
//
//        val preferences = context?.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
//        Log.d("SettingsFragment", "preferences: $preferences")
//
//        var savedUrl = preferences?.getString(URL_KEY, "")
//
//        savedUrlPref?.let {
//            //val savedUrl = PreferenceManager.getDefaultSharedPreferences(requireContext())
//            //    .getString("saved_url", "")
//            Log.d("SettingsFragment", "savedUrl: $savedUrl")
//            it.text = savedUrl
//        }
//
//        savedUrlPref?.setOnPreferenceChangeListener { _, newValue ->
//            val newUrl = newValue as String
//            Log.d("SettingsFragment", "newUrl: $newUrl")
//            val url = parseUrl(newUrl)
//            Log.d("SettingsFragment", "url: $url")
//            if (url.isNullOrEmpty()) {
//                Log.d("SettingsFragment", "ERROR CHANGING URL!!")
//                Toast.makeText(context, "Invalid URL!", Toast.LENGTH_SHORT).show()
//                false
//            } else {
//                if (url == savedUrl) {
//                    Toast.makeText(context, "URL Not Changed!", Toast.LENGTH_SHORT).show()
//                    false
//                } else {
//                    preferences?.edit { putString(URL_KEY, url) }
//                    savedUrl = url
//                    Log.d("SettingsFragment", "URL CHANGED")
//                    true
//                }
//            }
//
//            //PreferenceManager.getDefaultSharedPreferences(requireContext())
//            //    .edit { putString("saved_url", newUrl) }
//
//        }
//    }
//
//    private fun parseUrl(urlString: String): String? {
//        var url = urlString.trim { it <= ' ' }
//        if (url.isEmpty()) {
//            Log.d("parseUrl", "url.isEmpty()")
//            return null
//        }
//        if (!url.lowercase().startsWith("http")) {
//            url = "https://$url"
//        }
//        if (url.endsWith("/")) {
//            url = url.substring(0, url.length - 1)
//        }
//        Log.d("parseUrl", "matching: $url")
//        //if (!Patterns.WEB_URL.matcher(url).matches()) {
//        //    Log.d("parseUrl", "Patterns.WEB_URL.matcher Failed")
//        //    return null
//        //}
//        return url
//    }
//}


//@Entity
//data class User(
//    @PrimaryKey val uid: Int,
//    @ColumnInfo(name = "first_name") val firstName: String?,
//    @ColumnInfo(name = "last_name") val lastName: String?
//)

@Entity
data class Server(
    @PrimaryKey val url: String,
    val token: String = "",
    val active: Boolean = false
)


@Dao
interface ServerDao {
    @Query("SELECT * FROM server")
    fun getAll(): List<Server>

    @Query("SELECT * FROM server WHERE active = 1 LIMIT 1")
    fun getActive(): Server?

    @Query("SELECT * FROM server WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): Server?

    @Insert
    fun add(server: Server)

    @Delete
    fun delete(server: Server)

    //@Insert
    //fun insertAll(vararg servers: Server)

//    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
//    fun loadAllByIds(userIds: IntArray): List<User>
//
//    @Query("SELECT * FROM server WHERE first_name LIKE :first AND last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User
}

@Database(entities = [Server::class], version = 1)
abstract class ServerDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
}
