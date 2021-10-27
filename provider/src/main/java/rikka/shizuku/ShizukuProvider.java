package rikka.shizuku;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import moe.shizuku.api.BinderContainer;
import rikka.sui.Sui;

/**
 * <p>
 * This provider receives binder from Shizuku server. When app process starts,
 * Shizuku server (it runs under adb/root) will send the binder to client apps with this provider.
 * </p>
 * <p>
 * Add the provider to your manifest like this:
 * </p>
 * <pre class="prettyprint">&lt;manifest&gt;
 *    ...
 *    &lt;application&gt;
 *        ...
 *        &lt;provider
 *            android:name="rikka.shizuku.ShizukuProvider"
 *            android:authorities="${applicationId}.shizuku"
 *            android:exported="true"
 *            android:multiprocess="false"
 *            android:permission="android.permission.INTERACT_ACROSS_USERS_FULL"
 *        &lt;/provider&gt;
 *        ...
 *    &lt;/application&gt;
 * &lt;/manifest&gt;</pre>
 *
 * <p>
 * There are something needs you attention:
 * </p>
 * <ol>
 * <li><code>android:permission</code> shoule be a permission that granted to Shell (com.android.shell)
 * but not normal apps (e.g., android.permission.INTERACT_ACROSS_USERS_FULL), so that it can only
 * be used by the app itself and Shizuku server.</li>
 * <li><code>android:exported</code> must be <code>true</code> so that the provider can be accessed
 * from Shizuku server runs under adb.</li>
 * <li><code>android:multiprocess</code> must be <code>false</code>
 * since Shizuku server only gets uid when app starts.</li>
 * </ol>
 * <p>
 * If your app run in multiply processes, this provider also provides the functionality of sharing
 * the binder across processes. See {@link #enableMultiProcessSupport(boolean)}.
 * </p>
 */
public class ShizukuProvider extends ContentProvider {

    private static final String TAG = "ShizukuProvider";

    // For receive Binder from Shizuku
    public static final String METHOD_SEND_BINDER = "sendBinder";

    // For share Binder between processes
    public static final String METHOD_GET_BINDER = "getBinder";

    public static final String ACTION_BINDER_RECEIVED = "moe.shizuku.api.action.BINDER_RECEIVED";

    private static final String EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER";

    public static final String PERMISSION = "moe.shizuku.manager.permission.API_V23";

    public static final String MANAGER_APPLICATION_ID = "moe.shizuku.privileged.api";

    private static boolean enableMultiProcess = false;

    private static boolean isProviderProcess = false;

    private static boolean enableSuiInitialization = true;

    public static void setIsProviderProcess(boolean isProviderProcess) {
        ShizukuProvider.isProviderProcess = isProviderProcess;
    }

    /**
     * Enables built-in multi-process support.
     * <p>
     * This method MUST be called as early as possible (e.g., static block in Application).
     */
    public static void enableMultiProcessSupport(boolean isProviderProcess) {
        Log.d(TAG, "Enable built-in multi-process support (from " + (isProviderProcess ? "provider process" : "non-provider process") + ")");

        ShizukuProvider.isProviderProcess = isProviderProcess;
        ShizukuProvider.enableMultiProcess = true;
    }

    /**
     * Disable automatic Sui initialization.
     */
    public static void disableAutomaticSuiInitialization() {
        ShizukuProvider.enableSuiInitialization = false;
    }

    /**
     * Require binder for non-provider process, should have {@link #enableMultiProcessSupport(boolean)} called first.
     *
     * @param context Context
     */
    public static void requestBinderForNonProviderProcess(@NonNull Context context) {
        if (isProviderProcess) {
            return;
        }

        Log.d(TAG, "request binder in non-provider process");

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BinderContainer container = intent.getParcelableExtra(EXTRA_BINDER);
                if (container != null && container.binder != null) {
                    Log.i(TAG, "binder received from broadcast");
                    Shizuku.onBinderReceived(container.binder, context.getPackageName());
                }
            }
        }, new IntentFilter(ACTION_BINDER_RECEIVED));

        Bundle reply;
        try {
            reply = context.getContentResolver().call(Uri.parse("content://" + context.getPackageName() + ".shizuku"),
                    ShizukuProvider.METHOD_GET_BINDER, null, new Bundle());
        } catch (Throwable tr) {
            reply = null;
        }

        if (reply != null) {
            reply.setClassLoader(BinderContainer.class.getClassLoader());

            BinderContainer container = reply.getParcelable(EXTRA_BINDER);
            if (container != null && container.binder != null) {
                Log.i(TAG, "Binder received from other process");
                Shizuku.onBinderReceived(container.binder, context.getPackageName());
            }
        }
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);

        if (info.multiprocess)
            throw new IllegalStateException("android:multiprocess must be false");

        if (!info.exported)
            throw new IllegalStateException("android:exported must be true");

        isProviderProcess = true;
    }

    @Override
    public boolean onCreate() {
        if (enableSuiInitialization && !Sui.isSui()) {
            boolean result = Sui.init(getContext().getPackageName());
            Log.d(TAG, "Initialize Sui: " + result);
        }
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (Sui.isSui()) {
            Log.w(TAG, "Provider called when Sui is available. Are you using Shizuku and Sui at the same time?");
            return new Bundle();
        }

        if (extras == null) {
            return null;
        }

        extras.setClassLoader(BinderContainer.class.getClassLoader());

        Bundle reply = new Bundle();
        switch (method) {
            case METHOD_SEND_BINDER: {
                handleSendBinder(extras);
                break;
            }
            case METHOD_GET_BINDER: {
                if (!handleGetBinder(reply)) {
                    return null;
                }
                break;
            }
        }
        return reply;
    }

    private void handleSendBinder(@NonNull Bundle extras) {
        if (Shizuku.pingBinder()) {
            Log.d(TAG, "sendBinder is called when already a living binder");
            return;
        }

        BinderContainer container = extras.getParcelable(EXTRA_BINDER);
        if (container != null && container.binder != null) {
            Log.d(TAG, "binder received");

            Shizuku.onBinderReceived(container.binder, getContext().getPackageName());

            if (enableMultiProcess) {
                Log.d(TAG, "broadcast binder");

                Intent intent = new Intent(ACTION_BINDER_RECEIVED)
                        .putExtra(EXTRA_BINDER, container)
                        .setPackage(getContext().getPackageName());
                getContext().sendBroadcast(intent);
            }
        }
    }

    private boolean handleGetBinder(@NonNull Bundle reply) {
        // Other processes in the same app can read the provider without permission
        IBinder binder = Shizuku.getBinder();
        if (binder == null || !binder.pingBinder())
            return false;

        reply.putParcelable(EXTRA_BINDER, new BinderContainer(binder));
        return true;
    }

    // no other provider methods
    @Nullable
    @Override
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public final String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public final Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public final int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public final int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
