package com.vinhtt.PDFReader.util;

import java.util.prefs.Preferences;

public class ConfigLoader {
    private static final String NODE_NAME = "com.vinhtt.PDFReader";
    private static final String KEY_API_TOKEN = "gemini_api_key";
    private static final Preferences prefs = Preferences.userRoot().node(NODE_NAME);

    public static void saveApiKey(String key) {
        prefs.put(KEY_API_TOKEN, key);
    }

    public static String getApiKey() {
        return prefs.get(KEY_API_TOKEN, "");
    }
}