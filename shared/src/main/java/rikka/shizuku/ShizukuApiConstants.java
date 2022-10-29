package rikka.shizuku;

import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

public class ShizukuApiConstants {

    public static final int SERVER_VERSION = 13;

    // binder
    public static final String BINDER_DESCRIPTOR = "moe.shizuku.server.IShizukuService";
    public static final int BINDER_TRANSACTION_transact = 1;

    // user service
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int USER_SERVICE_TRANSACTION_destroy = 16777115;

    public static final String USER_SERVICE_ARG_TAG = "shizuku:user-service-arg-tag";
    public static final String USER_SERVICE_ARG_COMPONENT = "shizuku:user-service-arg-component";
    public static final String USER_SERVICE_ARG_DEBUGGABLE = "shizuku:user-service-arg-debuggable";
    public static final String USER_SERVICE_ARG_VERSION_CODE = "shizuku:user-service-arg-version-code";
    public static final String USER_SERVICE_ARG_PROCESS_NAME = "shizuku:user-service-arg-process-name";
    public static final String USER_SERVICE_ARG_NO_CREATE = "shizuku:user-service-arg-no-create";
    public static final String USER_SERVICE_ARG_DAEMON = "shizuku:user-service-arg-daemon";
    public static final String USER_SERVICE_ARG_USE_32_BIT_APP_PROCESS = "shizuku:user-service-arg-use-32-bit-app-process";

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final String USER_SERVICE_ARG_TOKEN = "shizuku:user-service-arg-token";

    // bind application
    public static final String BIND_APPLICATION_SERVER_VERSION = "shizuku:attach-reply-version";
    public static final String BIND_APPLICATION_SERVER_PATCH_VERSION = "shizuku:attach-reply-patch-version";
    public static final String BIND_APPLICATION_SERVER_UID = "shizuku:attach-reply-uid";
    public static final String BIND_APPLICATION_SERVER_SECONTEXT = "shizuku:attach-reply-secontext";
    public static final String BIND_APPLICATION_PERMISSION_GRANTED = "shizuku:attach-reply-permission-granted";
    public static final String BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE = "shizuku:attach-reply-should-show-request-permission-rationale";

    // request permission
    public static final String REQUEST_PERMISSION_REPLY_ALLOWED = "shizuku:request-permission-reply-allowed";
    public static final String REQUEST_PERMISSION_REPLY_IS_ONETIME = "shizuku:request-permission-reply-is-onetime";

    // attach application
    public static final String ATTACH_APPLICATION_PACKAGE_NAME = "shizuku:attach-package-name";
    public static final String ATTACH_APPLICATION_API_VERSION = "shizuku:attach-api-version";
}
