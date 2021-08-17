package rikka.shizuku.server;

import androidx.annotation.Nullable;

import java.util.List;

public abstract class ConfigManager {

    public static final int FLAG_ALLOWED = 1 << 1;
    public static final int FLAG_DENIED = 1 << 2;
    public static final int MASK_PERMISSION = FLAG_ALLOWED | FLAG_DENIED;

    @Nullable
    public abstract ConfigPackageEntry find(int uid);

    public abstract void update(int uid, List<String> packages, int mask, int values);

    public abstract void remove(int uid);
}
