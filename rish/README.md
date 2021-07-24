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

If the backend runs under root, `RISH_PRESERVE_ENV` will be treated as `1` when not set.