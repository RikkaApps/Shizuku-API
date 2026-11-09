package rikka.shizuku.demo.util;

import android.app.ActivityThread;
import android.app.Application;
import android.os.Build;

public class ApplicationUtils {

    private static Application application;

    public static Application getApplication() {
        return application;
    }

    public static void setApplication(Application application) {
        ApplicationUtils.application = application;
    }

    public static String getProcessName() {
        if (Build.VERSION.SDK_INT >= 28)
            return Application.getProcessName();
        else {
            return ActivityThread.currentProcessName();
        }
    }
}
