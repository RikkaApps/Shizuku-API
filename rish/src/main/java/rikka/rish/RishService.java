package rikka.rish;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class RishService {

    private static final String TAG = "RishService";

    private static final Map<Integer, RishHost> HOSTS = new HashMap<>();

    private static final boolean IS_ROOT = Os.getuid() == 0;

    private void createHost(
            String[] args, String[] env, String dir,
            byte tty,
            ParcelFileDescriptor stdin, ParcelFileDescriptor stdout, ParcelFileDescriptor stderr) {

        int callingPid = Binder.getCallingPid();

        // Termux app set PATH and LD_PRELOAD to Termux's internal path.
        // Adb does not have sufficient permissions to access such places.

        // Under adb, users need to set RISH_PRESERVE_ENV=1 to preserve env.
        // Under root, keep env unless RISH_PRESERVE_ENV=0 is set.

        boolean allowEnv = IS_ROOT;
        for (String e : env) {
            if ("RISH_PRESERVE_ENV=1".equals(e)) {
                allowEnv = true;
                break;
            } else if ("RISH_PRESERVE_ENV=0".equals(e)) {
                allowEnv = false;
                break;
            }
        }
        if (!allowEnv) {
            env = null;
        }

        RishHost host = new RishHost(args, env, dir, tty, stdin, stdout, stderr);
        host.start();
        Log.d(TAG, "Forked " + host.getPid());

        HOSTS.put(callingPid, host);
    }

    private void setWindowSize(long size) {
        int callingPid = Binder.getCallingPid();

        RishHost host = HOSTS.get(callingPid);
        if (host == null) {
            Log.d(TAG, "Not existing host created by " + callingPid);
            return;
        }

        host.setWindowSize(size);
    }

    private int getExitCode() {
        int callingPid = Binder.getCallingPid();

        RishHost host = HOSTS.get(callingPid);
        if (host == null) {
            Log.d(TAG, "Not existing host created by " + callingPid);
            return -1;
        }

        return host.getExitCode();
    }

    public abstract void enforceCallingPermission(String func);

    public boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) {
        if (code == RishConfig.getTransactionCode(RishConfig.TRANSACTION_createHost)) {
            Log.d(TAG, "TRANSACTION_createHost");

            enforceCallingPermission("createHost");

            if (reply == null || (flags & IBinder.FLAG_ONEWAY) != 0) {
                return true;
            }

            ParcelFileDescriptor stdin;
            ParcelFileDescriptor stdout;
            ParcelFileDescriptor stderr = null;

            data.enforceInterface(RishConfig.getInterfaceToken());
            byte tty = data.readByte();
            stdin = data.readFileDescriptor();
            stdout = data.readFileDescriptor();
            if ((tty & RishConstants.ATTY_ERR) == 0) {
                stderr = data.readFileDescriptor();
            }
            String[] args = data.createStringArray();
            String[] env = data.createStringArray();
            String dir = data.readString();
            createHost(args, env, dir, tty, stdin, stdout, stderr);
            reply.writeNoException();
            return true;
        } else if (code == RishConfig.getTransactionCode(RishConfig.TRANSACTION_setWindowSize)) {
            Log.d(TAG, "TRANSACTION_setWindowSize");

            enforceCallingPermission("setWindowSize");

            data.enforceInterface(RishConfig.getInterfaceToken());
            long size = data.readLong();
            setWindowSize(size);
            if (reply != null) {
                reply.writeNoException();
            }
            return true;
        } else if (code == RishConfig.getTransactionCode(RishConfig.TRANSACTION_getExitCode)) {
            Log.d(TAG, "TRANSACTION_getExitCode");

            enforceCallingPermission("getExitCode");

            data.enforceInterface(RishConfig.getInterfaceToken());
            int exitCode = getExitCode();
            if (reply != null) {
                reply.writeNoException();
                reply.writeInt(exitCode);
            }
            return true;
        }
        return false;
    }

}
