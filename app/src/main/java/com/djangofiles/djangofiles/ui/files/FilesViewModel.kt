package com.djangofiles.djangofiles.ui.files

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.djangofiles.djangofiles.ServerApi.RecentResponse

class FilesViewModel : ViewModel() {
    val savedUrl = MutableLiveData<String>()
    val filesData = MutableLiveData<List<RecentResponse>>()
    val atEnd = MutableLiveData<Boolean>()
}
