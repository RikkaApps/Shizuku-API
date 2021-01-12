package rikka.shizuku;

import android.os.RemoteException;

/**
 * @since added from version 9
 */
public class ShizukuSystemProperties {

    public static String get(String key) throws RemoteException {
        return Shizuku.requireService().getSystemProperty(key, null);
    }

    public static String get(String key, String def) throws RemoteException {
        return Shizuku.requireService().getSystemProperty(key, def);
    }

    public static int getInt(String key, int def) throws RemoteException {
        return Integer.decode(Shizuku.requireService().getSystemProperty(key, Integer.toString(def)));
    }

    public static long getLong(String key, long def) throws RemoteException {
        return Long.decode(Shizuku.requireService().getSystemProperty(key, Long.toString(def)));
    }

    public static boolean getBoolean(String key, boolean def) throws RemoteException {
        return Boolean.parseBoolean(Shizuku.requireService().getSystemProperty(key, Boolean.toString(def)));
    }

    public static void set(String key, String val) throws RemoteException {
        Shizuku.requireService().setSystemProperty(key, val);
    }
}
