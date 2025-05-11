package com.djangofiles.djangofiles.ui.files

import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerApi.FilesEditRequest
import com.djangofiles.djangofiles.db.AlbumEntity
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

        val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Choose Albums")
            .setIcon(R.drawable.md_imagesmode_24)
            .setMultiChoiceItems(albumNames, selected) { _, which, isChecked ->
                selected[which] = isChecked
            }
            .setPositiveButton("Set") { _, _ ->
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
            .setNegativeButton("Cancel") { _, _ -> }
            .create()

        return dialog
    }

//    // The activity that creates an instance of this dialog fragment must
//    // implement this interface to receive event callbacks. Each method passes
//    // the DialogFragment in case the host needs to query it.
//    interface NoticeDialogListener {
//        fun onDialogPositiveClick(dialog: DialogFragment)
//        fun onDialogNegativeClick(dialog: DialogFragment)
//    }
//
//    // Override the Fragment.onAttach() method to instantiate the
//    // NoticeDialogListener.
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        // Verify that the host activity implements the callback interface.
//        try {
//            // Instantiate the NoticeDialogListener so you can send events to
//            // the host.
//            listener = context as NoticeDialogListener
//        } catch (e: ClassCastException) {
//            // The activity doesn't implement the interface. Throw exception.
//            throw ClassCastException((context.toString() +
//                    " must implement NoticeDialogListener"))
//        }
//    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val sharedPreferences =
//            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
//        val savedUrl = sharedPreferences.getString("saved_url", "").orEmpty()
//        val dao = AlbumDatabase.getInstance(requireContext(), savedUrl).albumDao()
//        val api = ServerApi(requireContext(), savedUrl)
//
//        lifecycleScope.launch {
//            val albums = withContext(Dispatchers.IO) { dao.getAll() }
//            val selected = BooleanArray(albums.size) { i -> albums[i].id in selectedIds }
//            val albumNames = albums.map { it.name }.toTypedArray()
//            AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
//                .setTitle("Choose Albums")
//                .setIcon(R.drawable.md_imagesmode_24)
//                .setMultiChoiceItems(albumNames, selected) { _, which, isChecked ->
//                    selected[which] = isChecked
//                }
//                .setPositiveButton("Set") { _, _ ->
//                    val selectedIds = albums.mapIndexedNotNull { i, album ->
//                        if (selected[i]) album.id else null
//                    }
//                    Log.d("AlbumFragment", "Selected IDs: $selectedIds")
//                    val request = FilesEditRequest(ids = fileIds, albums = selectedIds)
//                    CoroutineScope(Dispatchers.IO).launch { api.filesEdit(request) }
//                    //listener.onDialogPositiveClick(this@AlbumFragment)
//                    setFragmentResult("albums_result", bundleOf("albums" to selectedIds))
//                    dismiss()
//                }
//                //.setNegativeButton("Cancel", null)
//                .setNegativeButton("Cancel", { _, _ ->
//                    dismiss()
//                })
//                //.setNegativeButton("Cancel",
//                //    DialogInterface.OnClickListener { dialog, id ->
//                //        listener.onDialogNegativeClick(this@AlbumFragment)
//                //    })
//                .show()
//        }
//    }
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return AlertDialog.Builder(requireContext()).create()
//    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val sharedPreferences =
//            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
//        val savedUrl = sharedPreferences.getString("saved_url", "").orEmpty()
//        val dao = AlbumDatabase.getInstance(requireContext(), savedUrl).albumDao()
//        val api = ServerApi(requireContext(), savedUrl)
//
//        val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
//            .setTitle("Choose Albums")
//            .setIcon(R.drawable.md_imagesmode_24)
//            .setMultiChoiceItems(arrayOf(), null, null)
//            .setPositiveButton("Set", null)
//            .setNegativeButton("Cancel", null)
//            .create()
//
//        lifecycleScope.launch {
//            val albums = withContext(Dispatchers.IO) { dao.getAll() }
//            val selected = BooleanArray(albums.size) { i -> albums[i].id in selectedIds }
//            Log.d("Album[onCreateDialog]", "selected: $selected")
//            val albumNames = albums.map { it.name }.toTypedArray()
//
//            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Set") { _, _ ->
//                val selectedIds = albums.mapIndexedNotNull { i, album ->
//                    if (selected[i]) album.id else null
//                }
//                val request = FilesEditRequest(ids = fileIds, albums = selectedIds)
//                CoroutineScope(Dispatchers.IO).launch { api.filesEdit(request) }
//                Log.d("Album[onCreateDialog]", "selectedIds: $selectedIds")
//                setFragmentResult("albums_result", bundleOf("albums" to selectedIds))
//                dismiss()
//            }
//
//            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> }
//
//            dialog.listView?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, albumNames)
//            for (i in selected.indices) dialog.listView.setItemChecked(i, selected[i])
//        }
//
//        return dialog
//    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val sharedPreferences =
//            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
//        val savedUrl = sharedPreferences.getString("saved_url", "").orEmpty()
//        val dao = AlbumDatabase.getInstance(requireContext(), savedUrl).albumDao()
//        val api = ServerApi(requireContext(), savedUrl)
//
//        val dialog = Dialog(requireContext())
//
//        lifecycleScope.launch {
//            val albums = withContext(Dispatchers.IO) { dao.getAll() }
//            val selected = BooleanArray(albums.size) { i -> albums[i].id in selectedIds }
//            val albumNames = albums.map { it.name }.toTypedArray()
//
//            AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
//                .setTitle("Choose Albums")
//                .setIcon(R.drawable.md_imagesmode_24)
//                .setMultiChoiceItems(albumNames, selected) { _, which, isChecked ->
//                    selected[which] = isChecked
//                }
//                .setPositiveButton("Set") { _, _ ->
//                    val selectedIds = albums.mapIndexedNotNull { i, album ->
//                        if (selected[i]) album.id else null
//                    }
//                    val request = FilesEditRequest(ids = fileIds, albums = selectedIds)
//                    CoroutineScope(Dispatchers.IO).launch { api.filesEdit(request) }
//                    Log.d("Album[onCreateDialog]", "selectedIds: $selectedIds")
//                    setFragmentResult("albums_result", bundleOf("albums" to selectedIds))
//                    dismiss()
//                }
//                .setNegativeButton("Cancel", null)
//                .show()
//        }
//
//        return dialog
//    }

}
