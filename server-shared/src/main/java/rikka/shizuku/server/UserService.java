package rikka.shizuku.server;

import android.app.ActivityThread;
import android.content.Context;
import android.content.ContextHidden;
import android.ddm.DdmHandleAppName;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserHandleHidden;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

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

        Log.i(TAG, String.format("starting service %s/%s...", pkg, cls));

        try {
            Context systemContext = ActivityThread.systemMain().getSystemContext();

            DdmHandleAppName.setAppName(name != null ? name : pkg + ":user_service", 0);

            UserHandle userHandle = Refine.unsafeCast(UserHandleHidden.of(uid / 100000));
            var flags = Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY;
            Context context = Refine.<ContextHidden>unsafeCast(systemContext)
                    .createPackageContextAsUser(pkg, flags, userHandle);
            ClassLoader classLoader = context.getClassLoader();
            Class<?> serviceClass = classLoader.loadClass(cls);
            try {
                var constructorWithContext = serviceClass.getConstructor(Context.class);
                var service = (IBinder) constructorWithContext.newInstance(context);
                return new Pair<>(service, token);
            } catch (ReflectiveOperationException ignored) {
            }
            var constructor = serviceClass.getConstructor();
            var service = (IBinder) constructor.newInstance();
            return new Pair<>(service, token);
        } catch (Throwable tr) {
            Log.e(TAG, String.format("unable to start service %s/%s...", pkg, cls), tr);
            return null;
        }
    }
}
