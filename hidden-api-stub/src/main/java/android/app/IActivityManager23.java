package android.app;

import android.content.IContentProvider;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(IActivityManager.class)
public interface IActivityManager23 extends IInterface {

    ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token)
            throws RemoteException;

    class ContentProviderHolder {

        public IContentProvider provider;
    }
}
