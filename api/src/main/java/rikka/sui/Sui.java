package rikka.sui;

import android.os.IBinder;
import android.os.Parcel;

import rikka.shizuku.Shizuku;
import rikka.shizuku.SystemServiceHelper;

public class Sui {

    private static final int BRIDGE_TRANSACTION_CODE = ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I';
    private static final String BRIDGE_SERVICE_DESCRIPTOR = "android.app.IActivityManager";
    private static final String BRIDGE_SERVICE_NAME = "activity";
    private static final int BRIDGE_ACTION_GET_BINDER = 2;

    private static IBinder requestBinder() {
        IBinder binder = SystemServiceHelper.getSystemService(BRIDGE_SERVICE_NAME);
        if (binder == null) return null;

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(BRIDGE_SERVICE_DESCRIPTOR);
            data.writeInt(BRIDGE_ACTION_GET_BINDER);
            binder.transact(BRIDGE_TRANSACTION_CODE, data, reply, 0);
            reply.readException();
            IBinder received = reply.readStrongBinder();
            if (received != null) {
                return received;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
        return null;
    }

    private static boolean isSui;

    public static boolean isSui() {
        return isSui;
    }

    /**
     * Request binder from Sui. This method must only be called once.
     *
     * @param packageName Package name of the current process
     * @return If binder is received (this also shows if Sui is installed and if it is working)
     */
    public static boolean init(String packageName) {
        IBinder binder = requestBinder();
        if (binder != null) {
            Shizuku.onBinderReceived(binder, packageName);
            isSui = true;
            return true;
        }
        isSui = false;
        return false;
    }
}
