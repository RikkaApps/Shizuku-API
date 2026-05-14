package com.thedjchi.shizukux.exampleapp.models

data class MainUiState(
    val method: PrivilegedServiceMethod? = null,
    val serialNumber: String = "",
    val isShizukuRunning: Boolean = false,
    val isUserServiceBound: Boolean = false,
    val isPermissionGranted: Boolean = false
) {
    val serialTitleText: String
        get() = "Serial${
            if (method != null) " (${ method.name.replace("_", " ") })"
            else "" 
        }"

    val shizukuStatusText: String
        get() = "ShizukuX: ${
            if (isShizukuRunning) "Running"
            else "Not running"
        }"

    val permissionStatusText: String
        get() = "Permission: ${
            if (!isShizukuRunning) "Unknown"
            else if (isPermissionGranted) "Granted"
            else "Denied"
        }"

    val userServiceStatusText: String
        get() = "UserService: ${
            if (isUserServiceBound) "Bound"
            else "Not bound"
        }"
}
