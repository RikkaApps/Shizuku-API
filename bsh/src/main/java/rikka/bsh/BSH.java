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
        try {
            BSHTerminal terminal = new BSHTerminal(args);
            terminal.start();
            int exitCode = terminal.waitFor();
            System.exit(exitCode);
        } catch (Throwable e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            System.err.println(e.getMessage());
            System.err.flush();
            System.exit(1);
            //abort(e.getMessage());
        }
    }

    public void main(String[] args) {
        Log.d(TAG, "bsh: " + Arrays.toString(args));
        startShell(args, false);
    }
}
