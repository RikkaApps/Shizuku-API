package rikka.shizuku.demo.service;

import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import rikka.shizuku.demo.IUserService;

public class UserService extends IUserService.Stub {

    /**
     * Constructor is required.
     */
    public UserService() {
    }

    /**
     * Reserved destroy method
     */
    @Override
    public void destroy() {
        Log.i("UserService", "destroy");
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    @Override
    public String doSomething() throws RemoteException {
        return "pid=" + Os.getpid() + ", uid=" + Os.getuid() + ", " + stringFromJNI();
    }

    static {
        System.loadLibrary("hello-jni");
    }

    public static native String stringFromJNI();
}
