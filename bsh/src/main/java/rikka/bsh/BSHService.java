package rikka.bsh;

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

public abstract class BSHService {

    private static final String TAG = "BSHService";

    private static final Map<Integer, BSHHost> HOSTS = new HashMap<>();

    private static final boolean IS_ROOT = Os.getuid() == 0;

    private void createHost(String[] args, String[] env, String dir, ParcelFileDescriptor stdin, ParcelFileDescriptor stdout) {
        int callingPid = Binder.getCallingPid();

        // Termux app set PATH and LD_PRELOAD to Termux's internal path.
        // Adb does not have sufficient permissions to access such places.

        // Under adb, users need to set BSH_PRESERVE_ENV=1 to preserve env.
        // Under root, keep env unless BSH_PRESERVE_ENV=0 is set.

        boolean allowEnv = IS_ROOT;
        for (String e : env) {
            if ("BSH_PRESERVE_ENV=1".equals(e)) {
                allowEnv = true;
                break;
            } else if ("BSH_PRESERVE_ENV=0".equals(e)) {
                allowEnv = false;
                break;
            }
        }
        if (!allowEnv) {
            env = null;
        }

        BSHHost host = BSHHost.create(args, env, dir);
        host.prepare(stdin, stdout);
        host.start();
        Log.d(TAG, "Forked " + host.getPid());

        HOSTS.put(callingPid, host);
    }

    private void setWindowSize(long size) {
        int callingPid = Binder.getCallingPid();

        BSHHost host = HOSTS.get(callingPid);
        if (host == null) {
            Log.d(TAG, "Not existing host created by " + callingPid);
            return;
        }

        host.setWindowSize(size);
    }

    private int getExitCode() {
        int callingPid = Binder.getCallingPid();

        BSHHost host = HOSTS.get(callingPid);
        if (host == null) {
            Log.d(TAG, "Not existing host created by " + callingPid);
            return -1;
        }

        return host.getExitCode();
    }

    public abstract void enforceCallingPermission(String func);

    public boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) {
        if (code == BSHConfig.getTransactionCode(BSHConfig.TRANSACTION_createHost)) {
            Log.d(TAG, "TRANSACTION_createHost");

            enforceCallingPermission("createHost");

            if (reply == null || (flags & IBinder.FLAG_ONEWAY) != 0) {
                return true;
            }

            data.enforceInterface(BSHConfig.getInterfaceToken());
            ParcelFileDescriptor stdin = data.readFileDescriptor();
            ParcelFileDescriptor stdout = data.readFileDescriptor();
            String[] args = data.createStringArray();
            String[] env = data.createStringArray();
            String dir = data.readString();
            createHost(args, env, dir, stdin, stdout);
            reply.writeNoException();
            return true;
        } else if (code == BSHConfig.getTransactionCode(BSHConfig.TRANSACTION_setWindowSize)) {
            Log.d(TAG, "TRANSACTION_setWindowSize");

            enforceCallingPermission("setWindowSize");

            data.enforceInterface(BSHConfig.getInterfaceToken());
            long size = data.readLong();
            setWindowSize(size);
            if (reply != null) {
                reply.writeNoException();
            }
            return true;
        } else if (code == BSHConfig.getTransactionCode(BSHConfig.TRANSACTION_getExitCode)) {
            Log.d(TAG, "TRANSACTION_getExitCode");

            enforceCallingPermission("getExitCode");

            data.enforceInterface(BSHConfig.getInterfaceToken());
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
