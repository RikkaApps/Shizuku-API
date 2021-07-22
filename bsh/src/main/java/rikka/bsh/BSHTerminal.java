package rikka.bsh;

import android.os.Parcel;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.FileDescriptor;

public class BSHTerminal {

    private static final String TAG = "BSHTerminal";

    public static BSHTerminal create() {
        FileDescriptor[] stdinPipe, stdoutPipe;
        try {
            stdinPipe = Os.pipe();
        } catch (ErrnoException e) {
            return null;
        }

        try {
            stdoutPipe = Os.pipe();
        } catch (ErrnoException e) {
            return null;
        }

        return new BSHTerminal(stdinPipe, stdoutPipe);
    }

    private final FileDescriptor[] stdinPipe;
    private final FileDescriptor[] stdoutPipe;
    private int exitCode;

    private BSHTerminal(FileDescriptor[] stdinPipe, FileDescriptor[] stdoutPipe) {
        this.stdinPipe = stdinPipe;
        this.stdoutPipe = stdoutPipe;

        prepareToWaitWindowSizeChange();
    }

    private void createHost() throws RemoteException {
        Log.d(TAG, "createHost");

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();

        try {
            data.writeInterfaceToken(BSHConfig.getInterfaceToken());
            data.writeFileDescriptor(stdinPipe[0]);
            data.writeFileDescriptor(stdoutPipe[1]);
            BSHConfig.getBinder().transact(BSHConfig.getTransactionCode(BSHConfig.TRANSACTION_createHost), data, reply, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }

        try {
            Os.close(stdinPipe[0]);
        } catch (ErrnoException ignored) {
        }
        try {
            Os.close(stdoutPipe[1]);
        } catch (ErrnoException ignored) {
        }
    }

    private void setWindowSize(long size) throws RemoteException {
        Log.d(TAG, "setWindowSize");

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();

        try {
            data.writeInterfaceToken(BSHConfig.getInterfaceToken());
            data.writeLong(size);
            BSHConfig.getBinder().transact(BSHConfig.getTransactionCode(BSHConfig.TRANSACTION_setWindowSize), data, null, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private int requestExitCode() throws RemoteException {
        Log.d(TAG, "requestExitCode");

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();

        try {
            data.writeInterfaceToken(BSHConfig.getInterfaceToken());
            BSHConfig.getBinder().transact(BSHConfig.getTransactionCode(BSHConfig.TRANSACTION_getExitCode), data, null, 0);
            reply.readException();
            return reply.readInt();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void start() throws RemoteException {
        createHost();

        new Thread(() -> {
            long size = waitForWindowSizeChange();

            try {
                setWindowSize(size);
            } catch (Throwable e) {
                Log.w(TAG, Log.getStackTraceString(e));
            }
        }).start();

        Log.d(TAG, "start");

        start(FileDescriptors.getFd(stdinPipe[1]), FileDescriptors.getFd(stdoutPipe[0]));
    }

    public int waitFor() {
        Log.d(TAG, "waitFor");

        waitForProcessExit();
        try {
            exitCode = requestExitCode();
        } catch (Throwable e) {
            Log.w(TAG, Log.getStackTraceString(e));
            exitCode = -1;
        }
        return exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

    public static native void prepareToWaitWindowSizeChange();

    private static native int start(int stdin_write_pipe, int stdout_read_pipe);

    private static native long waitForWindowSizeChange();

    private static native void waitForProcessExit();
}
