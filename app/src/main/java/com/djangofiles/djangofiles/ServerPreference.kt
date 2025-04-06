package com.djangofiles.djangofiles

import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import com.djangofiles.djangofiles.settings.SettingsFragment.ServerEntry

class ServerPreference(
    context: Context,
    private val index: Int,
    private val entry: ServerEntry,
    private val onEdit: (Int, ServerEntry) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val savedUrl: String? = null
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

        deleteButton?.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark), PorterDuff.Mode.SRC_IN)

        titleView?.text = entry.url

        Log.d("ServerPreference", "entryurl: ${entry.url}")
        Log.d("ServerPreference", "savedUrl: $savedUrl")
        if (entry.url == savedUrl) {
            val attrs = intArrayOf(android.R.attr.colorControlHighlight)
            val typedArray = context.obtainStyledAttributes(attrs)
            val highlight = typedArray.getColor(0, 0)
            typedArray.recycle()
            holder.itemView.setBackgroundColor(highlight)
        }

        deleteButton?.setOnClickListener {
            onDelete(index)
        }

        holder.itemView.setOnClickListener {
            onEdit(index, entry)
        }
    }
}
