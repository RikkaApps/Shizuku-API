# Shizuku-API

API for [Shizuku](https://github.com/RikkaApps/Shizuku) and [Sui](https://github.com/RikkaApps/Sui).

The concept is "same API, different implementation", Shizuku and Sui shares the API design.

## Introduction

First of all, please read the README of [Shizuku](https://github.com/RikkaApps/Shizuku) and [Sui](https://github.com/RikkaApps/Sui), so that you will have a basic understanding of how Shizuku and Sui works.

The most important functions provided by Shizuku API is "remote binder call" and "user service". 

* Remote binder call

  Binder calls as root (or adb). In short, the experience is same to calling Android APIs such as `getInstalledPackages`, but as the identity of root (or adb).

* User service

  Run app's own Java service under root (or adb). The experience is petty similar to [Bound services](https://developer.android.com/guide/components/bound-services). JNI is  supported.

For existing applications, there is an API that allows you to create a "sh" as root (or adb), so you can always do what was previously possible.

## Migrating from "su"

* Simple applications which only use commands like `pm` `am`

  Use "Remote binder call".

* Complicated applications such as root file managers

  Use "User service".

* Complicated applications which heavily depend on commands in Linux world

  Use "User service" with JNI or continue to use shell.

## Different between Sui and Shizuku

### Sui

- Requires Magisk
- Requires the user to install two Magisk modules, "Riru" and "Riru - Sui"
- Does NOT required to install a manager app
- Possible to be bundled in Magisk in the future

### Shizuku

- Requires root or adb
  - For adb, it's required to manually restart with adb everytime on boot
  - For adb, of course, only has limited permissions of adb
- Requires the user to install a standalone app, Shizuku

## Guide

Note, something is not mentioned below, please be sure to read the [demo](https://github.com/RikkaApps/Shizuku-API/tree/master/demo).

1. Add dependency

   ```
   maven { url 'https://dl.bintray.com/rikkaw/Libraries' }
   ```
   
   ```
   def shizuku_version = '11.0.0'

   // required by Shizuku and Sui
   implementation "rikka.shizuku:api:$shizuku_version"

   // required by Shizuku
   implementation "rikka.shizuku:provider:$shizuku_version"
   ```
2. Add `ShizukuProvider` (Shizuku only)

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