package com.djangofiles.djangofiles

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import com.djangofiles.djangofiles.settings.SettingsFragment.ServerEntry

class ServerPreference(
    context: Context,
    private val index: Int,
    private val entry: ServerEntry,
    private val onEdit: (Int, ServerEntry) -> Unit,
    private val onDelete: (Int) -> Unit
) : Preference(context) {

    init {
        layoutResource = R.layout.pref_server_item
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
//        super.onBindViewHolder(holder)
//        val themedContext = ContextThemeWrapper(context, R.style.Theme_djangofiles)
//        LayoutInflater.from(themedContext).inflate(layoutResource, holder.itemView as ViewGroup, true)
        Log.d("ServerPreference", "entry: $entry")
        val titleView = holder.findViewById(android.R.id.title) as? TextView
        Log.d("ServerPreference", "titleView: $titleView")
        val deleteButton = holder.findViewById(R.id.delete_button) as? ImageView

        titleView?.text = entry.url
        Log.d("ServerPreference", "entry.url: ${entry.url}")
        deleteButton?.setOnClickListener {
            onDelete(index)
        }

        holder.itemView.setOnClickListener {
            onEdit(index, entry)
        }
    }
}
