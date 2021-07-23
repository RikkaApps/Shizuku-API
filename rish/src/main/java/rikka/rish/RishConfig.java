package rikka.rish;

import android.os.IBinder;
import android.util.Log;

public class RishConfig {

    static {
        System.loadLibrary("rish");
    }

    private static final String TAG = "RISHConfig";

    static final int TRANSACTION_createHost = 0;
    static final int TRANSACTION_setWindowSize = 1;
    static final int TRANSACTION_getExitCode = 2;

    private static IBinder binder;
    private static String interfaceToken;
    private static int transactionCodeStart;

    static IBinder getBinder() {
        return binder;
    }

    static String getInterfaceToken() {
        return interfaceToken;
    }

    static int getTransactionCode(int code) {
        return transactionCodeStart + code;
    }

    public static void init(String interfaceToken, int transactionCodeStart) {
        Log.d(TAG, "init (server) " + interfaceToken + " " + transactionCodeStart);
        RishConfig.interfaceToken = interfaceToken;
        RishConfig.transactionCodeStart = transactionCodeStart;
    }

    public static void init(IBinder binder, String interfaceToken, int transactionCodeStart) {
        Log.d(TAG, "init (client) " + binder + " " + interfaceToken + " " + transactionCodeStart);
        RishConfig.binder = binder;
        RishConfig.interfaceToken = interfaceToken;
        RishConfig.transactionCodeStart = transactionCodeStart;
    }
}
