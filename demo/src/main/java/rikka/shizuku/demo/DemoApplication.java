package rikka.shizuku.demo;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L");
        }
        ApplicationUtils.setApplication(this);

        Log.d("ShizukuSample", getClass().getSimpleName() + " attachBaseContext | Process=" + ApplicationUtils.getProcessName());
    }
}
