package rikka.shizuku.demo;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.ShizukuSystemProperties;
import rikka.shizuku.demo.databinding.MainActivityBinding;
import rikka.shizuku.demo.service.UserService;
import rikka.shizuku.demo.util.ApplicationUtils;
import rikka.shizuku.demo.util.IIntentSenderAdaptor;
import rikka.shizuku.demo.util.IntentSenderUtils;
import rikka.shizuku.demo.util.PackageInstallerUtils;
import rikka.shizuku.demo.util.ShizukuSystemServerApi;

@SuppressLint("SetTextI18n")
public class DemoActivity extends Activity {

    private static final int REQUEST_CODE_BUTTON1 = 1;
    private static final int REQUEST_CODE_BUTTON2 = 2;
    private static final int REQUEST_CODE_BUTTON3 = 3;
    private static final int REQUEST_CODE_BUTTON4 = 4;
    private static final int REQUEST_CODE_BUTTON7 = 7;
    private static final int REQUEST_CODE_BUTTON8 = 8;
    private static final int REQUEST_CODE_BUTTON9 = 9;
    private static final int REQUEST_CODE_PICK_APKS = 1000;

    private MainActivityBinding binding;

    private final Shizuku.OnBinderReceivedListener BINDER_RECEIVED_LISTENER = () -> {
        if (Shizuku.isPreV11()) {
            binding.text1.setText("Shizuku pre-v11 is not supported");
        } else {
            binding.text1.setText("Binder received");
        }
    };
    private final Shizuku.OnBinderDeadListener BINDER_DEAD_LISTENER = () -> binding.text1.setText("Binder dead");
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ShizukuSample", getClass().getSimpleName() + " onCreate | Process=" + ApplicationUtils.getProcessName());

        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.text2.setText("Using " + (DemoApplication.isSui() ? "Sui" : "Shizuku or nothing is installed") + ".");

        binding.text1.setText("Waiting for binder");
        binding.button1.setOnClickListener((v) -> {
            if (checkPermission(REQUEST_CODE_BUTTON1)) getUsers();
        });
        binding.button2.setOnClickListener((v) -> {
            if (checkPermission(REQUEST_CODE_BUTTON2)) installApks();
        });
        binding.button3.setOnClickListener((v) -> {
            if (checkPermission(REQUEST_CODE_BUTTON3)) abandonMySessions();
        });
        binding.button4.setOnClickListener((v) -> {
            if (checkPermission(REQUEST_CODE_BUTTON4)) getSystemProperty();
        });
        binding.button7.setOnClickListener((v) -> {
            if (checkPermission(REQUEST_CODE_BUTTON7)) bindUserService();
        });
        binding.button8.setOnClickListener((v) -> {
            if (checkPermission(REQUEST_CODE_BUTTON8)) unbindUserService();
        });
        binding.button9.setOnClickListener((v) -> {
            if (checkPermission(REQUEST_CODE_BUTTON9)) peekUserService();
        });

        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER);
        Shizuku.addBinderDeadListener(BINDER_DEAD_LISTENER);
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Shizuku.removeBinderReceivedListener(BINDER_RECEIVED_LISTENER);
        Shizuku.removeBinderDeadListener(BINDER_DEAD_LISTENER);
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (grantResult == PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_CODE_BUTTON1: {
                    getUsers();
                    break;
                }
                case REQUEST_CODE_BUTTON2: {
                    installApks();
                    break;
                }
                case REQUEST_CODE_BUTTON3: {
                    abandonMySessions();
                    break;
                }
                case REQUEST_CODE_BUTTON4: {
                    getSystemProperty();
                    break;
                }
                case REQUEST_CODE_BUTTON7: {
                    bindUserService();
                    break;
                }
                case REQUEST_CODE_BUTTON8: {
                    unbindUserService();
                    break;
                }
                case REQUEST_CODE_BUTTON9: {
                    peekUserService();
                    break;
                }
            }
        } else {
            binding.text1.setText("User denied permission");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_APKS && resultCode == RESULT_OK) {
            List<Uri> uris;
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                uris = new ArrayList<>(clipData.getItemCount());
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) {
                        uris.add(uri);
                    }
                }
            } else {
                uris = new ArrayList<>();
                uris.add(data.getData());
            }
            doInstallApks(uris);
        }
    }

    private boolean checkPermission(int code) {
        if (Shizuku.isPreV11()) {
            return false;
        }
        try {
            if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
                return true;
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                binding.text3.setText("User denied permission (shouldShowRequestPermissionRationale=true)");
                return false;
            } else {
                Shizuku.requestPermission(code);
                return false;
            }
        } catch (Throwable e) {
            binding.text3.setText(Log.getStackTraceString(e));
        }

        return false;
    }

    private void getUsers() {
        String res;
        try {
            res = ShizukuSystemServerApi.UserManager_getUsers(true, true, true).toString();
        } catch (Throwable tr) {
            tr.printStackTrace();
            res = tr.getMessage();
        }
        binding.text3.setText(res);
    }

    private void installApks() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("application/vnd.android.package-archive");

        startActivityForResult(intent, REQUEST_CODE_PICK_APKS);
    }

    private void doInstallApks(List<Uri> uris) {
        PackageInstaller packageInstaller;
        PackageInstaller.Session session = null;
        ContentResolver cr = getContentResolver();
        StringBuilder res = new StringBuilder();
        String installerPackageName;
        int userId;
        boolean isRoot;

        try {
            IPackageInstaller _packageInstaller = ShizukuSystemServerApi.PackageManager_getPackageInstaller();
            isRoot = Shizuku.getUid() == 0;

            // the reason for use "com.android.shell" as installer package under adb is that getMySessions will check installer package's owner
            installerPackageName = isRoot ? getPackageName() : "com.android.shell";
            userId = isRoot ? Process.myUserHandle().hashCode() : 0;
            packageInstaller = PackageInstallerUtils.createPackageInstaller(_packageInstaller, installerPackageName, userId);

            int sessionId;
            res.append("createSession: ");

            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int installFlags = PackageInstallerUtils.getInstallFlags(params);
            installFlags |= 0x00000004/*PackageManager.INSTALL_ALLOW_TEST*/ | 0x00000002/*PackageManager.INSTALL_REPLACE_EXISTING*/;
            PackageInstallerUtils.setInstallFlags(params, installFlags);

            sessionId = packageInstaller.createSession(params);
            res.append(sessionId).append('\n');

            res.append('\n').append("write: ");

            IPackageInstallerSession _session = IPackageInstallerSession.Stub.asInterface(new ShizukuBinderWrapper(_packageInstaller.openSession(sessionId).asBinder()));
            session = PackageInstallerUtils.createSession(_session);

            int i = 0;
            for (Uri uri : uris) {
                String name = i + ".apk";

                InputStream is = cr.openInputStream(uri);
                OutputStream os = session.openWrite(name, 0, -1);

                byte[] buf = new byte[8192];
                int len;
                try {
                    while ((len = is.read(buf)) > 0) {
                        os.write(buf, 0, len);
                        os.flush();
                        session.fsync(os);
                    }
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                i++;

                Thread.sleep(1000);
            }

            res.append('\n').append("commit: ");

            Intent[] results = new Intent[]{null};
            CountDownLatch countDownLatch = new CountDownLatch(1);
            IntentSender intentSender = IntentSenderUtils.newInstance(new IIntentSenderAdaptor() {
                @Override
                public void send(Intent intent) {
                    results[0] = intent;
                    countDownLatch.countDown();
                }
            });
            session.commit(intentSender);

            countDownLatch.await();
            Intent result = results[0];
            int status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            String message = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            res.append('\n').append("status: ").append(status).append(" (").append(message).append(")");

        } catch (Throwable tr) {
            tr.printStackTrace();
            res.append(tr);
        } finally {
            if (session != null) {
                try {
                    session.close();

                } catch (Throwable tr) {
                    res.append(tr);
                }
            }
        }

        binding.text3.setText(res.toString().trim());
    }

    private void abandonMySessions() {
        StringBuilder res = new StringBuilder();
        String installer;
        int userId;
        boolean isRoot;
        IPackageInstaller packageInstaller;

        try {
            packageInstaller = ShizukuSystemServerApi.PackageManager_getPackageInstaller();
            isRoot = Shizuku.getUid() == 0;

            installer = isRoot ? getPackageName() : "com.android.shell";
            userId = isRoot ? android.os.Process.myUserHandle().hashCode() : 0;

            List<PackageInstaller.SessionInfo> sessions;
            res.append("abandonMySessions: ");
            sessions = packageInstaller.getMySessions(installer, userId).getList();
            for (PackageInstaller.SessionInfo session : sessions) {
                res.append(session.getSessionId());
                packageInstaller.abandonSession(session.getSessionId());
                res.append(" (abandoned)\n");
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            res.append(tr);
        }

        binding.text3.setText(res.toString().trim());
    }

    private void getSystemProperty() {
        StringBuilder res = new StringBuilder();
        try {
            if (Shizuku.getVersion() < 9) {
                res.append("requires Shizuku API 9");
            } else {
                res.append("ro.build.fingerprint=").append(ShizukuSystemProperties.get("ro.build.fingerprint")).append('\n');
                res.append("ro.build.version.sdk=").append(ShizukuSystemProperties.getInt("ro.build.version.sdk", -1)).append('\n');
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            res.append(tr.toString());
        }
        binding.text3.setText(res.toString().trim());
    }

    private final ServiceConnection userServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            StringBuilder res = new StringBuilder();
            res.append("onServiceConnected: ").append(componentName.getClassName()).append('\n');
            if (binder != null && binder.pingBinder()) {
                IUserService service = IUserService.Stub.asInterface(binder);
                try {
                    res.append(service.doSomething());
                } catch (RemoteException e) {
                    e.printStackTrace();
                    res.append(Log.getStackTraceString(e));
                }
            } else {
                res.append("invalid binder for ").append(componentName).append(" received");
            }
            binding.text3.setText(res.toString().trim());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binding.text3.setText("onServiceDisconnected: " + '\n' + componentName.getClassName());
        }
    };

    private final Shizuku.UserServiceArgs userServiceArgs =
            new Shizuku.UserServiceArgs(new ComponentName(BuildConfig.APPLICATION_ID, UserService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("service")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);

    private void bindUserService() {
        StringBuilder res = new StringBuilder();
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10");
            } else {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection);
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            res.append(tr.toString());
        }
        binding.text3.setText(res.toString().trim());
    }

    private void unbindUserService() {
        StringBuilder res = new StringBuilder();
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10");
            } else {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true);
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            res.append(tr.toString());
        }
        binding.text3.setText(res.toString().trim());
    }

    private void peekUserService() {
        StringBuilder res = new StringBuilder();
        try {
            if (Shizuku.getVersion() < 12) {
                res.append("requires Shizuku API 12");
            } else {
                if (Shizuku.peekUserService(userServiceArgs, userServiceConnection)) {
                    res.append("Service is running");
                } else {
                    res.append("Service is not running");
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            res.append(tr.toString());
        }
        binding.text3.setText(res.toString().trim());
    }
}
