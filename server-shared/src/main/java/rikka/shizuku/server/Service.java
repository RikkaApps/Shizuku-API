package rikka.shizuku.server;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemProperties;
import android.system.Os;

import androidx.annotation.Nullable;

import moe.shizuku.server.IShizukuService;
import moe.shizuku.server.IShizukuServiceConnection;
import rikka.rish.RishService;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.server.api.SystemService;
import rikka.shizuku.server.util.Logger;
import rikka.shizuku.server.util.OsUtils;

public abstract class Service<
        UserServiceMgr extends UserServiceManager,
        ClientMgr extends ClientManager<ConfigMgr>,
        ConfigMgr extends ConfigManager> extends IShizukuService.Stub {

    private final UserServiceMgr userServiceManager = onCreateUserServiceManager();

    private final ClientMgr clientManager = onCreateClientManager();

    private final ConfigMgr configManager = onCreateConfigManager();

    private final RishService rishService = new RishService() {

        @Override
        public void enforceCallingPermission(String func) {
            Service.this.enforceCallingPermission(func);
        }
    };

    private final Logger LOGGER = onCreateLogger();

    public Service() {

    }

    public abstract UserServiceMgr onCreateUserServiceManager();

    public abstract ClientMgr onCreateClientManager();

    public abstract ConfigMgr onCreateConfigManager();

    public abstract Logger onCreateLogger();

    public final UserServiceMgr getUserServiceManager() {
        return userServiceManager;
    }

    public final ClientMgr getClientManager() {
        return clientManager;
    }

    public ConfigMgr getConfigManager() {
        return configManager;
    }

    public abstract boolean checkCallerManagerPermission(String func, int callingUid, int callingPid);

    public final void enforceManagerPermission(String func) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingPid == Os.getpid()) {
            return;
        }

        if (checkCallerManagerPermission(func, callingUid, callingPid)) {
            return;
        }

        String msg = "Permission Denial: " + func + " from pid="
                + Binder.getCallingPid()
                + " is not manager ";
        LOGGER.w(msg);
        throw new SecurityException(msg);
    }

    public abstract boolean checkCallerPermission(String func, int callingUid, int callingPid, @Nullable ClientRecord clientRecord);

    public final void enforceCallingPermission(String func) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == OsUtils.getUid()) {
            return;
        }

        ClientRecord clientRecord = clientManager.findClient(callingUid, callingPid);

        if (checkCallerPermission(func, callingUid, callingPid, clientRecord)) {
            return;
        }

        if (clientRecord == null) {
            String msg = "Permission Denial: " + func + " from pid="
                    + Binder.getCallingPid()
                    + " is not an attached client";
            LOGGER.w(msg);
            throw new SecurityException(msg);
        }

        if (!clientRecord.allowed) {
            String msg = "Permission Denial: " + func + " from pid="
                    + Binder.getCallingPid()
                    + " requires permission";
            LOGGER.w(msg);
            throw new SecurityException(msg);
        }
    }

    public final ClientRecord requireClient(int callingUid, int callingPid) {
        ClientRecord clientRecord = clientManager.findClient(callingUid, callingPid);
        if (clientRecord == null) {
            LOGGER.w("Caller (uid %d, pid %d) is not an attached client", callingUid, callingPid);
            throw new IllegalStateException("Not an attached client");
        }
        return clientRecord;
    }

    public final void transactRemote(Parcel data, Parcel reply, int flags) throws RemoteException {
        enforceCallingPermission("transactRemote");

        IBinder targetBinder = data.readStrongBinder();
        int targetCode = data.readInt();

        LOGGER.d("transact: uid=%d, descriptor=%s, code=%d", Binder.getCallingUid(), targetBinder.getInterfaceDescriptor(), targetCode);
        Parcel newData = Parcel.obtain();
        try {
            newData.appendFrom(data, data.dataPosition(), data.dataAvail());
        } catch (Throwable tr) {
            LOGGER.w(tr, "appendFrom");
            return;
        }
        try {
            long id = Binder.clearCallingIdentity();
            targetBinder.transact(targetCode, newData, reply, flags);
            Binder.restoreCallingIdentity(id);
        } finally {
            newData.recycle();
        }
    }

    @Override
    public final int getVersion() {
        enforceCallingPermission("getVersion");
        return ShizukuApiConstants.SERVER_VERSION;
    }

    @Override
    public final int getUid() {
        enforceCallingPermission("getUid");
        return Os.getuid();
    }

    @Override
    public final int checkPermission(String permission) throws RemoteException {
        enforceCallingPermission("checkPermission");
        return SystemService.checkPermission(permission, Os.getuid());
    }

    @Override
    public final String getSELinuxContext() {
        enforceCallingPermission("getSELinuxContext");

        try {
            return SELinux.getContext();
        } catch (Throwable tr) {
            throw new IllegalStateException(tr.getMessage());
        }
    }

    @Override
    public final String getSystemProperty(String name, String defaultValue) {
        enforceCallingPermission("getSystemProperty");

        try {
            return SystemProperties.get(name, defaultValue);
        } catch (Throwable tr) {
            throw new IllegalStateException(tr.getMessage());
        }
    }

    @Override
    public final void setSystemProperty(String name, String value) {
        enforceCallingPermission("setSystemProperty");

        try {
            SystemProperties.set(name, value);
        } catch (Throwable tr) {
            throw new IllegalStateException(tr.getMessage());
        }
    }

    @Override
    public final int removeUserService(IShizukuServiceConnection conn, Bundle options) {
        enforceCallingPermission("removeUserService");

        return userServiceManager.removeUserService(conn, options);
    }

    @Override
    public final int addUserService(IShizukuServiceConnection conn, Bundle options) {
        enforceCallingPermission("addUserService");

        return userServiceManager.addUserService(conn, options);
    }

    @Override
    public final void attachUserService(IBinder binder, Bundle options) {
        enforceManagerPermission("attachUserService");

        userServiceManager.attachUserService(binder, options);
    }

    @Override
    public final boolean checkSelfPermission() {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == OsUtils.getUid() || callingPid == OsUtils.getPid()) {
            return true;
        }

        return requireClient(callingUid, callingPid).allowed;
    }

    @Override
    public boolean shouldShowRequestPermissionRationale() {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == OsUtils.getUid() || callingPid == OsUtils.getPid()) {
            return true;
        }

        requireClient(callingUid, callingPid);

        ConfigPackageEntry entry = configManager.find(callingUid);
        return entry != null && entry.isDenied();
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == ShizukuApiConstants.BINDER_TRANSACTION_transact) {
            data.enforceInterface(ShizukuApiConstants.BINDER_DESCRIPTOR);
            transactRemote(data, reply, flags);
            return true;
        } else if (rishService.onTransact(code, data, reply, flags)) {
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}
