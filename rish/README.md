# RISH

<del>Rish is an Interactive SHell for android</del>

## Description

`rish` is an Android program for interacting with a shell that runs on a high-privileged daemon process.

Currently, Shizuku and Sui are two available backends.

## Usage

First of all, follow the guide from Shizuku or Sui to create the files of `rish`.

The remain is very simple, you only need to replace `sh` with `rish` in the command you want to run, `rish` will pass arguments directly to the remote shell.

Here is an example.

```
rish -c 'ls'
```

This is what will be executed at remote:

```
/system/bin/sh -c 'ls'
```

If you want to use other shells rather than `/system/bin/sh`, use `rish exec /path/to/other/shell`.

## Options

Since `rish` passes arguments directly to the remote, `rish` uses environment variable for options.

### RISH_PRESERVE_ENV

| Value | Description                                                          |
|-------|----------------------------------------------------------------------|
| `0`   | Do not change environment variables of the remote process            |
| `1`   | Replace the environment variables of the remote process with local's |

Termux app set `PATH` and `LD_PRELOAD` to Termux's internal path.
Adb (Shizuku can run under adb) does not have sufficient permissions to access such places, making users can even not using commands like `cd`.

If the backend runs under adb, `RISH_PRESERVE_ENV` will be treated as `0` when not set.

If the backend runs under root, `RISH_PRESERVE_ENV` will be treated as `1` when not set.#!/system/bin/sh
BASEDIR=$(dirname "$0")
DEX="$BASEDIR"/rish_shizuku.dex

if [ ! -f "$DEX" ]; then
  echo "Cannot find $DEX, please check the tutorial in Shizuku app"
  exit 1
fi

if [ $(getprop ro.build.version.sdk) -ge 34 ]; then
  if [ -w $DEX ]; then
    echo "On Android 14+, app_process cannot load writable dex."
    echo "Attempting to remove the write permission..."
    chmod 400 $DEX
  fi
  if [ -w $DEX ]; then
    echo "Cannot remove the write permission of $DEX."
    echo "You can copy to file to terminal app's private directory (/data/data/<package>, so that remove write permission is possible"
    exit 1
  fi
fi

# Replace "PKG" with the application id of your terminal app
[ -z "$RISH_APPLICATION_ID" ] && export RISH_APPLICATION_ID="PKG"
/system/bin/app_process -Djava.class.path="$DEX" /system/bin --nice-name=rish rikka.shizuku.shell.ShizukuShellLoader "$@"
