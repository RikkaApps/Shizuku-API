package $android.app;

import android.content.IContentProvider;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IActivityManager extends IInterface {


    ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token)
            throws RemoteException;

    class ContentProviderHolder {

        public IContentProvider provider;
    }
}
