package rikka.rish;

import android.util.Log;

import java.util.Arrays;

public class Rish {

    private static final String TAG = "RISH";

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
            RishTerminal terminal = new RishTerminal(args);
            terminal.start();
            int exitCode = terminal.waitFor();
            System.exit(exitCode);
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            System.err.flush();
            System.exit(1);
            //abort(e.getMessage());
        }
    }

    public void start(String[] args) {
        Log.d(TAG, "args: " + Arrays.toString(args));
        startShell(args, false);
    }
}
