# Shizuku-API

API and developer guide for [Shizuku](https://github.com/RikkaApps/Shizuku) and [Sui](https://github.com/RikkaApps/Sui).

The concept is "same API, different implementation", Shizuku and Sui shares the API design. As the application developer, you only need to write the code once to support both Shizuku and Sui.

## Introduction

First of all, please read the README of [Shizuku](https://github.com/RikkaApps/Shizuku) and [Sui](https://github.com/RikkaApps/Sui), so that you will have a basic understanding of how Shizuku and Sui works.

The most important functions provided by Shizuku API is "remote binder call" and "user service". 

* Remote binder call

  Call any Android APIs which uses binder (such as `getInstalledPackages`) as the identity of root (or adb).

* User service

  Similar to [Bound services](https://developer.android.com/guide/components/bound-services), but the service runs as the identity of root (or adb). JNI is also supported.

## Requirements

Shizuku and Sui requires the user to install something first.

#### Sui

- Requires Magisk
- Requires the user to install two Magisk modules, "Riru" and "Riru - Sui"

#### Shizuku

- Requires root or adb
  - For adb, it's required to manually restart with adb everytime on boot
  - For adb, of course, only has limited permissions of adb
- Requires the user to install a standalone app, Shizuku

## Make a choice

For root-only apps, continue to support the old school "su" or not, you may have to make this choice.

Since Sui is possible to be bundled in Magisk in the future (Riru is already in Magisk), abandon old school "su" is not a bad choice.

For existing applications, there is an API that allows you to create a "sh" as root (or adb), so you can always do what was previously possible.

## Migrating from "su"

* Simple applications which only use commands like `pm` `am`

  Use "Remote binder call".

* Complicated applications such as root file managers

  Use "User service".

* Complicated applications which heavily depend on commands in Linux world

  Use "User service" with JNI or continue to use shell.

## Migration guide for existing applications use Shizuku pre-v11
<details>
  <summary>Click to expand</summary>

### Changes

- Dependency changed (see Guide below)
- Self-implemented permission is used from v11, the API is same to runtime permission (see demo, and existing runtime permission still works)
- Package name is rename to `rikka.shizuku` (replace all `moe.shizuku.api.` to `rikka.shizuku.`)
- `ShizukuService` class is renamed to `Shizuku`
- Methods in `Shizuku` class now throw `RuntimeException` rather than `RemoteException` like other Android APIs
- Listeners are moved from `ShizukuProvider` class to `Shizuku` class

### Add support for Sui

- Call `Sui#init()`
- Do not use `ShizukuProvider#isShizukuInstalled` since Sui does not have a manager
- It's better to use check Sui with `Sui#isSui` before using Shizuku only methods in `ShizukuProvider`

</details>

## Guide

Note, something is not mentioned below, please be sure to read the [demo](https://github.com/RikkaApps/Shizuku-API/tree/master/demo).

1. Add dependency

   ```
   maven { url 'https://dl.bintray.com/rikkaw/Libraries' }
   ```
   
   ```
   def shizuku_version = '11.0.1'
   implementation "rikka.shizuku:api:$shizuku_version"

   // add this if you want to support Shizuku
   implementation "rikka.shizuku:provider:$shizuku_version"
   ```

   Since all root users using Shizuku will eventually switch to Sui, if your application requires root, it's better not to support Shizuku from the begining.

2. Add `ShizukuProvider` (Shizuku only)

   Don't add this if your app only supports Sui.

   Add to your `AndroidManifest.xml`.

   ```
   <provider
        android:name="rikka.shizuku.ShizukuProvider"
        android:authorities="${applicationId}.shizuku"
        android:multiprocess="false"
        android:enabled="true"
        android:exported="true"
        android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
   ```

3. Use

   See demo.