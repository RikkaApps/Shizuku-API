# Shizuku-API

Shizuku API is the API provided by [Shizuku](https://github.com/RikkaApps/Shizuku) and [Sui](https://github.com/RikkaApps/Sui). With Shizuku API, you can your Java codes (JNI is also supported) as the identity of root or shell (adb).

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
def shizuku_version = (the versoin above)
implementation "dev.rikka.shizuku:api:$shizuku_version"

// Add this line if you want to support Shizuku
implementation "dev.rikka.shizuku:provider:$shizuku_version"
```

### Acquire the Binder

The first step is to acquire the Binder from Shizuku or Sui.

`Shizuku` class provides listeners, `Shizuku#addBinderReceivedListener()` and `Shizuku.addBinderDeadListener()`, that allows you to track the life of the binder. You should call methods in `Shizuku` class when the binder is alive or you will get an `IllegalStateException`.

The steps to get a Binder from Shizuku and Shizuku are different.

#### Sui

Call `Sui.init(packageName)` before using `Shizuku` class. This method only needs to be called once. If this method returns true, means Sui is installed and available.

For multi-process applications, call this method in every process that needs to use Shizuku API.

Note, request the binder for Sui only requires two times of binder IPC, this is significantly cheaper than initialize Shizuku which uses `ContentProvider`. `Sui.init(packageName)` can be used in main thread, you don't need to worry about performance.

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

Starting from v12.1.0, Sui is initialized automatically in `ShizukuProvider`. You can opt-out this behavior by calling `ShizukuProvider#disableAutomaticSuiInitialization()` before `ShizukuProvider#onCreate()` is called. Unless there are special reasons, apps that support Shizuku should also support Sui, otherwise it will cause user confusion.

### Request permission

Requesting permission is similar to [requesting runtime permissions](https://developer.android.com/training/permissions/requesting).

A simple example of requesting permission:

```java
private void onRequestPermissionsResult(int requestCode, int grantResult) {
    boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
    // Do stuff based on the result and the request code
}

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

### Differents of the privilege betweent ADB and ROOT

Shizuku can be started with ADB or ROOT, and Sui is a Magisk module, so the privilege could be ADB or ROOT. You can use `Shizuku#getUid()` to check your privilege, for ROOT it returns `0`, for ADB is `2000`.

What ADB can do is significantly different from ROOT:

* In the Android world, the privilege is determined by Android permissions. See [AndroidManifest of Shell](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/packages/Shell/AndroidManifest.xml), all the permission granted to Shell (ADB) are listed here. Be aware, the permission changes under different Android versions.

* In Linux world, the privilege is determined by Shell's uid, capabilities, SELinux context, etc. For example, Shell (ADB) cannot access other apps' data files `/data/user/0/<package>`.

### Remote binder call

This is a relatively simple way, but what you can do is limited to Binder calls. Therefore, this is only suitable for simple applications.

Shizuku API provides `rikka.shizuku.ShizukuBinderWrapper` class which forward Binder calls to Shizuku service which has ADB or ROOT privilege.

### UserService

User Service is like [Bound services](https://developer.android.com/guide/components/bound-services) which allows you to run Java or native codes (through JNI). The difference is that the service runs in a different process and as the identity (Linux UID) of root (UID 0) or shell (UID 2000, if the backend is Shizuku and user starts Shizuku with adb).

There are no restrictions on non-SDK APIs in the user service process. However, the User Service process is not a valid Android application process. Therefore, even if you can acquire a `Context` instance, many APIs, such as `Context#registerReceiver` and `Context#getContentResolver` will not work. You will need to dig into Android source code to find out how things work.

* Start the User Service

  Use `bindUserService` method. This method has two parameters, `UserServiceArgs` and `ServiceConnection`.

  `UserServiceArgs` is like `Intent` in Bound services, which decides which service will be started and some options.

  `ServiceConnection` is same as Bound services, but only `onServiceConnected` and `onServiceDisconnected` are used.
  
  Unlike Bound service, the service class must implement `IBinder` interface. The usual usage is `public class YourService extends IYouAidlInterface.Stub`.

  The service class can have two constructors, one is default constructor, another is with `Context` parameter available from Shizuku v13. Shizuku v13 will try the constructor with `Context` parameter first. Older Shizuku will always use the default constructor. Beaware that the `Context` does not work as same as `Context` in normal Android application. See "Use Android APIs in user service" below.

  Shizuku uses `tag` from `UserServiceArgs` to determine if the User Service is same. If `tag` is not set, class name will be uses, but class name is unstable after ProGuard/R8. If `version` from `UserServiceArgs` mismatches, a new User Service will be start and "destroy" method (see below) will be called for the old.

* Stop the User Service

  Use `unbindUserService` method. However, the user service process will **NOT** be killed automatically. You need to implement a "destroy" method in your service. The transaction code for that method is `16777115` (use `16777114` in aidl). In this method, you can do some cleanup jobs and call `System.exit()` in the end.

### The use of non-SDK interfaces

For "Remote binder call", as the APIs are accessed from the app's process, you may need to use [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) or any ways you want to bypass restrictions on non-SDK interfaces.

We also provides [HiddenApiRefinePlugin](https://github.com/RikkaApps/HiddenApiRefinePlugin) to help you to programing with hidden APIs conveniently.

## Changelog

### 13.0.0

- The constructor of `UserService` can have a `Context` parameter which value is the `Context` used to create the instance of `UserService`

### 12.2.0

- Fix `onServiceDisconnected` is not called if the UserService is stopped by `Shizuku#unbindUserService`

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
