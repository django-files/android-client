package com.djangofiles.djangofiles.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.djangofiles.djangofiles.ServerApi

//import androidx.databinding.Bindable

class SettingsViewModel : ViewModel() {
    val hostname = MutableLiveData<String>()
    val siteName = MutableLiveData<String>()
    val authMethods = MutableLiveData<List<ServerApi.Methods>>()

    //@Bindable
    //fun getHostname(): String {
    //    return hostname.value ?: ""
    //}
    //
    //fun setHostname(value: String) {
    //    if (hostname.value != value) {
    //        hostname.value = value
    //    }
    //}
}
