package com.djangofiles.djangofiles.ui.upload

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UploadViewModel : ViewModel() {
    val selectedUris = MutableLiveData<Set<Uri>>()
}
