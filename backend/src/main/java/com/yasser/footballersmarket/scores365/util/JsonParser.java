package com.yasser.footballersmarket.scores365.util;

import java.util.List;
import java.util.Map;

public class JsonParser {
    public static <T> Map<String, T> safeGetMap(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, T>) value : null;
    }

    public static <T> List<Map<String, T>> safeGetListMap(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        return value instanceof List ? (List<Map<String, T>>) value : null;
    }

    public static String safeGetString(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    public static Integer safeGetInteger(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        if (value instanceof Integer) return (Integer) value;
        return null;
    }
}
