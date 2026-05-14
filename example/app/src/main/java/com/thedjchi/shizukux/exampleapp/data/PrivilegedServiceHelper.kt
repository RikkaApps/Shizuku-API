package com.thedjchi.shizukux.exampleapp.data

import android.content.pm.PackageManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import rikka.shizuku.Shizuku

object PrivilegedServiceHelper {

    val isRunning = callbackFlow {
        val receivedListener = Shizuku.OnBinderReceivedListener { trySend(true) }
        val deadListener = Shizuku.OnBinderDeadListener { trySend(false) }

        Shizuku.addBinderReceivedListener(receivedListener)
        Shizuku.addBinderDeadListener(deadListener)

        trySend(Shizuku.pingBinder())

        awaitClose {
            Shizuku.removeBinderReceivedListener(receivedListener)
            Shizuku.removeBinderDeadListener(deadListener)
        }
    }

    val permissionResult = callbackFlow {
        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, result ->
            trySend(requestCode to (result == PackageManager.PERMISSION_GRANTED))
        }

        Shizuku.addRequestPermissionResultListener(listener)

        awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    val isPermissionGranted = permissionResult
        .map { it.second }
        .onStart { emit(checkPermission()) }

    fun checkPermission(requestCode: Int? = null): Boolean {
        if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return false

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        requestCode?.let { Shizuku.requestPermission(it) }
        return false
    }

}
