package com.djangofiles.djangofiles.ui.files

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.djangofiles.djangofiles.ServerApi.FileEditRequest
import com.djangofiles.djangofiles.ServerApi.FileResponse

class FilesViewModel : ViewModel() {

    val viewKey = MutableLiveData<String>()
    val filesData = MutableLiveData<List<FileResponse>>()
    val atEnd = MutableLiveData<Boolean>()
    val deleteId = MutableLiveData<Int>()
    val editRequest = MutableLiveData<FileEditRequest>()

    val meterHidden = MutableLiveData<Boolean>().apply { value = false }
    val selected = MutableLiveData<MutableSet<Int>>()

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
