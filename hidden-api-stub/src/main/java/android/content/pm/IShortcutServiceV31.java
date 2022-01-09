package android.content.pm;

import android.os.Build;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import com.android.internal.infra.AndroidFuture;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(IShortcutService.class)
public interface IShortcutServiceV31 {

    @RequiresApi(Build.VERSION_CODES.S)
    AndroidFuture removeDynamicShortcuts(String packageName, List<String> shortcutIds, int userId)
            throws RemoteException;
}
