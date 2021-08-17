package rikka.shizuku.server;

import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_TRANSACTION_destroy;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;

import java.util.UUID;

import moe.shizuku.server.IShizukuServiceConnection;
import rikka.shizuku.server.util.Logger;

public abstract class UserServiceRecord {

    private class ConnectionList extends RemoteCallbackList<IShizukuServiceConnection> {

        @Override
        public void onCallbackDied(IShizukuServiceConnection callback) {
            if (daemon || getRegisteredCallbackCount() != 0) {
                return;
            }

            LOGGER.v("Remove service record %s since it does not run as a daemon and all connections are gone", token);
            removeSelf();
        }
    }

    protected static final Logger LOGGER = new Logger("UserServiceRecord");

    private final IBinder.DeathRecipient deathRecipient;
    public final int versionCode;
    public String token;
    public IBinder service;
    public final RemoteCallbackList<IShizukuServiceConnection> callbacks = new ConnectionList();
    public boolean daemon;

    public UserServiceRecord(int versionCode, boolean daemon) {
        this.versionCode = versionCode;
        this.token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        this.deathRecipient = () -> {
            LOGGER.v("Binder for service record %s is dead", token);
            removeSelf();
        };
        this.daemon = daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public void setBinder(IBinder binder) {
        LOGGER.v("Binder received for service record %s", token);

        service = binder;

        try {
            binder.linkToDeath(deathRecipient, 0);
        } catch (Throwable tr) {
            LOGGER.w("linkToDeath %s", token);
        }

        broadcastBinderReceived();
    }

    public void broadcastBinderReceived() {
        LOGGER.v("Broadcast binder received for service record %s", token);

        int count = callbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                callbacks.getBroadcastItem(i).connected(service);
            } catch (Throwable e) {
                LOGGER.w("Failed to call connected %s", token);
            }
        }
        callbacks.finishBroadcast();
    }

    public void broadcastBinderDied() {
        LOGGER.v("Broadcast binder died for service record %s", token);

        int count = callbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                callbacks.getBroadcastItem(i).died();
            } catch (Throwable e) {
                LOGGER.w("Failed to call died %s", token);
            }
        }
        callbacks.finishBroadcast();
    }

    public abstract void removeSelf();

    public void destroy() {
        if (service != null) {
            service.unlinkToDeath(deathRecipient, 0);
        }

        if (service != null && service.pingBinder()) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(service.getInterfaceDescriptor());
                service.transact(USER_SERVICE_TRANSACTION_destroy, data, reply, Binder.FLAG_ONEWAY);
            } catch (Throwable e) {
                LOGGER.w("Failed to call destroy %s", token);
            } finally {
                data.recycle();
                reply.recycle();
            }
        }

        callbacks.kill();
    }
}
