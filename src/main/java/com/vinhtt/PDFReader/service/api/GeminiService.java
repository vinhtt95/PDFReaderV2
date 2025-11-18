package com.vinhtt.PDFReader.service.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.vinhtt.PDFReader.util.ConfigLoader;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GeminiService implements ITranslationService {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private final OkHttpClient client;

    public GeminiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private CompletableFuture<String> callGemini(String prompt) {
        String apiKey = ConfigLoader.getApiKey();
        if (apiKey.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("API Key not found. Please set it in Settings."));
        }

        JsonObject jsonBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();

        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        jsonBody.add("contents", contents);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL + "?key=" + apiKey)
                .post(body)
                .build();

        CompletableFuture<String> future = new CompletableFuture<>();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("API Error: " + response.code()));
                        return;
                    }
                    String respStr = responseBody.string();
                    // Parse JSON response from Gemini to get the text
                    JsonObject jsonResp = JsonParser.parseString(respStr).getAsJsonObject();
                    try {
                        String resultText = jsonResp.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();
                        future.complete(resultText);
                    } catch (Exception e) {
                        future.completeExceptionally(new RuntimeException("Failed to parse Gemini response"));
                    }
                }
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<String> translate(String text) {
        return callGemini("Translate the following text to Vietnamese. Only provide the translated text, no explanations:\n\n" + text);
    }

    @Override
    public CompletableFuture<String> analyze(String text) {
        return callGemini("Analyze the grammar of the following English sentence. Identify Subject, Verb, Object and Tense. Return format as JSON:\n\n" + text);
    }
}