package com.djangofiles.djangofiles.ui.files

import android.annotation.SuppressLint
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi.FileEditRequest
import com.djangofiles.djangofiles.ServerApi.FileResponse
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily

//import android.widget.ImageView
//import android.net.ConnectivityManager
//import androidx.fragment.app.FragmentActivity
//import androidx.navigation.fragment.FragmentNavigatorExtras
//import androidx.core.util.Pair as UtilPair

class FilesViewAdapter(
    private val context: Context,
    private val dataSet: MutableList<FileResponse>,
    private val isMetered: Boolean,
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
        val openMenu: LinearLayout = view.findViewById(R.id.open_menu)
        val loadingSpinner: ProgressBar = view.findViewById(R.id.loading_spinner)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.file_item_files, viewGroup, false)
        return ViewHolder(view)
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        //Log.d("UploadMultiAdapter", "position: $position")
        val data = dataSet[position]
        //Log.d("onBindViewHolder", "data[$position]: $data")

        // Setup
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

        // Variables
        val passParam = if (data.password.isNotEmpty()) "&password=${data.password}" else ""
        val viewUrl = "${data.raw}?view=gallery${passParam}"
        val thumbUrl = "${data.thumb}${passParam}"

        val bundle = Bundle().apply {
            putInt("fileId", data.id)
            putString("fileName", data.name)
            putString("mimeType", data.mime)
            putString("viewUrl", viewUrl)
            putString("thumbUrl", thumbUrl)
            putString("shareUrl", data.url)
            putString("rawUrl", data.raw)
            putString("filePassword", data.password)
            putBoolean("isPrivate", data.private)
        }
        //Log.d("FilesViewAdapter", "bundle: $bundle")

        // Menu Link
        viewHolder.openMenu.setOnClickListener {
            Log.d("FilesViewAdapter", "openMenu.setOnClickListener: $bundle")
            val bottomSheet = FilesBottomSheet.newInstance(bundle)
            bottomSheet.show(
                (context as FragmentActivity).supportFragmentManager,
                bottomSheet.tag
            )
        }

        // Preview - itemView Click
        viewHolder.itemView.setOnClickListener {
            // TODO: Setup proper transition/animation
            //val activity = context as Activity
            //val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            //    activity,
            //    UtilPair.create(viewHolder.fileImage, data.id.toString())
            //)
            //val extras = ActivityNavigatorExtras(options)
            //
            //val extras = FragmentNavigatorExtras(viewHolder.fileImage to data.id.toString())
            //it.findNavController()
            //    .navigate(R.id.nav_item_files_action_preview, bundle, null, extras)
            it.findNavController().navigate(R.id.nav_item_files_action_preview, bundle)
        }

        // Image - Holder
        val radius = context.resources.getDimension(R.dimen.image_preview_small)
        viewHolder.fileImage.setShapeAppearanceModel(
            viewHolder.fileImage.shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, radius)
                .build()
        )
        //viewHolder.fileImage.transitionName = data.id.toString()

        // Image - Glide Listener
        val glideListener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                //Log.d("Glide", "onLoadFailed: ${data.name}")
                viewHolder.loadingSpinner.visibility = View.GONE
                viewHolder.fileImage.setImageResource(getGenericIcon(data.mime))
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
                //viewHolder.fileImage.scaleType = ImageView.ScaleType.CENTER_CROP
                return false
            }
        }

        // Image - Logic
        if (isGlideMime(data.mime)) {
            viewHolder.loadingSpinner.visibility = View.VISIBLE
            //Log.d("Glide", "load: ${data.id}: ${data.mime}: $thumbUrl")

            Glide.with(viewHolder.itemView)
                .load(thumbUrl)
                .onlyRetrieveFromCache(isMetered)
                .listener(glideListener)
                .into(viewHolder.fileImage)

            //viewHolder.fileImage.transitionName = data.id.toString()
            //Log.d("FilesPreviewFragment", "transitionName: ${viewHolder.fileImage.transitionName}")

        } else {
            viewHolder.fileImage.setImageResource(getGenericIcon(data.mime))
            //viewHolder.fileImage.transitionName = null
            //viewHolder.previewLink.setOnClickListener { }
        }
    }

    override fun getItemCount() = dataSet.size

    fun addData(newData: List<FileResponse>) {
        val start = dataSet.size
        dataSet.addAll(newData)
        notifyItemRangeInserted(start, newData.size)
    }

    fun getData(): List<FileResponse> {
        return dataSet
    }

    fun deleteById(fileId: Int) {
        val index = dataSet.indexOfFirst { it.id == fileId }
        if (index != -1) {
            dataSet.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun editById(request: FileEditRequest) {
        Log.d("editById", "id: ${request.id}")
        Log.d("editById", "request: $request")
        val index = dataSet.indexOfFirst { it.id == request.id }
        Log.d("editById", "index: $index")
        if (index != -1) {
            val file = dataSet[index]
            if (request.private != null) {
                file.private = request.private
            }
            if (request.password != null) {
                file.password = request.password
            }
            notifyItemChanged(index)
        }
    }

    // Note: this has not been tested due to warning on notifyDataSetChanged
    //fun submitList(newList: List<FileResponse>) {
    //    Log.d("submitList", "newList.size: ${newList.size}")
    //    dataSet.clear()
    //    dataSet.addAll(newList)
    //    notifyDataSetChanged()
    //}

    private fun bytesToHuman(bytes: Double) = when {
        bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30))
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20))
        bytes >= 1 shl 10 -> "%.0f kB".format(bytes / (1 shl 10))
        else -> "$bytes b"
    }
}

fun isGlideMime(mimeType: String): Boolean {
    return when (mimeType.lowercase()) {
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "image/heif",
            -> true

        else -> false
    }
}

fun openUrl(context: Context, url: String) {
    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(url.toUri(), "text/plain")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(openIntent, null))
}

fun shareUrl(context: Context, url: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(shareIntent, null))
}

fun isCodeMime(mimeType: String): Boolean {
    if (mimeType.startsWith("text/x-script")) return true
    return when (mimeType.lowercase()) {
        "application/atom+xml",
        "application/javascript",
        "application/json",
        "application/ld+json",
        "application/rss+xml",
        "application/xml",
        "application/x-httpd-php",
        "application/x-python",
        "application/x-www-form-urlencoded",
        "application/yaml",
        "text/javascript",
        "text/python",
        "text/x-go",
        "text/x-ruby",
        "text/x-php",
        "text/x-python",
        "text/x-shellscript",
            -> true

        else -> false
    }
}

fun getGenericIcon(mimeType: String): Int = when {
    isCodeMime(mimeType) -> R.drawable.md_code_blocks_24
    mimeType.startsWith("application/json") -> R.drawable.md_file_json_24
    mimeType.startsWith("application/pdf") -> R.drawable.md_picture_as_pdf_24
    mimeType.startsWith("image/gif") -> R.drawable.md_gif_box_24
    mimeType.startsWith("image/png") -> R.drawable.md_file_png_24
    mimeType.startsWith("text/csv") -> R.drawable.md_csv_24
    mimeType.startsWith("audio/") -> R.drawable.md_music_note_24
    mimeType.startsWith("image/") -> R.drawable.md_imagesmode_24
    mimeType.startsWith("text/") -> R.drawable.md_docs_24
    mimeType.startsWith("video/") -> R.drawable.md_videocam_24
    else -> R.drawable.md_unknown_document_24
}
