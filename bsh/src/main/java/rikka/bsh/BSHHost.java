package rikka.bsh;

import android.os.ParcelFileDescriptor;
import android.util.Log;

public class BSHHost {

    private static final String TAG = "BSHHost";

    public static BSHHost create() {
        return new BSHHost();
    }

    private int stdinReadPipe;
    private int stdoutWritePipe;
    private int pid;
    private int ptmx;
    private int exitCode = Integer.MAX_VALUE;

    public void prepare(ParcelFileDescriptor stdin, ParcelFileDescriptor stdout) {
        stdinReadPipe = stdin.detachFd();
        stdoutWritePipe = stdout.detachFd();
    }

    /**
     * Fork and execute, start transfer threads.
     */
    public void start() {
        Log.d(TAG, "start");

        int[] result = start(stdinReadPipe, stdoutWritePipe);
        pid = result[0];
        ptmx = result[1];

        new Thread(() -> exitCode = waitFor(pid)).start();
    }

    public int getPid() {
        return pid;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setWindowSize(long size) {
        Log.d(TAG, "setWindowSize");

        setWindowSize(ptmx, size);
    }

    private static native int[] start(int stdin_read_pipe, int stdout_write_pipe);

    private static native void setWindowSize(int ptmx, long size);

    private static native int waitFor(int pid);
}
