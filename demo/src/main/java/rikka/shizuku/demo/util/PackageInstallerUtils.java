package rikka.shizuku.demo.util;

import android.content.Context;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;

@SuppressWarnings({"JavaReflectionMemberAccess"})
public class PackageInstallerUtils {

    public static PackageInstaller createPackageInstaller(IPackageInstaller installer, String installerPackageName, String installerAttributionTag, int userId) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PackageInstaller.class.getConstructor(IPackageInstaller.class, String.class, String.class, int.class)
                    .newInstance(installer, installerPackageName, installerAttributionTag, userId);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PackageInstaller.class.getConstructor(IPackageInstaller.class, String.class, int.class)
                    .newInstance(installer, installerPackageName, userId);
        } else {
            return PackageInstaller.class.getConstructor(Context.class, PackageManager.class, IPackageInstaller.class, String.class, int.class)
                    .newInstance(ApplicationUtils.getApplication(), ApplicationUtils.getApplication().getPackageManager(), installer, installerPackageName, userId);
        }
    }

    public static PackageInstaller.Session createSession(IPackageInstallerSession session) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return PackageInstaller.Session.class.getConstructor(IPackageInstallerSession.class)
                .newInstance(session);

    }

    public static int getInstallFlags(PackageInstaller.SessionParams params) throws NoSuchFieldException, IllegalAccessException {
        return (int) PackageInstaller.SessionParams.class.getDeclaredField("installFlags").get(params);
    }

    public static void setInstallFlags(PackageInstaller.SessionParams params, int newValue) throws NoSuchFieldException, IllegalAccessException {
        PackageInstaller.SessionParams.class.getDeclaredField("installFlags").set(params, newValue);
    }
}
