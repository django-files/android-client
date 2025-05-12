package com.djangofiles.djangofiles.ui.files

import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerApi.FilesEditRequest
import com.djangofiles.djangofiles.db.AlbumEntity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlbumFragment : DialogFragment() {

    private var albums: List<AlbumEntity> = emptyList()
    private var fileIds: List<Int> = emptyList()
    private var selectedIds: List<Int> = emptyList()

    fun setAlbumData(albums: List<AlbumEntity>, fileIds: List<Int>, selectedIds: List<Int>) {
        this.albums = albums
        this.fileIds = fileIds
        this.selectedIds = selectedIds
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", "").orEmpty()
        val api = ServerApi(requireContext(), savedUrl)

        val selected = BooleanArray(albums.size) { i -> albums[i].id in selectedIds }
        Log.d("dialog[lifecycleScope]", "selected: $selected")
        val albumNames = albums.map { it.name }.toTypedArray()

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Set Albums")
            .setIcon(R.drawable.md_imagesmode_24)
            .setNegativeButton("Cancel") { _, _ -> }
        if (albums.isNotEmpty()) {
            dialog.setMultiChoiceItems(albumNames, selected) { _, which, isChecked ->
                selected[which] = isChecked
            }
            dialog.setPositiveButton("Save") { _, _ ->
                val selectedIds = albums.mapIndexedNotNull { i, album ->
                    if (selected[i]) album.id else null
                }
                Log.d("dialog[setButton]", "selectedIds: $selectedIds")
                val request = FilesEditRequest(ids = fileIds, albums = selectedIds)
                CoroutineScope(Dispatchers.IO).launch { api.filesEdit(request) }
                Log.d("dialog[setButton]", "selectedIds: $selectedIds")
                setFragmentResult("albums_result", bundleOf("albums" to selectedIds))
                dismiss()
            }
        } else {
            dialog.setMessage("No Albums Found.")
        }

        return dialog.create()
    }
}
