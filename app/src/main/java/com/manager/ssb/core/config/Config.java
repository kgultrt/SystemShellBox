/*
 * System Shell Box
 * Copyright (C) 2025-2026 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.manager.ssb.core.config;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.manager.ssb.Application;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Config {
    private static final String TAG = "Config";
    private static final String CONFIG_PATH = "config/config.json";
    private static final String ASSETS_CONFIG_PATH = "config/config.json";

    private static final AtomicReference<JsonObject> rootConfig = new AtomicReference<>(new JsonObject());
    private static final ConcurrentHashMap<String, Object> configCache = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static volatile Future<?> loadFuture = null;
    private static volatile boolean isLoaded = false;

    private static File configFile;

    private static final Object configLock = new Object();

    // 确保只真正初始化一次
    private static final AtomicBoolean initStarted = new AtomicBoolean(false);

    /**
     * 兼容旧代码的显式初始化方法，内部会调用自动初始化逻辑。
     * 现在任意位置可直接使用 Config.get/set，无需手动调用此方法。
     */
    public static void initialize() {
        ensureInitialized();
    }

    /**
     * 懒初始化：保证目录创建和首次加载只在第一次 get/set 时真正执行一次。
     */
    private static void ensureInitialized() {
        if (initStarted.get()) {
            return; // 已经成功启动过
        }

        if (initStarted.compareAndSet(false, true)) {
            Context context = Application.getAppContext();
            if (context == null) {
                // Application 尚未初始化，重置标志，让下次调用再尝试
                initStarted.set(false);
                Log.w(TAG, "Config initialization deferred: Application context is null.");
                return;
            }

            File configDir = new File(context.getFilesDir(), "config");
            if (!configDir.exists() && !configDir.mkdirs()) {
                Log.e(TAG, "Failed to create config directory");
                isLoaded = true; // 避免后续卡死
                return;
            }
            configFile = new File(configDir, "config.json");
            refresh();
        }
    }

    public static void refresh() {
        if (loadFuture != null && !loadFuture.isDone()) {
            loadFuture.cancel(true);
        }

        isLoaded = false;
        loadFuture = executor.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (configLock) {
                    try {
                        if (!configFile.exists()) {
                            if (!copyConfigFromAssets()) {
                                createDefaultConfig();
                            }
                            return;
                        }

                        String jsonContent;
                        try (FileInputStream fis = new FileInputStream(configFile);
                             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                             BufferedReader reader = new BufferedReader(isr)) {

                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            jsonContent = sb.toString();
                        }

                        JsonObject newConfig = gson.fromJson(jsonContent, JsonObject.class);
                        if (newConfig != null) {
                            rootConfig.set(newConfig);
                            configCache.clear();
                            Log.d(TAG, "Config loaded successfully");
                        } else {
                            Log.e(TAG, "Failed to parse config file");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load config", e);
                    } finally {
                        isLoaded = true;
                    }
                }
            }
        });
    }

    private static boolean copyConfigFromAssets() {
        Context context = Application.getAppContext();
        try {
            InputStream assetsStream = null;
            try {
                assetsStream = context.getAssets().open(ASSETS_CONFIG_PATH);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "No config file found in assets: " + ASSETS_CONFIG_PATH);
                return false;
            }

            try (InputStream is = assetsStream;
                 FileOutputStream fos = new FileOutputStream(configFile);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }

                writer.write(sb.toString());
                writer.flush();

                Log.i(TAG, "Config file copied from assets successfully");

                String jsonContent = sb.toString();
                JsonObject newConfig = gson.fromJson(jsonContent, JsonObject.class);
                if (newConfig != null) {
                    rootConfig.set(newConfig);
                    configCache.clear();
                    return true;
                } else {
                    Log.e(TAG, "Failed to parse config from assets");
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy config from assets", e);
            return false;
        }
    }

    private static void createDefaultConfig() {
        synchronized (configLock) {
            try {
                JsonObject defaultConfig = new JsonObject();
                defaultConfig.addProperty("appName", "System Shell Box");
                defaultConfig.addProperty("isFirst", true);

                saveConfigInternal(gson.toJson(defaultConfig));
                rootConfig.set(defaultConfig);
                configCache.clear();
                Log.i(TAG, "Default config created successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create default config", e);
            } finally {
                isLoaded = true;
            }
        }
    }

    private static void saveConfigInternal(String json) throws IOException {
        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(configFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            writer.write(json);
            writer.flush();
        }
    }

    private static void saveConfigAsync() {
        JsonObject config = rootConfig.get();
        if (config == null) {
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (configLock) {
                    try {
                        JsonObject configCopy = gson.fromJson(gson.toJson(config), JsonObject.class);
                        saveConfigInternal(gson.toJson(configCopy));
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to save config", e);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T defaultValue) {
        ensureInitialized();  // 自动初始化

        try {
            if (!isLoaded && loadFuture != null) {
                try {
                    loadFuture.get(5, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    Log.w(TAG, "Config loading timeout, using default value for key: " + key);
                } catch (Exception e) {
                    Log.e(TAG, "Error waiting for config load", e);
                }
            }

            if (configCache.containsKey(key)) {
                Object cached = configCache.get(key);
                if (defaultValue == null ||
                        (cached != null && (defaultValue.getClass().isInstance(cached) ||
                                isCompatibleType(cached, defaultValue)))) {
                    return (T) cached;
                }
            }

            JsonObject config = rootConfig.get();
            if (config == null) {
                return defaultValue;
            }

            String[] keys = key.split("\\.");
            JsonElement element = config;

            for (String k : keys) {
                if (element.isJsonObject()) {
                    element = element.getAsJsonObject().get(k);
                    if (element == null || element.isJsonNull()) {
                        return defaultValue;
                    }
                } else {
                    return defaultValue;
                }
            }

            T result = convertJsonElement(element, defaultValue);

            if (result != null) {
                configCache.put(key, result);
            }

            return result != null ? result : defaultValue;
        } catch (Exception e) {
            Log.e(TAG, "Error getting config for key: " + key, e);
            return defaultValue;
        }
    }

    public static void set(String key, Object value) {
        ensureInitialized();  // 自动初始化

        try {
            if (!isLoaded && loadFuture != null) {
                try {
                    loadFuture.get(5, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    Log.w(TAG, "Config loading timeout, proceeding with set operation for key: " + key);
                } catch (Exception e) {
                    Log.e(TAG, "Error waiting for config load", e);
                }
            }

            synchronized (configLock) {
                JsonObject config = rootConfig.get();
                if (config == null) {
                    config = new JsonObject();
                    rootConfig.set(config);
                }

                String[] keys = key.split("\\.");
                JsonObject current = config;

                for (int i = 0; i < keys.length - 1; i++) {
                    String k = keys[i];
                    JsonElement next = current.get(k);

                    if (next == null || !next.isJsonObject()) {
                        JsonObject newObj = new JsonObject();
                        current.add(k, newObj);
                        current = newObj;
                    } else {
                        current = next.getAsJsonObject();
                    }
                }

                String lastKey = keys[keys.length - 1];
                JsonElement jsonValue = convertToJsonElement(value);

                if (jsonValue == null) {
                    current.remove(lastKey);
                } else {
                    current.add(lastKey, jsonValue);
                }

                configCache.remove(key);
                saveConfigAsync();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting config for key: " + key, e);
        }
    }

    private static boolean isCompatibleType(Object value, Object defaultValue) {
        if (value == null || defaultValue == null) {
            return false;
        }

        Class<?> valueClass = value.getClass();
        Class<?> defaultClass = defaultValue.getClass();

        if (isPrimitiveOrWrapper(valueClass) && isPrimitiveOrWrapper(defaultClass)) {
            return true;
        }

        if (JsonElement.class.isAssignableFrom(valueClass) &&
                JsonElement.class.isAssignableFrom(defaultClass)) {
            return true;
        }

        return valueClass.equals(defaultClass);
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Boolean.class ||
                type == Integer.class ||
                type == Double.class ||
                type == Float.class ||
                type == Long.class ||
                type == Short.class ||
                type == Byte.class ||
                type == Character.class;
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertJsonElement(JsonElement element, T defaultValue) {
        if (element == null) {
            return defaultValue;
        }

        if (defaultValue instanceof JsonElement) {
            return (T) element;
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();

            if (defaultValue instanceof String) {
                return (T) primitive.getAsString();
            } else if (defaultValue instanceof Integer || defaultValue.getClass() == int.class) {
                return (T) Integer.valueOf(primitive.getAsInt());
            } else if (defaultValue instanceof Boolean || defaultValue.getClass() == boolean.class) {
                return (T) Boolean.valueOf(primitive.getAsBoolean());
            } else if (defaultValue instanceof Double || defaultValue.getClass() == double.class) {
                return (T) Double.valueOf(primitive.getAsDouble());
            } else if (defaultValue instanceof Float || defaultValue.getClass() == float.class) {
                return (T) Float.valueOf(primitive.getAsFloat());
            } else if (defaultValue instanceof Long || defaultValue.getClass() == long.class) {
                return (T) Long.valueOf(primitive.getAsLong());
            } else if (defaultValue instanceof Short || defaultValue.getClass() == short.class) {
                return (T) Short.valueOf(primitive.getAsShort());
            } else if (defaultValue instanceof Byte || defaultValue.getClass() == byte.class) {
                return (T) Byte.valueOf(primitive.getAsByte());
            }
        } else if (element.isJsonArray() && defaultValue instanceof JsonArray) {
            return (T) element.getAsJsonArray();
        } else if (element.isJsonObject() && defaultValue instanceof JsonObject) {
            return (T) element.getAsJsonObject();
        }

        try {
            return gson.fromJson(element, (Class<T>) defaultValue.getClass());
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert JSON element to type: " + defaultValue.getClass().getSimpleName(), e);
            return defaultValue;
        }
    }

    private static JsonElement convertToJsonElement(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return new JsonPrimitive((String) value);
        } else if (value instanceof Integer) {
            return new JsonPrimitive((Integer) value);
        } else if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        } else if (value instanceof Double) {
            return new JsonPrimitive((Double) value);
        } else if (value instanceof Float) {
            return new JsonPrimitive((Float) value);
        } else if (value instanceof Long) {
            return new JsonPrimitive((Long) value);
        } else if (value instanceof Short) {
            return new JsonPrimitive((Short) value);
        } else if (value instanceof Byte) {
            return new JsonPrimitive((Byte) value);
        } else if (value instanceof Character) {
            return new JsonPrimitive((Character) value);
        } else if (value instanceof JsonElement) {
            return (JsonElement) value;
        } else {
            return gson.toJsonTree(value);
        }
    }
}
