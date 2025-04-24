package com.djangofiles.djangofiles.ui.files

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.ActivityNavigatorExtras
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi.RecentResponse
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import androidx.core.util.Pair as UtilPair

//import androidx.fragment.app.FragmentActivity
//import androidx.navigation.fragment.FragmentNavigatorExtras

class FilesViewAdapter(
    private val context: Context,
    private val dataSet: MutableList<RecentResponse>,
) : RecyclerView.Adapter<FilesViewAdapter.ViewHolder>() {

    private var colorOnSecondary: ColorStateList? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileImage: ShapeableImageView = view.findViewById(R.id.file_image)
        val fileName: TextView = view.findViewById(R.id.file_name)
        val fileSize: TextView = view.findViewById(R.id.file_size)
        val fileView: TextView = view.findViewById(R.id.file_view)
        val filePrivate: TextView = view.findViewById(R.id.file_private)
        val filePassword: TextView = view.findViewById(R.id.file_password)
        val fileExpr: TextView = view.findViewById(R.id.file_expr)
        val previewLink: LinearLayout = view.findViewById(R.id.preview_link)
        val shareLink: LinearLayout = view.findViewById(R.id.share_link)
        val openLink: LinearLayout = view.findViewById(R.id.open_link)
        val loadingSpinner: ProgressBar = view.findViewById(R.id.loading_spinner)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.file_item_files, viewGroup, false)
        return ViewHolder(view)
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val data = dataSet[position]
        //Log.i("onBindViewHolder", "data[$position]: $data")
        //Log.d("onBindViewHolder", "mime[$position]: ${data.mime}")

        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorOnSecondary,
            typedValue,
            true
        )
        colorOnSecondary = ContextCompat.getColorStateList(context, typedValue.resourceId)

        // Name
        viewHolder.fileName.text = data.name

        // Size
        viewHolder.fileSize.text = bytesToHuman(data.size.toDouble()).toString()

        // Views
        viewHolder.fileView.text = data.view.toString()
        viewHolder.fileView.compoundDrawableTintList = if (data.view > 0) null else colorOnSecondary

        // Private
        viewHolder.filePrivate.compoundDrawableTintList =
            if (data.private) null else colorOnSecondary

        // Password
        viewHolder.filePassword.compoundDrawableTintList =
            if (data.password.isNotEmpty()) null else colorOnSecondary

        // Expiration
        viewHolder.fileExpr.text = data.expr
        viewHolder.fileExpr.compoundDrawableTintList =
            if (data.expr.isNotEmpty()) null else colorOnSecondary

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
        val radius = context.resources.getDimension(R.dimen.image_preview_small)
        viewHolder.fileImage.setShapeAppearanceModel(
            viewHolder.fileImage.shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, radius)
                .build()
        )

        val setGenericIcon = {
            when {
                isCodeMime(data.mime) -> viewHolder.fileImage.setImageResource(R.drawable.md_code_blocks_24)
                data.mime.startsWith("application/json") -> viewHolder.fileImage.setImageResource(R.drawable.md_file_json_24)
                data.mime.startsWith("application/pdf") -> viewHolder.fileImage.setImageResource(R.drawable.md_picture_as_pdf_24)
                data.mime.startsWith("image/gif") -> viewHolder.fileImage.setImageResource(R.drawable.md_gif_box_24)
                data.mime.startsWith("image/png") -> viewHolder.fileImage.setImageResource(R.drawable.md_file_png_24)
                data.mime.startsWith("text/csv") -> viewHolder.fileImage.setImageResource(R.drawable.md_csv_24)
                data.mime.startsWith("audio/") -> viewHolder.fileImage.setImageResource(R.drawable.md_music_note_24)
                data.mime.startsWith("image/") -> viewHolder.fileImage.setImageResource(R.drawable.md_imagesmode_24)
                data.mime.startsWith("text/") -> viewHolder.fileImage.setImageResource(R.drawable.md_docs_24)
                data.mime.startsWith("video/") -> viewHolder.fileImage.setImageResource(R.drawable.md_videocam_24)
                else -> viewHolder.fileImage.setImageResource(R.drawable.md_unknown_document_24)
            }
        }

        val glideListener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                //Log.d("Glide", "onLoadFailed: ${data.name}")
                viewHolder.loadingSpinner.visibility = View.GONE
                setGenericIcon()
                return true
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                //Log.d("Glide", "onResourceReady: ${data.name}")
                viewHolder.loadingSpinner.visibility = View.GONE
                viewHolder.fileImage.scaleType = ImageView.ScaleType.CENTER_CROP
                return false
            }
        }

        viewHolder.fileImage.scaleType = ImageView.ScaleType.CENTER_INSIDE

        if (isGlideMime(data.mime)) {
            viewHolder.loadingSpinner.visibility = View.VISIBLE
            val viewUrl = "${data.raw}?view=gallery"
            val thumbUrl =
                data.thumb + if (data.password.isNotEmpty()) "&password=${data.password}" else ""
            //Log.d("Glide", "load: ${data.id}: ${data.mime}: $thumbUrl")
            Glide.with(viewHolder.itemView)
                .load(thumbUrl)
                .listener(glideListener)
                .into(viewHolder.fileImage)

            viewHolder.fileImage.transitionName = data.id.toString()

            viewHolder.previewLink.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("viewUrl", viewUrl)
                    putString("thumbUrl", thumbUrl)
                    putInt("fileId", data.id)
                }
                //val extras = FragmentNavigatorExtras(viewHolder.fileImage to data.id.toString())

                val activity = context as Activity
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    UtilPair.create(viewHolder.fileImage, data.id.toString())
                )
                val extras = ActivityNavigatorExtras(options)

                it.findNavController()
                    .navigate(R.id.nav_item_files_action_preview, bundle, null, extras)
            }

            // TODO: Refactor the BottomBitch as a context menu...
            //viewHolder.fileImage.setOnClickListener {
            //    val bottomSheet = FilesBottomSheet.newInstance(thumbUrl)
            //    bottomSheet.show(
            //        (context as FragmentActivity).supportFragmentManager,
            //        bottomSheet.tag
            //    )
            //}
        } else {
            setGenericIcon()
            viewHolder.fileImage.transitionName = null
            viewHolder.previewLink.setOnClickListener {  }
        }
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

    private fun isGlideMime(mimeType: String): Boolean {
        return when (mimeType.lowercase()) {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/heif",
            "video/mp4",
                -> true

            else -> false
        }
    }

    private fun isCodeMime(mimeType: String): Boolean {
        if (mimeType.startsWith("text/x-script")) return true
        return when (mimeType.lowercase()) {
            "application/javascript",
            "application/x-httpd-php",
            "application/x-python",
            "text/javascript",
            "text/python",
            "text/x-go",
            "text/x-ruby",
            "text/x-php",
            "text/x-shellscript",
                -> true

            else -> false
        }
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
