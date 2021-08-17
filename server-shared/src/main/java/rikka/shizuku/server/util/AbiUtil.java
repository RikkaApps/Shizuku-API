package rikka.shizuku.server.util;

import android.os.Build;

public class AbiUtil {

    private static Boolean has32Bit;

    public static boolean has32Bit() {
        if (has32Bit == null) {
            has32Bit = Build.SUPPORTED_32_BIT_ABIS.length > 0;
        }
        return has32Bit;
    }
}
