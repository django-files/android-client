package com.djangofiles.djangofiles.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.api.ServerApi.RecentResponse

class FilesViewAdapter(
    private val context: Context,
    private val dataSet: MutableList<RecentResponse>
) : RecyclerView.Adapter<FilesViewAdapter.ViewHolder>() {

    private var colorOnSecondary: ColorStateList? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileImage: ImageView = view.findViewById(R.id.file_image)
        val fileName: TextView = view.findViewById(R.id.file_name)
        val fileSize: TextView = view.findViewById(R.id.file_size)
        val fileView: TextView = view.findViewById(R.id.file_view)
        val filePrivate: TextView = view.findViewById(R.id.file_private)
        val filePassword: TextView = view.findViewById(R.id.file_password)
        val fileExpr: TextView = view.findViewById(R.id.file_expr)
        val shareLink: LinearLayout = view.findViewById(R.id.share_link)
        val openLink: LinearLayout = view.findViewById(R.id.open_link)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.file_item_files, viewGroup, false)

        // Set MD Color
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorOnSecondary,
            typedValue,
            true
        )
        colorOnSecondary = ContextCompat.getColorStateList(context, typedValue.resourceId)

        return ViewHolder(view)
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val data = dataSet[position]
        //Log.i("onBindViewHolder", "data[$position]: $data")
        //Log.d("onBindViewHolder", "mime[$position]: ${data.mime}")

        // Name
        viewHolder.fileName.text = data.name

        // Size
        viewHolder.fileSize.text = bytesToHuman(data.size.toDouble()).toString()

        // Views
        viewHolder.fileView.text = data.view.toString()
        if (data.view > 0) {
            viewHolder.fileView.compoundDrawableTintList = null
        } else {
            viewHolder.fileView.compoundDrawableTintList = colorOnSecondary
        }

        // Private
        if (data.private) {
            viewHolder.filePrivate.compoundDrawableTintList = null
        } else {
            viewHolder.filePrivate.compoundDrawableTintList = colorOnSecondary
        }

        // Password
        if (!data.password.isEmpty()) {
            viewHolder.filePassword.compoundDrawableTintList = null
        } else {
            viewHolder.filePassword.compoundDrawableTintList = colorOnSecondary
        }

        // Expiration
        viewHolder.fileExpr.text = data.expr
        if (!data.expr.isEmpty()) {
            viewHolder.fileExpr.compoundDrawableTintList = null
        } else {
            viewHolder.fileExpr.compoundDrawableTintList = colorOnSecondary
        }

        // Share Link
        viewHolder.shareLink.setOnClickListener {
            Log.d("FilesViewAdapter", "shareLink.setOnClickListener: $data")
            shareUrl(data.url)
        }

        // Open Link
        viewHolder.openLink.setOnClickListener {
            Log.d("FilesViewAdapter", "openLink.setOnClickListener: $data")
            openUrl(data.url)
        }

        // Image
        if (data.mime.startsWith("application/pdf") == true) {
            viewHolder.fileImage.setImageResource(R.drawable.md_picture_as_pdf_24)
        } else if (data.mime.startsWith("text/csv") == true) {
            viewHolder.fileImage.setImageResource(R.drawable.md_csv_24)
        } else if (data.mime.startsWith("text/") == true) {
            viewHolder.fileImage.setImageResource(R.drawable.md_docs_24)
        } else if (data.mime.startsWith("image/") == true) {
            viewHolder.fileImage.setImageResource(R.drawable.md_imagesmode_24)
        } else if (data.mime.startsWith("video/") == true) {
            viewHolder.fileImage.setImageResource(R.drawable.md_videocam_24)
        } else if (data.mime.startsWith("audio/") == true) {
            viewHolder.fileImage.setImageResource(R.drawable.md_music_note_24)
        } else {
            viewHolder.fileImage.setImageResource(R.drawable.md_unknown_document_24)
        }

        //// TODO: TESTING ONLY
        //if (data.mime.startsWith("image/")) {
        //    Log.i("testOnly", "data.mime: ${data.mime}")
        //    try {
        //        val galleryUrl = URL("${data.raw}?view=gallery")
        //        Log.i("testOnly", "galleryUrl: $galleryUrl")
        //        CoroutineScope(Dispatchers.IO).launch {
        //            val drawable = Drawable.createFromStream(galleryUrl.openStream(), "src")
        //            CoroutineScope(Dispatchers.Main).launch {
        //                viewHolder.fileImage.setImageDrawable(drawable)
        //            }
        //        }
        //    } catch (e: Exception) {
        //        Log.i("testOnly", "Exception: $e")
        //    }
        //}
    }

    override fun getItemCount() = dataSet.size

    fun addData(newData: List<RecentResponse>) {
        val start = dataSet.size
        dataSet.addAll(newData)
        notifyItemRangeInserted(start, newData.size)
    }

    fun getData(): List<RecentResponse> {
        return dataSet
    }

    private fun openUrl(url: String) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(url.toUri(), "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(openIntent, null))
    }

    private fun shareUrl(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    private fun bytesToHuman(bytes: Double) = when {
        bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30))
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20))
        bytes >= 1 shl 10 -> "%.0f kB".format(bytes / (1 shl 10))
        else -> "$bytes b"
    }
}
