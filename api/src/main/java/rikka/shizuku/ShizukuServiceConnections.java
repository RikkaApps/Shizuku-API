package rikka.shizuku;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ShizukuServiceConnections {

    private static final Map<String, ShizukuServiceConnection> CACHE = Collections.synchronizedMap(new HashMap<>());

    @NonNull
    static ShizukuServiceConnection get(Shizuku.UserServiceArgs args) {
        String key = args.tag != null ? args.tag : args.componentName.getClassName();
        ShizukuServiceConnection connection = CACHE.get(key);

        if (connection == null) {
            connection = new ShizukuServiceConnection(args);
            CACHE.put(key, connection);
        }
        return connection;
    }

    static void remove(ShizukuServiceConnection connection) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, ShizukuServiceConnection> entry : CACHE.entrySet()) {
            if (entry.getValue() == connection) {
                keys.add(entry.getKey());
            }
        }
        for (String key : keys) {
            CACHE.remove(key);
        }
    }
}
