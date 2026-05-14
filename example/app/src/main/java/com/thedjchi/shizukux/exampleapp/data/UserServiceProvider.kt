package com.thedjchi.shizukux.exampleapp.data

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.thedjchi.shizukux.exampleapp.BuildConfig
import com.thedjchi.shizukux.exampleapp.IUserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku

object UserServiceProvider {
    private val _userService = MutableStateFlow<IUserService?>(null)
    val isUserServiceBound = _userService.map { it != null }

    private val mutex = Mutex()

    private val args =
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("userService")
            .tag("UserService")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            if (binder == null || !binder.isBinderAlive) return
            _userService.value = IUserService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            _userService.value = null
        }
    }

    suspend fun getService(): IUserService = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _userService.value
            if (current != null && current.asBinder().isBinderAlive) {
                return@withLock current
            }

            Shizuku.bindUserService(args, connection)

            withTimeout(5_000) {
                _userService.filterNotNull().first()
            }
        }
    }
}