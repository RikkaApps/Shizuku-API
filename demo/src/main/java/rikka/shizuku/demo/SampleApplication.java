package rikka.shizuku.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import me.weishu.reflection.Reflection;
import rikka.shizuku.demo.util.ApplicationUtils;
import rikka.sui.Sui;

public class SampleApplication extends Application {

    static {
        boolean isProviderProcess = ApplicationUtils.getProcessName().endsWith(":test");
        //ShizukuProvider.enableMultiProcessSupport(isProviderProcess);
        Sui.init(BuildConfig.APPLICATION_ID);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("ShizukuSample", getClass().getSimpleName() + " onCreate | Process=" + ApplicationUtils.getProcessName());

        //ShizukuProvider.requestBinderForNonProviderProcess(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(this); // bypass hidden api restriction, https://github.com/tiann/FreeReflection
        ApplicationUtils.setApplication(this);

        Log.d("ShizukuSample", getClass().getSimpleName() + " attachBaseContext | Process=" + ApplicationUtils.getProcessName());
    }
}
