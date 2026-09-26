package com.djangofiles.djangofiles.ui.settings

import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.db.Server

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
        val highlightView = holder.findViewById(R.id.server_highlight)

        deleteButton?.setColorFilter(
            ContextCompat.getColor(context, android.R.color.holo_red_dark),
            PorterDuff.Mode.SRC_IN
        )

        titleView?.text = server.url

        // Every ServerPreference shares one RecyclerView view type, so a holder that last
        // bound the active server gets recycled onto the other rows. Toggle a child view
        // instead of the item background, which is never restored once overwritten.
        highlightView?.visibility = if (server.url == savedUrl) View.VISIBLE else View.GONE

        deleteButton?.setOnClickListener {
            onDelete(server)
        }

        holder.itemView.setOnClickListener {
            onEdit(server)
        }
    }
}
