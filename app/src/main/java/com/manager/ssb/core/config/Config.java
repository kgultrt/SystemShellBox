package com.manager.ssb.core.config;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.manager.ssb.App;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Config {

    private static final String CONFIG_PATH = "config/config.json";
    private static File configFile;
    private static volatile JsonObject rootConfig = null; // 缓存整个 JSON 对象
    private static ConcurrentHashMap<String, Object> configCache = new ConcurrentHashMap<>(); // 使用 ConcurrentHashMap
    private static final Gson gson = new Gson();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(); // 异步线程池
    private static CompletableFuture<Void> loadFuture = CompletableFuture.completedFuture(null);

    static {
        initialize();
    }

    private static void initialize() {
        Context context = App.getAppContext();
        File configDir = new File(context.getFilesDir(), "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        configFile = new File(configDir, "config.json");

        refresh(); // 首次自动加载
    }

    public static void refresh() {
        loadFuture = CompletableFuture.runAsync(() -> { // 异步加载
            try {
                if (!configFile.exists()) {
                    createDefaultConfig();
                }

                // 读取配置
                String json = readFile(configFile);
                JsonObject newConfig = gson.fromJson(json, JsonObject.class);
                rootConfig = newConfig;
                configCache.clear(); // 清空缓存
                Log.d("Config", "Config loaded successfully");

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Config", "Failed to load config", e);
            }
        }, executor);
    }

    private static void createDefaultConfig() throws Exception {
        JsonObject defaultConfig = new JsonObject();
        defaultConfig.addProperty("appName", "System Shell Box");
        defaultConfig.addProperty("maxUsers", 10);
        defaultConfig.addProperty("darkMode", false);

        JsonObject nestedObject = new JsonObject();
        nestedObject.addProperty("key1", "value1");
        nestedObject.addProperty("key2", 123);
        defaultConfig.add("nested", nestedObject);

        saveConfig(gson.toJson(defaultConfig));
    }

    private static String readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return new String(data, "UTF-8");
    }

    private static void saveConfig(String json) {
        executor.execute(() -> { // 异步保存
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                fos.write(json.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 获取配置项的值，支持嵌套 JSON (不使用 JSON Path)
     *
     * @param key          配置项的键，可以使用点号分隔符访问嵌套字段 (e.g., "nested.key1")
     * @param defaultValue 默认值，如果配置项不存在则返回该值
     * @param <T>          配置项的类型
     * @return 配置项的值，如果不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T defaultValue) {
        try {
            // 确保配置加载完成
            loadFuture.join();

            // 先从缓存中获取
            if (configCache.containsKey(key)) {
                return (T) configCache.get(key);
            }

            // 从根 JSON 中解析
            if (rootConfig == null) {
                return defaultValue;
            }

            String[] keys = key.split("\\.");
            JsonElement element = rootConfig;

            for (String k : keys) {
                if (element instanceof JsonObject) {
                    element = ((JsonObject) element).get(k);
                    if (element == null) {
                        return defaultValue;
                    }
                } else {
                    return defaultValue; // 如果不是 JsonObject，说明路径错误
                }
            }

            // 转换类型
            if (element.isJsonPrimitive()) {
                if (defaultValue instanceof String) {
                    String value = element.getAsString();
                    configCache.put(key, value);
                    return (T) value;
                } else if (defaultValue instanceof Integer) {
                    Integer value = element.getAsInt();
                    configCache.put(key, value);
                    return (T) value;
                } else if (defaultValue instanceof Boolean) {
                    Boolean value = element.getAsBoolean();
                    configCache.put(key, value);
                    return (T) value;
                } else if (defaultValue instanceof Double) {
                    Double value = element.getAsDouble();
                    configCache.put(key, value);
                    return (T) value;
                }
                // 可以根据需要添加更多类型
            }

            // 缓存结果
            configCache.put(key, element);

            return (T) element;

        } catch (Exception e) {
            Log.e("Config", "Error getting config", e);
            return defaultValue;
        }
    }


    /**
     * 设置配置项的值，支持嵌套 JSON (不使用 JSON Path)
     *
     * @param key   配置项的键，可以使用点号分隔符访问嵌套字段 (e.g., "nested.newKey")
     * @param value 配置项的值
     */
    public static void set(String key, Object value) {
        try {
            String[] keys = key.split("\\.");
            JsonObject current = rootConfig;

            // 找到要设置的 JSON 对象
            for (int i = 0; i < keys.length - 1; i++) {
                String k = keys[i];
                if (!current.has(k)) {
                    JsonObject newObj = new JsonObject();
                    current.add(k, newObj); // 如果不存在，创建新的 JSON 对象
                }
                current = current.getAsJsonObject(k);
            }

            // 设置值
            String lastKey = keys[keys.length - 1];
            JsonElement newElement = null;

            if (value instanceof String) {
                newElement = new JsonPrimitive((String) value);
            } else if (value instanceof Integer) {
                newElement = new JsonPrimitive((Integer) value);
            } else if (value instanceof Boolean) {
                newElement = new JsonPrimitive((Boolean) value);
            } else if (value instanceof Double) {
                newElement = new JsonPrimitive((Double) value);
            } else if (value instanceof Number) {
                newElement = new JsonPrimitive((Number) value);
            } else if (value == null) {
                current.remove(lastKey);
            } else {
                // 使用 Gson 转换成 JsonElement
                newElement = gson.toJsonTree(value);
            }

            if (newElement != null) {
                current.add(lastKey, newElement);
            }

            // 清除相关的缓存
            configCache.clear();

            saveConfig(); // 保存配置
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void saveConfig() {
        if (rootConfig != null) {
            saveConfig(gson.toJson(rootConfig));
        }
    }
}