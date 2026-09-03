package com.hiacademy.api.menu;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiacademy.api.dto.response.MenuFeatureFlag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MenuSettings {
    public static final List<String> KEYS = List.of(
        "parents", "consult", "class", "notice", "classNotice",
        "message", "attend", "homework", "examsRegular", "examsDaily", "calendar",
        "payment", "paymentMessage"
    );

    public static final Set<String> PARENT_KEYS = Set.of(
        "consult", "notice", "classNotice", "attend", "homework",
        "examsRegular", "examsDaily", "calendar"
    );

    private static final ObjectMapper OM = new ObjectMapper();
    private static final TypeReference<Map<String, MenuFeatureFlag>> TYPE = new TypeReference<>() {};

    private MenuSettings() {}

    public static Map<String, MenuFeatureFlag> defaults() {
        Map<String, MenuFeatureFlag> map = new LinkedHashMap<>();
        for (String key : KEYS) {
            map.put(key, flag(true, PARENT_KEYS.contains(key)));
        }
        return map;
    }

    public static Map<String, MenuFeatureFlag> merge(String json) {
        return applyIncoming(parse(json));
    }

    public static String toJson(Map<String, MenuFeatureFlag> incoming) {
        try {
            return OM.writeValueAsString(applyIncoming(incoming));
        } catch (Exception e) {
            return "{}";
        }
    }

    public static boolean enabled(Map<String, MenuFeatureFlag> settings, String key) {
        MenuFeatureFlag flag = settings == null ? null : settings.get(key);
        return flag == null || !Boolean.FALSE.equals(flag.getEnabled());
    }

    public static boolean parentVisible(Map<String, MenuFeatureFlag> settings, String key) {
        if (!enabled(settings, key) || !PARENT_KEYS.contains(key)) return false;
        MenuFeatureFlag flag = settings == null ? null : settings.get(key);
        return flag == null || !Boolean.FALSE.equals(flag.getParentVisible());
    }

    private static Map<String, MenuFeatureFlag> parse(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, MenuFeatureFlag> parsed = OM.readValue(json, TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Map<String, MenuFeatureFlag> applyIncoming(Map<String, MenuFeatureFlag> incoming) {
        Map<String, MenuFeatureFlag> base = defaults();
        if (incoming == null || incoming.isEmpty()) return base;
        for (String key : KEYS) {
            MenuFeatureFlag f = incoming.get(key);
            if (f == null) continue;
            boolean enabled = !Boolean.FALSE.equals(f.getEnabled());
            boolean parentVisible = PARENT_KEYS.contains(key) && !Boolean.FALSE.equals(f.getParentVisible());
            if (!PARENT_KEYS.contains(key)) parentVisible = false;
            base.put(key, flag(enabled, parentVisible));
        }
        MenuFeatureFlag legacy = incoming.get("exams");
        if (legacy != null) {
            if (incoming.get("examsRegular") == null) {
                base.put("examsRegular", copy(legacy, true));
            }
            if (incoming.get("examsDaily") == null) {
                base.put("examsDaily", copy(legacy, true));
            }
        }
        return base;
    }

    private static MenuFeatureFlag flag(boolean enabled, boolean parentVisible) {
        return MenuFeatureFlag.builder().enabled(enabled).parentVisible(parentVisible).build();
    }

    private static MenuFeatureFlag copy(MenuFeatureFlag src, boolean parentCapable) {
        boolean enabled = src == null || !Boolean.FALSE.equals(src.getEnabled());
        boolean parentVisible = parentCapable && (src == null || !Boolean.FALSE.equals(src.getParentVisible()));
        return flag(enabled, parentVisible);
    }
}
