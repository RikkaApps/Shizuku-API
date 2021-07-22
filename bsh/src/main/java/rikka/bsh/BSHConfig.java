package rikka.bsh;

import android.os.IBinder;

public class BSHConfig {

    static {
        System.loadLibrary("bsh");
    }

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
        BSHConfig.interfaceToken = interfaceToken;
        BSHConfig.transactionCodeStart = transactionCodeStart;
    }
}
