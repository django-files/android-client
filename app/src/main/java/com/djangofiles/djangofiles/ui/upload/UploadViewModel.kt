package com.djangofiles.djangofiles.ui.upload

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UploadViewModel : ViewModel() {

    private var initialUriSet: Set<Uri> = emptySet()

    val selectedUris = MutableLiveData<Set<Uri>>(emptySet())

    fun setInitialUris(uris: List<Uri>) {
        val uriSet = uris.toSet()
        if (uriSet != initialUriSet) {
            initialUriSet = uriSet
            selectedUris.value = uriSet
        }
    }

    //fun isUriValid(uri: Uri): Boolean {
    //    return initialUriSet.contains(uri)
    //}

    //fun selectUri(uri: Uri) {
    //    if (!isUriValid(uri)) return
    //    selectedUris.value = selectedUris.value.orEmpty() + uri
    //}

    //fun deselectUri(uri: Uri) {
    //    selectedUris.value = selectedUris.value.orEmpty() - uri
    //}

    fun getSelectedUris(): Set<Uri> {
        return selectedUris.value.orEmpty()
    }
}
