package com.djangofiles.djangofiles.ui.files

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.djangofiles.djangofiles.ServerApi.RecentResponse

class FilesViewModel : ViewModel() {

    val savedUrl = MutableLiveData<String>()
    val filesData = MutableLiveData<List<RecentResponse>>()
    val atEnd = MutableLiveData<Boolean>()
    val deleteId = MutableLiveData<Int>()

    //// Note: this will not work without a filesData observer to update data on changes
    //fun deleteById(fileId: Int) {
    //    Log.d("deleteById", "fileId: $fileId")
    //    val currentList = filesData.value.orEmpty()
    //    Log.d("deleteById", "currentList: ${currentList.size}")
    //    val updatedList = currentList.filter { it.id != fileId }
    //    Log.d("deleteById", "updatedList: ${updatedList.size}")
    //    filesData.value = updatedList
    //}
}
