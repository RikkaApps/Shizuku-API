package rikka.shizuku.service.api;

import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;

public class SystemServiceBinder<T extends IInterface> implements IBinder, IBinder.DeathRecipient {

    public interface ServiceCreator<T> {
        T create(IBinder binder);
    }

    private final String name;
    private final ServiceCreator<T> serviceCreator;

    private IBinder binderCache;
    private T serviceCache;

    public SystemServiceBinder(String name, ServiceCreator<T> serviceCreator) {
        this.name = name;
        this.serviceCreator = serviceCreator;
    }

    public IBinder getBinder() {
        if (binderCache != null) {
            return binderCache;
        }

        IBinder binder = ServiceManager.getService(name);
        if (binder == null) {
            return null;
        }

        try {
            binder.linkToDeath(this, 0);
        } catch (Throwable ignored) {
        }

        binderCache = binder;
        return binder;
    }

    public T getService() {
        if (serviceCache != null) {
            return serviceCache;
        }

        IBinder binder = getBinder();
        if (binder == null) {
            return null;
        }

        serviceCache = serviceCreator.create(binder);
        return serviceCache;
    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        IBinder binder = getBinder();
        if (binder == null) {
            return false;
        }

        try {
            return binder.transact(code, data, reply, flags);
        } catch (DeadObjectException e) {
            binderCache = null;

            IBinder newBinder = getBinder();
            if (newBinder != null) {
                return binder.transact(code, data, reply, flags);
            }
        }
        return false;
    }

    @Override
    public void binderDied() {
        binderCache.unlinkToDeath(this, 0);
        binderCache = null;
        serviceCache = null;
    }

    @Nullable
    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        IBinder binder = getBinder();
        if (binder != null) {
            return binder.getInterfaceDescriptor();
        }
        return null;
    }

    @Override
    public boolean pingBinder() {
        IBinder binder = getBinder();
        if (binder != null) {
            return binder.pingBinder();
        }
        return false;
    }

    @Override
    public boolean isBinderAlive() {
        IBinder binder = getBinder();
        if (binder != null) {
            return binder.isBinderAlive();
        }
        return false;
    }

    @Nullable
    @Override
    public IInterface queryLocalInterface(@NonNull String s) {
        IBinder binder = getBinder();
        if (binder != null) {
            return binder.queryLocalInterface(s);
        }
        return null;
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        IBinder binder = getBinder();
        if (binder != null) {
            binder.dump(fd, args);
        }
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        IBinder binder = getBinder();
        if (binder != null) {
            binder.dumpAsync(fd, args);
        }
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) throws RemoteException {
        IBinder binder = getBinder();
        if (binder != null) {
            binder.linkToDeath(recipient, flags);
        }
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        IBinder binder = getBinder();
        if (binder != null) {
            return binder.unlinkToDeath(recipient, flags);
        }
        return false;
    }
}
