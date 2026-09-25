package rikka.shizuku.server;

import android.app.ActivityThread;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextHidden;
import android.ddm.DdmHandleAppName;
import android.os.Build;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserHandleHidden;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dev.rikka.tools.refine.Refine;

public class UserService {

    private static String TAG;

    public static void setTag(String tag) {
        UserService.TAG = tag;
    }

    @Nullable
    public static Pair<IBinder, String> create(String[] args) {
        String name = null;
        String token = null;
        String pkg = null;
        String cls = null;
        int uid = -1;

        for (String arg : args) {
            if (arg.startsWith("--debug-name=")) {
                name = arg.substring(13);
            } else if (arg.startsWith("--token=")) {
                token = arg.substring(8);
            } else if (arg.startsWith("--package=")) {
                pkg = arg.substring(10);
            } else if (arg.startsWith("--class=")) {
                cls = arg.substring(8);
            } else if (arg.startsWith("--uid=")) {
                uid = Integer.parseInt(arg.substring(6));
            }
        }

        int userId = uid / 100000;

        Log.i(TAG, String.format("starting service %s/%s...", pkg, cls));

        IBinder service;

        try {
            ActivityThread activityThread = ActivityThread.systemMain();
            Context systemContext = activityThread.getSystemContext();

            DdmHandleAppName.setAppName(name != null ? name : pkg + ":user_service", userId);

            //noinspection InstantiationOfUtilityClass
            UserHandle userHandle = Refine.unsafeCast(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            ? UserHandleHidden.of(userId)
                            : new UserHandleHidden(userId));
            Context context = Refine.<ContextHidden>unsafeCast(systemContext).createPackageContextAsUser(pkg, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY, userHandle);

            // Shizuku 13.6 started handing the user service a real Application instead of a
            // plain Context, which means LoadedApk.makeApplication() now runs inside a process
            // that was forked by Shizuku rather than bound by ActivityManagerService. Some ROMs
            // inject code there that assumes a normal app process and throws: MediaTek's
            // ResOptExt preload dereferences Application.getProcessName(), which is null when
            // there is no AppBindData (MediaTek devices, Shizuku #1198), and a per-domain
            // network security config conflicts in NetworkSecurityConfigProvider
            // .handleNewApplication (Shizuku #1171). Both throw out of the same call, so keep
            // the Application when we can get it and otherwise fall back to the pre-13.6
            // Context-only path rather than losing the whole user service.
            Application application = null;
            try {
                Field mPackageInfo = context.getClass().getDeclaredField("mPackageInfo");
                mPackageInfo.setAccessible(true);
                Object loadedApk = mPackageInfo.get(context);
                Method makeApplication = loadedApk.getClass().getDeclaredMethod("makeApplication", boolean.class, Instrumentation.class);
                application = (Application) makeApplication.invoke(loadedApk, true, null);
                Field mInitialApplication = activityThread.getClass().getDeclaredField("mInitialApplication");
                mInitialApplication.setAccessible(true);
                mInitialApplication.set(activityThread, application);
            } catch (Throwable tr) {
                Log.w(TAG, String.format("makeApplication failed for %s/%s, falling back to Context-only user service", pkg, cls), tr);
                application = null;
            }

            ClassLoader classLoader = application != null ? application.getClassLoader() : context.getClassLoader();
            Class<?> serviceClass = classLoader.loadClass(cls);
            Constructor<?> constructorWithContext = null;
            try {
                constructorWithContext = serviceClass.getConstructor(Context.class);
            } catch (NoSuchMethodException | SecurityException ignored) {
            }
            if (constructorWithContext != null) {
                service = (IBinder) constructorWithContext.newInstance(application != null ? application : context);
            } else {
                service = (IBinder) serviceClass.newInstance();
            }
        } catch (Throwable tr) {
            Log.w(TAG, String.format("unable to start service %s/%s...", pkg, cls), tr);
            return null;
        }

        return new Pair<>(service, token);
    }
}
