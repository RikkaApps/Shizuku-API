package rikka.bsh;

import android.util.Log;

import java.util.Arrays;

import rikka.shizuku.shared.BuildConfig;

public class BSH {

    private static final String TAG = "BSH";

    public void requestPermission(Runnable onGrantedRunnable) {

    }

    private void startShell(String[] args, boolean permissionGranted) {
        if (!permissionGranted) {
            requestPermission(() -> startShell(args, true));
            return;
        }

        startShell(args);
    }

    private void startShell(String[] args) {
        BSHTerminal terminal = BSHTerminal.create(args);
        try {
            if (terminal != null) {
                terminal.start();
                int exitCode = terminal.waitFor();
                //verbose("Shell exited with " + exitCode);
                System.exit(exitCode);
                System.exit(0);
            }
        } catch (Throwable e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            //abort(e.getMessage());
        }
    }

    public void main(String[] args) {
        Log.d(TAG, "bsh: "  + Arrays.toString(args));
        startShell(args, false);
    }
}
