package rikka.shizuku;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_APPLICATION_API_VERSION;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_APPLICATION_PACKAGE_NAME;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_PERMISSION_GRANTED;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_PATCH_VERSION;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_SECONTEXT;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_UID;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_VERSION;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import moe.shizuku.server.IShizukuApplication;
import moe.shizuku.server.IShizukuService;

public class Shizuku {

    private static IBinder binder;
    private static IShizukuService service;

    private static int serverUid = -1;
    private static int serverApiVersion = -1;
    private static int serverPatchVersion = -1;
    private static String serverContext = null;
    private static boolean permissionGranted = false;
    private static boolean shouldShowRequestPermissionRationale = false;
    private static boolean preV11 = false;
    private static boolean binderReady = false;

    private static final IShizukuApplication SHIZUKU_APPLICATION = new IShizukuApplication.Stub() {

        @Override
        public void bindApplication(Bundle data) {
            serverUid = data.getInt(BIND_APPLICATION_SERVER_UID, -1);
            serverApiVersion = data.getInt(BIND_APPLICATION_SERVER_VERSION, -1);
            serverPatchVersion = data.getInt(BIND_APPLICATION_SERVER_PATCH_VERSION, -1);
            serverContext = data.getString(BIND_APPLICATION_SERVER_SECONTEXT);
            permissionGranted = data.getBoolean(BIND_APPLICATION_PERMISSION_GRANTED, false);
            shouldShowRequestPermissionRationale = data.getBoolean(BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE, false);

            scheduleBinderReceivedListeners();
        }

        @Override
        public void dispatchRequestPermissionResult(int requestCode, Bundle data) {
            boolean allowed = data.getBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, false);
            scheduleRequestPermissionResultListener(requestCode, allowed ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED);
        }

        @Override
        public void showPermissionConfirmation(int requestUid, int requestPid, String requestPackageName, int requestCode) {
            // non-app
        }
    };

    private static final IBinder.DeathRecipient DEATH_RECIPIENT = () -> {
        binderReady = false;
        onBinderReceived(null, null);
    };

    private static boolean attachApplicationV13(IBinder binder, String packageName) throws RemoteException {
        boolean result;

        Bundle args = new Bundle();
        args.putInt(ATTACH_APPLICATION_API_VERSION, ShizukuApiConstants.SERVER_VERSION);
        args.putString(ATTACH_APPLICATION_PACKAGE_NAME, packageName);

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("moe.shizuku.server.IShizukuService");
            data.writeStrongBinder(SHIZUKU_APPLICATION.asBinder());
            data.writeInt(1);
            args.writeToParcel(data, 0);
            result = binder.transact(18 /*IShizukuService.Stub.TRANSACTION_attachApplication*/, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }

        return result;
    }

    private static boolean attachApplicationV11(IBinder binder, String packageName) throws RemoteException {
        boolean result;

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("moe.shizuku.server.IShizukuService");
            data.writeStrongBinder(SHIZUKU_APPLICATION.asBinder());
            data.writeString(packageName);
            result = binder.transact(14 /*IShizukuService.Stub.TRANSACTION_attachApplication*/, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }

        return result;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void onBinderReceived(@Nullable IBinder newBinder, String packageName) {
        if (binder == newBinder) return;

        if (newBinder == null) {
            binder = null;
            service = null;
            serverUid = -1;
            serverApiVersion = -1;
            serverContext = null;

            scheduleBinderDeadListeners();
        } else {
            if (binder != null) {
                binder.unlinkToDeath(DEATH_RECIPIENT, 0);
            }
            binder = newBinder;
            service = IShizukuService.Stub.asInterface(newBinder);

            try {
                binder.linkToDeath(DEATH_RECIPIENT, 0);
            } catch (Throwable e) {
                Log.i("ShizukuApplication", "attachApplication");
            }

            try {
                if (!attachApplicationV13(binder, packageName) && !attachApplicationV11(binder, packageName)) {
                    preV11 = true;
                }
                Log.i("ShizukuApplication", "attachApplication");
            } catch (Throwable e) {
                Log.w("ShizukuApplication", Log.getStackTraceString(e));
            }

            if (preV11) {
                binderReady = true;
                scheduleBinderReceivedListeners();
            }
        }
    }

    public interface OnBinderReceivedListener {
        void onBinderReceived();
    }

    public interface OnBinderDeadListener {
        void onBinderDead();
    }

    public interface OnRequestPermissionResultListener {

        /**
         * Callback for the result from requesting permission.
         *
         * @param requestCode The code passed in {@link #requestPermission(int)}.
         * @param grantResult The grant result for which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
         *                    or {@link android.content.pm.PackageManager#PERMISSION_DENIED}.
         */
        void onRequestPermissionResult(int requestCode, int grantResult);
    }

    private static final List<OnBinderReceivedListener> RECEIVED_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<OnBinderDeadListener> DEAD_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<OnRequestPermissionResultListener> PERMISSION_LISTENERS = new CopyOnWriteArrayList<>();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Add a listener that will be called when binder is received.
     * <p>
     * Shizuku APIs can only be used when the binder is received, or a
     * {@link IllegalStateException} will be thrown.
     *
     * <p>Note:</p>
     * <ul>
     * <li>The listener will be called in main thread.</li>
     * <li>The listener could be called multiply times. For example, user restarts Shizuku when app is running.</li>
     * </ul>
     * <p>
     *
     * @param listener OnBinderReceivedListener
     */
    public static void addBinderReceivedListener(@NonNull OnBinderReceivedListener listener) {
        addBinderReceivedListener(Objects.requireNonNull(listener), false);
    }

    /**
     * Same to {@link #addBinderReceivedListener(OnBinderReceivedListener)} but only call the listener
     * immediately if the binder is already received.
     *
     * @param listener OnBinderReceivedListener
     */
    public static void addBinderReceivedListenerSticky(@NonNull OnBinderReceivedListener listener) {
        addBinderReceivedListener(Objects.requireNonNull(listener), true);
    }

    private static void addBinderReceivedListener(@NonNull OnBinderReceivedListener listener, boolean sticky) {
        if (sticky && binderReady) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                listener.onBinderReceived();
            } else {
                MAIN_HANDLER.post(listener::onBinderReceived);
            }
        }
        RECEIVED_LISTENERS.add(listener);
    }

    /**
     * Remove the listener added by {@link #addBinderReceivedListener(OnBinderReceivedListener)}
     * or {@link #addBinderReceivedListenerSticky(OnBinderReceivedListener)}.
     *
     * @param listener OnBinderReceivedListener
     * @return If the listener is removed.
     */
    public static boolean removeBinderReceivedListener(@NonNull OnBinderReceivedListener listener) {
        return RECEIVED_LISTENERS.remove(listener);
    }

    private static void scheduleBinderReceivedListeners() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dispatchBinderReceivedListeners();
        } else {
            MAIN_HANDLER.post(Shizuku::dispatchBinderReceivedListeners);
        }
    }

    private static void dispatchBinderReceivedListeners() {
        for (OnBinderReceivedListener listener : RECEIVED_LISTENERS) {
            listener.onBinderReceived();
        }

        binderReady = true;
    }

    /**
     * Add a listener that will be called when binder is dead.
     * <p>Note:</p>
     * <ul>
     * <li>The listener will be called in main thread.</li>
     * </ul>
     * <p>
     *
     * @param listener OnBinderReceivedListener
     */
    public static void addBinderDeadListener(@NonNull OnBinderDeadListener listener) {
        DEAD_LISTENERS.add(listener);
    }

    /**
     * Remove the listener added by {@link #addBinderDeadListener(OnBinderDeadListener)}.
     *
     * @param listener OnBinderDeadListener
     * @return If the listener is removed.
     */
    public static boolean removeBinderDeadListener(@NonNull OnBinderDeadListener listener) {
        return DEAD_LISTENERS.remove(listener);
    }

    private static void scheduleBinderDeadListeners() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dispatchBinderDeadListeners();
        } else {
            MAIN_HANDLER.post(Shizuku::dispatchBinderDeadListeners);
        }
    }

    private static void dispatchBinderDeadListeners() {
        for (OnBinderDeadListener listener : DEAD_LISTENERS) {
            listener.onBinderDead();
        }
    }

    /**
     * Add a listener to receive the result of {@link #requestPermission(int)}.
     * <p>Note:</p>
     * <ul>
     * <li>The listener will be called in main thread.</li>
     * </ul>
     * <p>
     *
     * @param listener OnBinderReceivedListener
     */
    public static void addRequestPermissionResultListener(@NonNull OnRequestPermissionResultListener listener) {
        PERMISSION_LISTENERS.add(listener);
    }

    /**
     * Remove the listener added by {@link #addRequestPermissionResultListener(OnRequestPermissionResultListener)}.
     *
     * @param listener OnRequestPermissionResultListener
     * @return If the listener is removed.
     */
    public static boolean removeRequestPermissionResultListener(@NonNull OnRequestPermissionResultListener listener) {
        return PERMISSION_LISTENERS.remove(listener);
    }

    static void scheduleRequestPermissionResultListener(int requestCode, int result) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dispatchRequestPermissionResultListener(requestCode, result);
        } else {
            MAIN_HANDLER.post(() -> dispatchRequestPermissionResultListener(requestCode, result));
        }
    }

    static void dispatchRequestPermissionResultListener(int requestCode, int result) {
        for (OnRequestPermissionResultListener listener : PERMISSION_LISTENERS) {
            listener.onRequestPermissionResult(requestCode, result);
        }
    }

    @NonNull
    protected static IShizukuService requireService() {
        if (service == null) {
            throw new IllegalStateException("binder haven't been received");
        }
        return service;
    }

    /**
     * Get the binder.
     * <p>
     * Normal apps should not use this method.
     */
    @Nullable
    public static IBinder getBinder() {
        return binder;
    }

    /**
     * Check if the binder is alive.
     * <p>
     * Normal apps should use listeners rather calling this method everytime.
     *
     * @see #addBinderReceivedListener(OnBinderReceivedListener)
     * @see #addBinderReceivedListener(OnBinderReceivedListener, boolean)
     * @see #addBinderDeadListener(OnBinderDeadListener)
     */
    public static boolean pingBinder() {
        return binder != null && binder.pingBinder();
    }

    private static RuntimeException rethrowAsRuntimeException(RemoteException e) {
        return new RuntimeException(e);
    }

    /**
     * Call {@link IBinder#transact(int, Parcel, Parcel, int)} at remote service.
     * <p>
     * Use {@link ShizukuBinderWrapper} to wrap the original binder.
     *
     * @see ShizukuBinderWrapper
     */
    public static void transactRemote(@NonNull Parcel data, @Nullable Parcel reply, int flags) {
        try {
            requireService().asBinder().transact(ShizukuApiConstants.BINDER_TRANSACTION_transact, data, reply, flags);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    /**
     * Start a new process at remote service, parameters are passed to {@link Runtime#exec(String, String[], java.io.File)}.
     * <br>From version 11, like "su", the process will be killed when the caller process is dead. If you have complicated
     * requirements, use {@link Shizuku#bindUserService(UserServiceArgs, ServiceConnection)}.
     * <p>
     * Note, you may need to read/write streams from RemoteProcess in different threads.
     * </p>
     *
     * @return RemoteProcess holds the binder of remote process
     * @deprecated This method should only be used when you are transitioning from "su".
     * Use {@link Shizuku#transactRemote(Parcel, Parcel, int)} for binder calls and {@link Shizuku#bindUserService(UserServiceArgs, ServiceConnection)}
     * for complicated requirements.
     * <p>This method is planned to be removed from Shizuku API 14.
     */
    public static ShizukuRemoteProcess newProcess(@NonNull String[] cmd, @Nullable String[] env, @Nullable String dir) {
        try {
            return new ShizukuRemoteProcess(requireService().newProcess(cmd, env, dir));
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    /**
     * Returns uid of remote service.
     *
     * @return uid
     * @throws IllegalStateException if called before binder is received
     */
    public static int getUid() {
        if (serverUid != -1) return serverUid;
        try {
            serverUid = requireService().getUid();
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        } catch (SecurityException e) {
            // Shizuku pre-v11 and permission is not granted
            return -1;
        }
        return serverUid;
    }

    /**
     * Returns remote service version.
     *
     * @return server version
     */
    public static int getVersion() {
        if (serverApiVersion != -1) return serverApiVersion;
        try {
            serverApiVersion = requireService().getVersion();
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        } catch (SecurityException e) {
            // Shizuku pre-v11 and permission is not granted
            return -1;
        }
        return serverApiVersion;
    }

    /**
     * Returns if the remote service version belows 11.
     *
     * @return If the remote service version belows 11
     */
    public static boolean isPreV11() {
        return preV11;
    }

    /**
     * Return latest service version when this library was released.
     *
     * @return Latest service version
     * @see Shizuku#getVersion()
     */
    public static int getLatestServiceVersion() {
        return ShizukuApiConstants.SERVER_VERSION;
    }

    /**
     * Returns SELinux context of Shizuku server process.
     *
     * <p>For adb, context should always be <code>u:r:shell:s0</code>.
     * <br>For root, context depends on su the user uses. E.g., context of Magisk is <code>u:r:magisk:s0</code>.
     * If the user's su does not allow binder calls between su and app, Shizuku will switch to context <code>u:r:shell:s0</code>.
     * </p>
     *
     * @return SELinux context
     * @since Added from version 6
     */
    public static String getSELinuxContext() {
        if (serverContext != null) return serverContext;
        try {
            serverContext = requireService().getSELinuxContext();
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        } catch (SecurityException e) {
            // Shizuku pre-v11 and permission is not granted
            return null;
        }
        return serverContext;
    }

    public static class UserServiceArgs {

        final ComponentName componentName;
        int versionCode = 1;
        String processName;
        String tag;
        boolean debuggable = false;
        boolean daemon = true;
        boolean use32BitAppProcess = false;

        public UserServiceArgs(@NonNull ComponentName componentName) {
            this.componentName = componentName;
        }

        /**
         * Daemon controls if the service should be run as daemon mode.
         * <br>Under non-daemon mode, the service will be stopped when the app process is dead.
         * <br>Under daemon mode, the service will run forever until {@link Shizuku#unbindUserService(UserServiceArgs, ServiceConnection, boolean)} is called.
         * <p>For upward compatibility reason, {@code daemon} is {@code true} by default.
         *
         * @param daemon Daemon
         */
        public UserServiceArgs daemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        /**
         * Tag is used to distinguish different services.
         * <p>If you want to obfuscate the user service class, you need to set a stable tag.
         * <p>By default, user service is shared by the same packages installed in all users.
         *
         * @param tag Tag
         */
        public UserServiceArgs tag(@NonNull String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Version code is used to distinguish different services.
         * <p>Use a different version code when the service code is updated, so that
         * the Shizuku or Sui server can recreate the user service for you.
         *
         * @param versionCode Version code
         */
        public UserServiceArgs version(int versionCode) {
            this.versionCode = versionCode;
            return this;
        }

        /**
         * Set if the service is debuggable. The process can be found when "Show all processes" is enabled.
         *
         * @param debuggable Debuggable
         */
        public UserServiceArgs debuggable(boolean debuggable) {
            this.debuggable = debuggable;
            return this;
        }

        /**
         * Set if the name suffix of the user service process. The final process name will like
         * <code>com.example:suffix</code>.
         *
         * @param processNameSuffix Name suffix
         */
        public UserServiceArgs processNameSuffix(String processNameSuffix) {
            this.processName = processNameSuffix;
            return this;
        }

        /**
         * Set if the 32-bits app_process should be used on 64-bits devices.
         * <p>This method will not work on 64-bits only devices.
         * <p>You should NEVER use this method unless if you have special requirements.
         * <p><strong>Reasons:</strong>
         * <p><a href="https://developer.android.com/distribute/best-practices/develop/64-bit">Google has required since August 2019 that all apps submitted to Google Play are 64-bit.</a>
         * <p><a href="https://www.arm.com/blogs/blueprint/64-bit">ARM announced that all Arm Cortex-A CPU mobile cores will be 64-bit only from 2023.</a>
         *
         * @param use32BitAppProcess Use 32bit app_process
         */
        private UserServiceArgs use32BitAppProcess(boolean use32BitAppProcess) {
            this.use32BitAppProcess = use32BitAppProcess;
            return this;
        }

        private Bundle forAdd() {
            Bundle options = new Bundle();
            options.putParcelable(ShizukuApiConstants.USER_SERVICE_ARG_COMPONENT, componentName);
            options.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_DEBUGGABLE, debuggable);
            options.putInt(ShizukuApiConstants.USER_SERVICE_ARG_VERSION_CODE, versionCode);
            options.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_DAEMON, daemon);
            options.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_USE_32_BIT_APP_PROCESS, use32BitAppProcess);
            options.putString(ShizukuApiConstants.USER_SERVICE_ARG_PROCESS_NAME,
                    Objects.requireNonNull(processName, "process name suffix must not be null"));
            if (tag != null) {
                options.putString(ShizukuApiConstants.USER_SERVICE_ARG_TAG, tag);
            }
            return options;
        }

        private Bundle forRemove() {
            Bundle options = new Bundle();
            options.putParcelable(ShizukuApiConstants.USER_SERVICE_ARG_COMPONENT, componentName);
            if (tag != null) {
                options.putString(ShizukuApiConstants.USER_SERVICE_ARG_TAG, tag);
            }
            return options;
        }
    }

    /**
     * User Service is similar to <a href="https://developer.android.com/guide/components/bound-services">Bound Services</a>.
     * The difference is that the service runs in a different process and as
     * the identity (Linux UID) of root (UID 0) or shell (UID 2000, if the
     * backend is Shizuku and user starts Shizuku with adb).
     * <p>
     * The user service can run under "Daemon mode".
     * Under "Daemon mode" (default behavior), the service will run forever
     * until you call the "unbind" method. Under "Non-daemon mode", the service
     * will be stopped when the process which called the "bind" method is dead.
     * <p>
     * When the "unbind" method is called, the user service will NOT be killed.
     * You need to implement a "destroy" method in your service. The transaction
     * code for that method is {@code 16777115} (use {@code 16777114} in aidl).
     * In this method, you can do some cleanup jobs and call
     * {@link System#exit(int)} in the end.
     * <p>
     * If the backend is Shizuku, whether in daemon mode or not, user service
     * will be killed when Shizuku service is stopped or restarted.
     * Shizuku sends binder to all Shizuku apps. Therefore, you only need to
     * start the user service again.
     * <p>
     * <b>Use Android APIs in user service:</b>
     * <p>
     * There is no restrictions on non-SDK APIs in user service process.
     * However, it is not an valid Android application process. Therefore,
     * even you can acquire an {@code Context} instance, many APIs, such as
     * {@code Context#registerReceiver} and {@code Context#getContentResolver}
     * will not work. You will need to dig into Android source code to find
     * out how things works, so that you will be able to implement your service
     * safely and elegantly.
     *
     * @see UserServiceArgs
     * @since Added from version 10
     */
    public static void bindUserService(@NonNull UserServiceArgs args, @NonNull ServiceConnection conn) {
        ShizukuServiceConnection connection = ShizukuServiceConnections.get(args);
        connection.addConnection(conn);
        try {
            requireService().addUserService(connection, args.forAdd());
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    /**
     * Similar to {@link Shizuku#bindUserService(UserServiceArgs, ServiceConnection)},
     * but does not start user service if it is not running.
     *
     * @return if the service is running
     * @see Shizuku#bindUserService(UserServiceArgs, ServiceConnection)
     * @since Added from version 12
     */
    public static boolean peekUserService(@NonNull UserServiceArgs args, @NonNull ServiceConnection conn) {
        ShizukuServiceConnection connection = ShizukuServiceConnections.get(args);
        connection.addConnection(conn);
        int result;
        try {
            Bundle bundle = args.forAdd();
            bundle.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_NO_CREATE, true);
            result = requireService().addUserService(connection, bundle);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
        return result == 0;
    }

    /**
     * Remove user service.
     * <p>
     * You need to implement a "destroy" method in your service,
     * or the service will not be killed.
     *
     * @param remove Remove (kill) the remote user service.
     * @see Shizuku#bindUserService(UserServiceArgs, ServiceConnection)
     */
    public static void unbindUserService(@NonNull UserServiceArgs args, @Nullable ServiceConnection conn, boolean remove) {
        if (remove) {
            try {
                requireService().removeUserService(null /* (unused) */, args.forRemove());
            } catch (RemoteException e) {
                throw rethrowAsRuntimeException(e);
            }
        } else {
            ShizukuServiceConnection connection = ShizukuServiceConnections.get(args);
            ShizukuServiceConnections.remove(connection);
        }
    }

    /**
     * Check if remote service has specific permission.
     *
     * @param permission permission name
     * @return PackageManager.PERMISSION_DENIED or PackageManager.PERMISSION_GRANTED
     */
    public static int checkRemotePermission(String permission) {
        if (serverUid == 0) return PackageManager.PERMISSION_GRANTED;
        try {
            return requireService().checkPermission(permission);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    /**
     * Request permission.
     * <p>
     * Different from runtime permission, you need to add a listener to receive
     * the result.
     *
     * @param requestCode Application specific request code to match with a result
     *                    reported to {@link OnRequestPermissionResultListener#onRequestPermissionResult(int, int)}.
     * @see #addRequestPermissionResultListener(OnRequestPermissionResultListener)
     * @see #removeRequestPermissionResultListener(OnRequestPermissionResultListener)
     * @since Added from version 11
     */
    public static void requestPermission(int requestCode) {
        try {
            requireService().requestPermission(requestCode);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    /**
     * Check if self has permission.
     *
     * @return Either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     * or {@link android.content.pm.PackageManager#PERMISSION_DENIED}.
     * @since Added from version 11
     */
    public static int checkSelfPermission() {
        if (permissionGranted) return PackageManager.PERMISSION_GRANTED;
        try {
            permissionGranted = requireService().checkSelfPermission();
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
        return permissionGranted ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
    }

    /**
     * Should show UI with rationale before requesting the permission.
     *
     * @since Added from version 11
     */
    public static boolean shouldShowRequestPermissionRationale() {
        if (permissionGranted) return false;
        if (shouldShowRequestPermissionRationale) return true;
        try {
            shouldShowRequestPermissionRationale = requireService().shouldShowRequestPermissionRationale();
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
        return shouldShowRequestPermissionRationale;
    }

    // --------------------- non-app ----------------------

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void exit() {
        try {
            requireService().exit();
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void attachUserService(@NonNull IBinder binder, @NonNull Bundle options) {
        try {
            requireService().attachUserService(binder, options);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void dispatchPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, @NonNull Bundle data) {
        try {
            requireService().dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static int getFlagsForUid(int uid, int mask) {
        try {
            return requireService().getFlagsForUid(uid, mask);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void updateFlagsForUid(int uid, int mask, int value) {
        try {
            requireService().updateFlagsForUid(uid, mask, value);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static int getServerPatchVersion() {
        return serverPatchVersion;
    }

}
