package android.os;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(UserHandle.class)
public class UserHandleHidden {
    public static UserHandleHidden ALL;

    public static UserHandleHidden of(int userId) {
        throw new RuntimeException();
    }
}
