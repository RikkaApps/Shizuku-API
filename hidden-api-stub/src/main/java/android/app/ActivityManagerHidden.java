package android.app;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(ActivityManager.class)
public class ActivityManagerHidden {

    public static int UID_OBSERVER_ACTIVE;

    public static int PROCESS_STATE_UNKNOWN;

    public static class RunningAppProcessInfo {

        public static int procStateToImportance(int procState) {
            throw new RuntimeException("STUB");
        }
    }
}
