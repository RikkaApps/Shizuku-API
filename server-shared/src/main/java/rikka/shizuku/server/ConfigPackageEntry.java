package rikka.shizuku.server;

public abstract class ConfigPackageEntry {

    public ConfigPackageEntry() {
    }

    public abstract boolean isAllowed();

    public abstract boolean isDenied();
}
