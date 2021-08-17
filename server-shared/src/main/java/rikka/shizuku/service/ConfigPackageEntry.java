package rikka.shizuku.service;

import java.util.ArrayList;
import java.util.List;

public class ConfigPackageEntry {

    public final int uid;

    public int flags;

    public List<String> packages;

    public ConfigPackageEntry(int uid, int flags) {
        this.uid = uid;
        this.flags = flags;
        this.packages = new ArrayList<>();
    }

    public boolean isAllowed() {
        return (flags & ConfigManager.FLAG_ALLOWED) != 0;
    }

    public boolean isDenied() {
        return (flags & ConfigManager.FLAG_DENIED) != 0;
    }
}
