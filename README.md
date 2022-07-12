# Shizuku-API

Shizuku API is the API provided by [Shizuku](https://github.com/RikkaApps/Shizuku) and [Sui](https://github.com/RikkaApps/Sui). With Shizuku API, your app will be able to use Android APIs (almost) directly with Java or Kotlin, and as the identity of root or shell (adb).

Shizuku and Sui share the API design. As the application developer, you only need to write the code once to support both Shizuku and Sui.

**NOTE:** There is no such a simple way to "use Shizuku as `su`" (Yep, you can still achieve this by yourself if you want). It is recommended to implement your application with Shizuku in the correct way.

As you can already use Shizuku to execute any codes with ADB permission (or ROOT permission when using Sui), we won't waste our time providing a method to "use Shizuku as `su`".

> shizuku is a tool for professional developers that provides comprehensive access to the Android framework. If you only use command line, we don't recommend you to use shizuku, you can choose libsu.
> — @vvb2060 [Shizuku#229](https://github.com/RikkaApps/Shizuku/issues/229#issuecomment-1179687217)

## What Shizuku/Sui can do

Shizuku can be started with ADB or ROOT, Sui is a Magisk module (started by Magisk), so the privilege could be ADB or ROOT. If the user starts Shizuku with ADB, what you can do is only what ADB can do. You can use `Shizuku#getUid()` to check this, for ROOT it returns `0`, for ADB is `2000`.

What ADB can do is significantly different from ROOT. 

In the Android world, the privilege is determined by Android permissions. See [AndroidManifest of Shell](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/packages/Shell/AndroidManifest.xml), all the permission granted to Shell (ADB) are listed here. Be aware, the permission changes under different Android versions.

In Linux world, the privilege is determined by Shell's uid, capabilities, SELinux context, etc. For example, Shell cannot access other apps' data files `/data/user/0/<package>`, one of the reasons is Shell has no permission to enter (search) `/data/user/0` folder (0771, UID 1000 GID 1000).

## Requirements

To use Shizuku APIs, you need to guide the user to install Shizuku or Sui first. Both of them require Android 6.0+.

### Shizuku

Shizuku is a standard Android application. You can guide the user to download Shizuku from https://shizuku.rikka.app/download/. Shizuku works for both rooted and unrooted devices.

On unrooted devices, Shizuku needs to manually restart with adb every time on boot. Before Android 11, a computer is required to run adb. Android 11 and above have built-in wireless debugging support, user can start Shizuku directly on the device.

### Sui

Sui is a [Magisk](https://github.com/topjohnwu/Magisk) module. Magisk requires an unlocked bootloader.

No additional setup steps are required except for the installation. You can guide rooted users (searching `su` in `PATH` is enough) to download Sui from Magisk or https://github.com/RikkaApps/Sui.

## Demo

A demo project is provided. See [demo](https://github.com/RikkaApps/Shizuku-API/tree/master/demo) for more.

## Guide

I'll say the difficult words first, using Shizuku APIs is similar to framework or system app development, some experience in developing common applications may not be applicable. You have to get used to digging into Android source code to find out how things work, [cs.android.com](https://cs.android.com) and AndroidXref sites will be your best friend.

### Add dependency

![Maven Central](https://img.shields.io/maven-central/v/dev.rikka.shizuku/api)

```groovy
def shizuku_version = '12.1.0'
implementation "dev.rikka.shizuku:api:$shizuku_version"

// Add this line if you want to support Shizuku
implementation "dev.rikka.shizuku:provider:$shizuku_version"
```

### Acquire the Binder

Before using Shizuku APIs, you need to acquire the Binder from Shizuku or Sui.

`Shizuku` provides listeners, `Shizuku#addBinderReceivedListener()` and `Shizuku.addBinderDeadListener()`, that allows you to track the life of the binder. You should call methods in `Shizuku` class when the binder is alive or you will get an `IllegalStateException`.

#### Sui

Call `Sui.init(packageName)` before using `Shizuku` class. This method only needs to be called once. If this method returns true, means Sui is installed and available.

For multi-process applications, call this method in every process that needs to use Shizuku API.

Note, request the binder for Sui only requires two times of binder IPC, this is significantly cheaper than initialize Shizuku which uses content provider. `Sui.init(packageName)` can be used in main thread, you don't need to worry about performance.

#### Shizuku

Add `ShizukuProvider` to `AndroidManifest.xml`.

```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:multiprocess="false"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />

<!-- android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" is to protect this provider from accessing by normal apps -->
```

For multi-process applications, you need to call `ShizukuProvider.enableMultiProcessSupport()` in every process which needs to use Shizuku API.

Starting from v12.1.0, Sui is initialized automatically in `ShizukuProvider`. You can opt-out this behavior by calling `ShizukuProvider#disableAutomaticSuiInitialization()` before `ShizukuProvider#onCreate()` is called. Note, request the binder for Sui only requires two times of binder IPC, this is significantly cheaper than initialize Shizuku which uses content provider. Unless there are special reasons, apps that support Shizuku should also support Sui, otherwise it will cause user confusion.

### Request permission

Requesting permission is similar to [requesting runtime permissions](https://developer.android.com/training/permissions/requesting).

A simple example of requesting permission:

```java
private void onRequestPermissionsResult(int requestCode, int grantResult) {
    boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
    // Do stuff based on the result and the request code
}
```

```java
private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

@Override
protected void onCreate(Bundle savedInstanceState) {
    // ...
    Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    // ...
}

@Override
protected void onDestroy() {
    // ...
    Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENE;
    // ...
}

private boolean checkPermission(int code) {
  if (Shizuku.isPreV11()) {
    // Pre-v11 is unsupported
    return false;
  }

  if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
    // Granted
    return true;
  } else if (Shizuku.shouldShowRequestPermissionRationale()) {
    // Users choose "Deny and don't ask again"
    return false;
  } else {
    // Request the permission
    Shizuku.requestPermission(code);
    return false;
  }
}
```

### Compile with hidden APIs

[HiddenApiRefinePlugin](https://github.com/RikkaApps/HiddenApiRefinePlugin)

### Using Shizuku APIs: Remote binder call

Call any Android APIs which use binder (such as `getInstalledPackages`) as the identity of root (or adb).

See `getUsers` and `installApks` in the demo.

This may need the app itself to access hidden APIs, use [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) or other libraries you like.

### Using Shizuku APIs：UserService

Similar to [Bound services](https://developer.android.com/guide/components/bound-services), but the service runs as the identity of root (or adb). JNI is also supported.

See javadoc of `bindUserService` method which is super detailed.

See `bindUserService` in the demo.

## Changelog

### 12.1.0

- Automatically initialize Sui if you are using Shizuku
  
  You can opt-out this behavior by calling `ShizukuProvider#disableAutomaticSuiInitialization()` before `ShizukuProvider#onCreate()` is called

- Add a lot more detailed document for most APIs
- Drop pre-v11 support
  
  You don't need to worry about this problem, just show a "not supported" message if the user really uses pre-v11.
  
  - Sui was born after API v11, Sui users are not affected at all.
  - For Shizuku, according to Google Play statistics, more than 95% of users are on v11+. Shizuku drops Android 5 support from v5, many of the remaining 5% are such people who are stuck at super old versions.
  - A useful API, UserService, is added from v11 and stable on v12. I believe that many Shizuku apps already have a "version > 11" check.
  - I really want to drop pre-v11 support since [a possible system issue that may cause system soft reboot (system server crash) on uninstalling Shizuku](https://github.com/RikkaApps/Shizuku/issues/83).

### 12.0.0

- Add `Shizuku#peekUserService` that allows you to check if a specific user service is running
- Add `Shizuku.UserServiceArgs#daemon` that allows you to control if the user service should be run in the "Daemon mode"

## Migration guide for existing applications use Shizuku pre-v11
<details>
  <summary>Click to expand</summary>

### Changes

- Dependency changed (see Guide below)
- Self-implemented permission is used from v11, the API is the same to runtime permission (see the demo, and existing runtime permission still works)
- Package name is rename to `rikka.shizuku` (replace all `moe.shizuku.api.` to `rikka.shizuku.`)
- `ShizukuService` class is renamed to `Shizuku`
- Methods in `Shizuku` class now throw `RuntimeException` rather than `RemoteException` like other Android APIs
- Listeners are moved from `ShizukuProvider` class to `Shizuku` class

### Add support for Sui

- Call `Sui#init()`
- It's better to use check Sui with `Sui#isSui` before using Shizuku only methods in `ShizukuProvider`

</details>
