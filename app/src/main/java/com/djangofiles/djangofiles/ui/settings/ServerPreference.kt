package com.djangofiles.djangofiles.ui.settings

import android.content.Context
import android.graphics.PorterDuff
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.djangofiles.djangofiles.R

class ServerPreference(
    context: Context,
    private val server: Server,
    private val onEdit: (Server) -> Unit,
    private val onDelete: (Server) -> Unit,
    private val savedUrl: String? = null
) : Preference(context) {

    init {
        layoutResource = R.layout.pref_server_item
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        val titleView = holder.findViewById(android.R.id.title) as? TextView
        val deleteButton = holder.findViewById(R.id.delete_button) as? ImageView

        deleteButton?.setColorFilter(
            ContextCompat.getColor(context, android.R.color.holo_red_dark),
            PorterDuff.Mode.SRC_IN
        )

        titleView?.text = server.url

        if (server.url == savedUrl) {
            val attrs = intArrayOf(android.R.attr.colorControlHighlight)
            val typedArray = context.obtainStyledAttributes(attrs)
            val highlight = typedArray.getColor(0, 0)
            typedArray.recycle()
            holder.itemView.setBackgroundColor(highlight)
        }

        deleteButton?.setOnClickListener {
            onDelete(server)
        }

        holder.itemView.setOnClickListener {
            onEdit(server)
        }
    }
}
