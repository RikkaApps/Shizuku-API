package rikka.shizuku;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ShizukuServiceConnections {

    private static final Map<String, ShizukuServiceConnection> CACHE = Collections.synchronizedMap(new HashMap<>());

    @Nullable
    static ShizukuServiceConnection get(Shizuku.UserServiceArgs args) {
        String key = args.tag != null ? args.tag : args.componentName.getClassName();
        return CACHE.get(key);
    }

    @NonNull
    static ShizukuServiceConnection getOrCreate(Shizuku.UserServiceArgs args) {
        String key = args.tag != null ? args.tag : args.componentName.getClassName();
        ShizukuServiceConnection connection = CACHE.get(key);

        if (connection == null) {
            connection = new ShizukuServiceConnection(args);
            CACHE.put(key, connection);
        }
        return connection;
    }
}
