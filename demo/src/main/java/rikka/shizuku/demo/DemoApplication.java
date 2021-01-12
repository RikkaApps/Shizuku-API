package rikka.shizuku.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import me.weishu.reflection.Reflection;
import rikka.shizuku.ShizukuProvider;
import rikka.shizuku.demo.util.ApplicationUtils;
import rikka.sui.Sui;

public class DemoApplication extends Application {

    private static boolean isSui;

    public static boolean isSui() {
        return isSui;
    }

    static {
        isSui = Sui.init(BuildConfig.APPLICATION_ID);
        if (!isSui) {
            // If this is a multi-process application
            //ShizukuProvider.enableMultiProcessSupport( /* is current process the same process of ShizukuProvider's */ );
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("ShizukuSample", getClass().getSimpleName() + " onCreate | Process=" + ApplicationUtils.getProcessName());

        if (!isSui) {
            // If this is a multi-process application
            //ShizukuProvider.requestBinderForNonProviderProcess(this);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(this); // bypass hidden api restriction, https://github.com/tiann/FreeReflection
        ApplicationUtils.setApplication(this);

        Log.d("ShizukuSample", getClass().getSimpleName() + " attachBaseContext | Process=" + ApplicationUtils.getProcessName());
    }
}
