package com.thedjchi.shizukux.exampleapp.ui

import android.os.IDeviceIdentifiersPolicyService
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thedjchi.shizukux.exampleapp.data.PrivilegedServiceHelper
import com.thedjchi.shizukux.exampleapp.data.UserServiceProvider
import com.thedjchi.shizukux.exampleapp.models.MainUiState
import com.thedjchi.shizukux.exampleapp.models.PrivilegedServiceMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class MainViewModel : ViewModel() {


    private val _method = MutableStateFlow<PrivilegedServiceMethod?>(null)
    private val _serialNumber = MutableStateFlow("...")

    val uiState = combine(
        _method,
        _serialNumber,
        PrivilegedServiceHelper.isRunning,
        UserServiceProvider.isUserServiceBound,
        PrivilegedServiceHelper.isPermissionGranted
    ) { method, serial, running, bound, granted ->
        MainUiState(method, serial, running, bound, granted)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainUiState()
    )

    private val deviceIdentifiersPolicyService by lazy {
        IDeviceIdentifiersPolicyService.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("device_identifiers"))
        )
    }

    init {
        PrivilegedServiceHelper.permissionResult.onEach { (requestCode, granted) ->
            if (granted) {
                runAction(requestCode)
            }
        }.launchIn(viewModelScope)
    }

    fun onBinderWrapperClicked() {
        runIfPermissionGranted(PrivilegedServiceMethod.BINDER_WRAPPER.ordinal)
    }

    fun onUserServiceClicked() {
        runIfPermissionGranted(PrivilegedServiceMethod.USER_SERVICE.ordinal)
    }

    private fun runIfPermissionGranted(requestCode: Int) {
        if (PrivilegedServiceHelper.checkPermission(requestCode)) {
            runAction(requestCode)
        }
    }

    private fun runAction(requestCode: Int) {
        val method = PrivilegedServiceMethod.entries.find { it.ordinal == requestCode }
            ?: throw IllegalStateException()
        getSerial(method)
    }

    private fun getSerial(method: PrivilegedServiceMethod) {
        viewModelScope.launch {
            _serialNumber.value = "..."
            _method.value = method

            _serialNumber.value = runCatching {
                when (method) {
                    PrivilegedServiceMethod.BINDER_WRAPPER ->
                        deviceIdentifiersPolicyService.getSerial()

                    PrivilegedServiceMethod.USER_SERVICE ->
                        UserServiceProvider.getService().getSerial()
                }
            }.getOrElse {
                Log.e("MainViewModel", "getSerial", it)
                "Error: ${it.message}"
            }
        }
    }

}
