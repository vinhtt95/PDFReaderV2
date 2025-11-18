package com.vinhtt.PDFReader.util;

import java.util.prefs.Preferences;

public class ConfigLoader {
    private static final String NODE_NAME = "com.vinhtt.PDFReader";
    private static final String KEY_API_TOKEN = "gemini_api_key";
    private static final String KEY_GEMINI_MODEL = "gemini_model";
    private static final String KEY_CUSTOM_PROMPT = "gemini_prompt";
    private static final String KEY_FONT_SIZE = "app_font_size"; // New Key

    private static final Preferences prefs = Preferences.userRoot().node(NODE_NAME);

    // Defaults
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String DEFAULT_PROMPT = "Translate the following text to Vietnamese. Only provide the translated text, no explanations:\n\n";
    private static final int DEFAULT_FONT_SIZE = 14;

    public static void saveSettings(String key, String model, String prompt, int fontSize) {
        if (key != null) prefs.put(KEY_API_TOKEN, key);
        if (model != null) prefs.put(KEY_GEMINI_MODEL, model);
        if (prompt != null) prefs.put(KEY_CUSTOM_PROMPT, prompt);
        prefs.putInt(KEY_FONT_SIZE, fontSize);
    }

    public static String getApiKey() {
        return prefs.get(KEY_API_TOKEN, "");
    }

    public static String getGeminiModel() {
        return prefs.get(KEY_GEMINI_MODEL, DEFAULT_MODEL);
    }

    public static String getTranslationPrompt() {
        return prefs.get(KEY_CUSTOM_PROMPT, DEFAULT_PROMPT);
    }

    public static int getFontSize() {
        return prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
}