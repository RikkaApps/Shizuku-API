package rikka.shizuku.server.api;

import android.annotation.SuppressLint;
import android.app.ActivityManagerNative;
import android.app.ContentProviderHolder;
import android.app.IActivityManager;
import android.app.IActivityManager23;
import android.app.IApplicationThread;
import android.app.IProcessObserver;
import android.app.IUidObserver;
import android.app.ProfilerInfo;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.RemoteException;
import android.permission.IPermissionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.os.BuildCompat;

import com.android.internal.app.IAppOpsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import dev.rikka.tools.refine.Refine;
import kotlin.jvm.JvmStatic;
import rikka.shizuku.server.util.OsUtils;

@BuildCompat.PrereleaseSdkCheck
public class SystemService {

    private SystemService() {
    }

    private static final SystemServiceBinder<IActivityManager> activityManager = new SystemServiceBinder<>("activity", binder -> {
        if (Build.VERSION.SDK_INT >= 26) {
            return IActivityManager.Stub.asInterface(binder);
        } else {
            return ActivityManagerNative.asInterface(binder);
        }
    });

    private static final SystemServiceBinder<IPackageManager> packageManager = new SystemServiceBinder<>("package", IPackageManager.Stub::asInterface);

    private static final SystemServiceBinder<IUserManager> userManager = new SystemServiceBinder<>("user", IUserManager.Stub::asInterface);

    private static final SystemServiceBinder<IAppOpsService> appOps = new SystemServiceBinder<>("appops", IAppOpsService.Stub::asInterface);

    @RequiresApi(30)
    private static final SystemServiceBinder<IPermissionManager> permissionManager = new SystemServiceBinder<>("permissionmgr", IPermissionManager.Stub::asInterface);

    public static int checkPermission(@Nullable String permission, int pid, int uid) throws RemoteException {
        return activityManager.getService().checkPermission(permission, pid, uid);
    }

    @Nullable
    public static PackageInfo getPackageInfo(@Nullable String packageName, long flags, int userId) throws RemoteException {
        if (BuildCompat.isAtLeastT()) {
            return packageManager.getService().getPackageInfo(packageName, flags, userId);
        } else {
            return packageManager.getService().getPackageInfo(packageName, (int) flags, userId);
        }
    }

    @Nullable
    public static ApplicationInfo getApplicationInfo(@Nullable String packageName, long flags, int userId) throws RemoteException {
        if (BuildCompat.isAtLeastT()) {
            return packageManager.getService().getApplicationInfo(packageName, flags, userId);
        } else {
            return packageManager.getService().getApplicationInfo(packageName, (int) flags, userId);
        }
    }

    public static int checkPermission(@Nullable String permName, int uid) throws RemoteException {
        if (Build.VERSION.SDK_INT != 30) {
            return packageManager.getService().checkUidPermission(permName, uid);
        } else {
            return permissionManager.getService().checkUidPermission(permName, uid);
        }
    }

    public static void registerProcessObserver(@Nullable IProcessObserver processObserver) throws RemoteException {
        activityManager.getService().registerProcessObserver(processObserver);
    }

    public static void registerUidObserver(@Nullable IUidObserver observer, int which, int cutpoint, @Nullable String callingPackage) throws RemoteException {
        activityManager.getService().registerUidObserver(observer, which, cutpoint, callingPackage);

    }

    @Nullable
    public static String[] getPackagesForUid(int uid) throws RemoteException {
        return packageManager.getService().getPackagesForUid(uid);
    }

    @Nullable
    public static ParceledListSlice<ApplicationInfo> getInstalledApplications(long flags, int userId) throws RemoteException {
        if (BuildCompat.isAtLeastT()) {
            //noinspection unchecked
            return packageManager.getService().getInstalledApplications(flags, userId);
        } else {
            //noinspection unchecked
            return packageManager.getService().getInstalledApplications((int) flags, userId);
        }
    }

    @Nullable
    public static ParceledListSlice<PackageInfo> getInstalledPackages(long flags, int userId) throws RemoteException {
        if (BuildCompat.isAtLeastT()) {
            //noinspection unchecked
            return packageManager.getService().getInstalledPackages(flags, userId);
        } else {
            //noinspection unchecked
            return packageManager.getService().getInstalledPackages((int) flags, userId);
        }
    }

    @Nullable
    public static IContentProvider getContentProviderExternal(@Nullable String name, int userId, @Nullable IBinder token, @Nullable String tag) throws RemoteException {
        IActivityManager am = activityManager.getService();
        ContentProviderHolder contentProviderHolder;
        IContentProvider provider;
        if (Build.VERSION.SDK_INT >= 29) {
            contentProviderHolder = am.getContentProviderExternal(name, userId, token, tag);
            provider = contentProviderHolder != null ? contentProviderHolder.provider : null;
        } else if (Build.VERSION.SDK_INT >= 26) {
            contentProviderHolder = am.getContentProviderExternal(name, userId, token);
            provider = contentProviderHolder != null ? contentProviderHolder.provider : null;
        } else {
            provider = Refine.<IActivityManager23>unsafeCast(am).getContentProviderExternal(name, userId, token).provider;
        }

        return provider;
    }

    public static void removeContentProviderExternal(@Nullable String name, @Nullable IBinder token) throws RemoteException {
        activityManager.getService().removeContentProviderExternal(name, token);
    }

    @SuppressLint("NewApi")
    @NonNull
    public static List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) throws RemoteException {
        IUserManager um = userManager.getService();
        List<UserInfo> list;
        if (Build.VERSION.SDK_INT >= 30) {
            list = um.getUsers(excludePartial, excludeDying, excludePreCreated);
        } else {
            try {
                list = um.getUsers(excludeDying);
            } catch (NoSuchMethodError var6) {
                list = um.getUsers(excludePartial, excludeDying, excludePreCreated);
            }
        }
        return list;
    }

    @NonNull
    public static UserInfo getUserInfo(int userId) {
        IUserManager um = userManager.getService();
        return um.getUserInfo(userId);
    }

    @NonNull
    public static List<PackageInfo> getInstalledPackagesNoThrow(long flags, int userId) {
        try {
            ParceledListSlice<PackageInfo> parceledListSlice = getInstalledPackages(flags, userId);
            if (parceledListSlice != null && parceledListSlice.getList() != null) {
                return parceledListSlice.getList();
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    @NonNull
    public static List<ApplicationInfo> getInstalledApplicationsNoThrow(long flags, int userId) {
        try {
            ParceledListSlice<ApplicationInfo> parceledListSlice = getInstalledApplications(flags, userId);
            if (parceledListSlice != null && parceledListSlice.getList() != null) {
                return parceledListSlice.getList();
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    @Nullable
    public static PackageInfo getPackageInfoNoThrow(@Nullable String packageName, long flags, int userId) {
        try {
            return getPackageInfo(packageName, flags, userId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static ApplicationInfo getApplicationInfoNoThrow(@Nullable String packageName, long flags, int userId) {
        try {
            return getApplicationInfo(packageName, flags, userId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @JvmStatic
    @NonNull
    public static List<Integer> getUserIdsNoThrow() {
        ArrayList<Integer> users = new ArrayList<>();
        try {
            for (UserInfo ui : getUsers(true, true, true)) {
                users.add(ui.id);
            }
        } catch (Throwable tr) {
            users.clear();
            users.add(0);
        }

        return users;
    }

    @NonNull
    public static List<String> getPackagesForUidNoThrow(int uid) {
        ArrayList<String> packages = new ArrayList<>();

        try {
            String[] packagesArray = getPackagesForUid(uid);
            if (packagesArray != null) {
                for (String packageName : packagesArray) {
                    if (packageName != null) {
                        packages.add(packageName);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return packages;
    }

    public static void forceStopPackageNoThrow(@Nullable String packageName, int userId) {
        try {
            activityManager.getService().forceStopPackage(packageName, userId);
        } catch (Exception ignored) {
        }
    }

    public static void startActivity(@Nullable Intent intent, @Nullable String mimeType, int userId) throws RemoteException {
        activityManager.getService().startActivityAsUser((IApplicationThread) null, OsUtils.getUid() == 2000 ? "com.android.shell" : null, intent, mimeType, (IBinder) null, (String) null, 0, 0, (ProfilerInfo) null, (Bundle) null, userId);
    }

    public static void startActivityNoThrow(@NonNull Intent intent, @Nullable String mimeType, int userId) {
        try {
            startActivity(intent, mimeType, userId);
        } catch (Throwable ignored) {
        }
    }

    public static void grantRuntimePermission(@Nullable String packageName, @Nullable String permissionName, int userId) throws RemoteException {
        if (Build.VERSION.SDK_INT >= 30) {
            IPermissionManager perm = permissionManager.getService();
            Objects.requireNonNull(perm);
            perm.grantRuntimePermission(packageName, permissionName, userId);
        } else {
            IPackageManager pm = packageManager.getService();
            Objects.requireNonNull(pm);
            pm.grantRuntimePermission(packageName, permissionName, userId);
        }
    }

    @JvmStatic
    public static void revokeRuntimePermission(@Nullable String packageName, @Nullable String permissionName, int userId) throws RemoteException {
        if (Build.VERSION.SDK_INT >= 30) {
            IPermissionManager perm = permissionManager.getService();
            Objects.requireNonNull(perm);

            try {
                perm.revokeRuntimePermission(packageName, permissionName, userId, (String) null);
            } catch (NoSuchMethodError e) {
                perm.revokeRuntimePermission(packageName, permissionName, userId);
            }
        } else {
            IPackageManager pm = packageManager.getService();
            Objects.requireNonNull(pm);
            pm.revokeRuntimePermission(packageName, permissionName, userId);
        }

    }
}
