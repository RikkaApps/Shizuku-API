package android.content.pm;

import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

@RequiresApi(Build.VERSION_CODES.O)
@RefineAs(IShortcutService.class)
public interface IShortcutService {

    void removeDynamicShortcuts(String packageName, List<String> shortcutIds, int userId)
            throws RemoteException;

    abstract class Stub extends Binder implements IShortcutService {

        public static IShortcutService asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
