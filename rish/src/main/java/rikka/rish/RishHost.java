package rikka.rish;

import android.os.ParcelFileDescriptor;
import android.util.Log;

public class RishHost {

    private static final String TAG = "RishHost";

    // libcore/ojluni/src/main/java/java/lang/ProcessImpl.java

    private static byte[] createCBytesForStringArray(String[] array) {
        if (array == null) {
            return null;
        }

        byte[][] bytes = new byte[array.length][];
        int count = bytes.length; // For added NUL bytes
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = array[i].getBytes();
            count += bytes[i].length;
        }
        byte[] block = new byte[count];
        int i = 0;
        for (byte[] arg : bytes) {
            System.arraycopy(arg, 0, block, i, arg.length);
            i += arg.length + 1;
            // No need to write NUL bytes explicitly
        }
        return block;
    }

    private static byte[] createCBytesForString(String s) {
        if (s == null) {
            return null;
        }

        byte[] bytes = s.getBytes();
        byte[] result = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0,
                result, 0,
                bytes.length);
        result[result.length - 1] = (byte) 0;
        return result;
    }

    private static int detachFd(ParcelFileDescriptor pfd) {
        if (pfd == null) {
            return -1;
        }
        return pfd.detachFd();
    }

    private final String[] args;
    private final String[] env;
    private final String dir;
    private final byte tty;
    private final int stdin;
    private final int stdout;
    private final int stderr;
    private int pid;
    private int ptmx;
    private int exitCode = Integer.MAX_VALUE;

    public RishHost(
            String[] args, String[] env, String dir,
            byte tty,
            ParcelFileDescriptor stdin, ParcelFileDescriptor stdout, ParcelFileDescriptor stderr) {

        this.args = args;
        this.env = env;
        this.dir = dir;
        this.tty = tty;
        this.stdin = detachFd(stdin);
        this.stdout = detachFd(stdout);
        this.stderr = detachFd(stderr);
    }

    /**
     * Fork and execute, start transfer threads.
     */
    public void start() {
        Log.d(TAG, "start");


        byte[] argBlock = createCBytesForStringArray(args);
        byte[] envBlock = createCBytesForStringArray(env);
        byte[] dirBlock = createCBytesForString(dir);

        int[] result = start(
                argBlock, args.length,
                envBlock, env != null ? env.length : -1,
                dirBlock,
                tty, stdin, stdout, stderr);

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

    private static native int[] start(
            byte[] argBlock, int argc,
            byte[] envBlock, int envc,
            byte[] dirBlock,
            byte tty, int stdin, int stdout, int stderr);

    private static native void setWindowSize(int ptmx, long size);

    private static native int waitFor(int pid);
}
