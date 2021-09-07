package android.os;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(UserHandle.class)
public class UserHandleHidden {
    public static UserHandle ALL;

    public static UserHandle of(int userId) {
        throw new RuntimeException();
    }
}
