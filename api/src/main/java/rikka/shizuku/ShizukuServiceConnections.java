/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */

package rikka.shizuku;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ShizukuServiceConnections {

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
