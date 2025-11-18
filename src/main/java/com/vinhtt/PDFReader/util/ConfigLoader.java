package com.vinhtt.PDFReader.util;

import java.util.prefs.Preferences;

/**
 * Utility class for loading and saving application configuration.
 * Uses Java Preferences API to persist user settings.
 */
public class ConfigLoader {
    private static final String NODE_NAME = "com.vinhtt.PDFReader";
    private static final String KEY_API_TOKEN = "gemini_api_key";
    private static final String KEY_GEMINI_MODEL = "gemini_model";
    private static final String KEY_CUSTOM_PROMPT = "gemini_prompt";
    private static final String KEY_ANALYSIS_PROMPT = "gemini_analysis_prompt";
    private static final String KEY_FONT_SIZE = "app_font_size";

    // Keys for UI State
    private static final String KEY_WINDOW_WIDTH = "window_width";
    private static final String KEY_WINDOW_HEIGHT = "window_height";
    private static final String KEY_DIVIDER_1 = "divider_pos_1";
    private static final String KEY_DIVIDER_2 = "divider_pos_2";

    private static final Preferences prefs = Preferences.userRoot().node(NODE_NAME);

    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String DEFAULT_PROMPT = "Translate the following text to Vietnamese. Only provide the translated text, no explanations:\n\n";
    private static final String DEFAULT_ANALYSIS_PROMPT = "Analyze grammar (S-V-O, Tense) and difficult vocabulary for the following English sentence. Return in Markdown format:\n\n";
    private static final int DEFAULT_FONT_SIZE = 14;

    /**
     * Saves general application settings.
     *
     * @param key            The API Key.
     * @param model          The Gemini Model ID.
     * @param prompt         The translation prompt.
     * @param analysisPrompt The analysis prompt.
     * @param fontSize       The application font size.
     */
    public static void saveSettings(String key, String model, String prompt, String analysisPrompt, int fontSize) {
        if (key != null) prefs.put(KEY_API_TOKEN, key);
        if (model != null) prefs.put(KEY_GEMINI_MODEL, model);
        if (prompt != null) prefs.put(KEY_CUSTOM_PROMPT, prompt);
        if (analysisPrompt != null) prefs.put(KEY_ANALYSIS_PROMPT, analysisPrompt);
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

    public static String getAnalysisPrompt() {
        return prefs.get(KEY_ANALYSIS_PROMPT, DEFAULT_ANALYSIS_PROMPT);
    }

    public static int getFontSize() {
        return prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }

    /**
     * Saves the current window dimensions.
     *
     * @param width  The width of the stage.
     * @param height The height of the stage.
     */
    public static void saveWindowSize(double width, double height) {
        prefs.putDouble(KEY_WINDOW_WIDTH, width);
        prefs.putDouble(KEY_WINDOW_HEIGHT, height);
    }

    /**
     * Retrieves the saved window width.
     *
     * @return The saved width or default 1440.0 if not found.
     */
    public static double getWindowWidth() {
        return prefs.getDouble(KEY_WINDOW_WIDTH, 1440.0);
    }

    /**
     * Retrieves the saved window height.
     *
     * @return The saved height or default 1000.0 if not found.
     */
    public static double getWindowHeight() {
        return prefs.getDouble(KEY_WINDOW_HEIGHT, 1000.0);
    }

    /**
     * Saves the positions of the split pane dividers.
     *
     * @param div1 Position of the first divider.
     * @param div2 Position of the second divider.
     */
    public static void saveDividerPositions(double div1, double div2) {
        prefs.putDouble(KEY_DIVIDER_1, div1);
        prefs.putDouble(KEY_DIVIDER_2, div2);
    }

    /**
     * Retrieves the saved divider positions.
     *
     * @return An array containing the positions, or null if not saved.
     */
    public static double[] getDividerPositions() {
        double d1 = prefs.getDouble(KEY_DIVIDER_1, -1);
        double d2 = prefs.getDouble(KEY_DIVIDER_2, -1);
        if (d1 != -1 && d2 != -1) {
            return new double[]{d1, d2};
        }
        return null;
    }
}