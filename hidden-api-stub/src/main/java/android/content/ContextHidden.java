package android.content;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.UserHandle;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Context.class)
public class ContextHidden {

    public Intent registerReceiverAsUser(
            BroadcastReceiver receiver,
            UserHandle user,
            IntentFilter filter,
            String broadcastPermission,
            Handler scheduler
    ) {
        throw new RuntimeException();
    }

    public Context createPackageContextAsUser(
            String packageName, int flags, UserHandle user)
            throws PackageManager.NameNotFoundException {
        throw new RuntimeException();
    }
}
