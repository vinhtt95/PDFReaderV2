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
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private final OkHttpClient client;

    public GeminiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private CompletableFuture<String> callGemini(String prompt) {
        String apiKey = ConfigLoader.getApiKey();
        String model = ConfigLoader.getGeminiModel();

        if (apiKey.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("API Key not found. Check Settings."));
        }

        String fullUrl = BASE_URL + model + ":generateContent?key=" + apiKey;

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
                .url(fullUrl)
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
                        String err = responseBody != null ? responseBody.string() : "Unknown Error";
                        future.completeExceptionally(new IOException("API Error (" + response.code() + "): " + err));
                        return;
                    }

                    String respStr = responseBody.string();
                    try {
                        JsonObject jsonResp = JsonParser.parseString(respStr).getAsJsonObject();
                        if (jsonResp.has("candidates") && jsonResp.getAsJsonArray("candidates").size() > 0) {
                            String resultText = jsonResp.getAsJsonArray("candidates")
                                    .get(0).getAsJsonObject()
                                    .getAsJsonObject("content")
                                    .getAsJsonArray("parts")
                                    .get(0).getAsJsonObject()
                                    .get("text").getAsString();
                            future.complete(resultText);
                        } else {
                            future.completeExceptionally(new RuntimeException("No content returned (Safety block?)"));
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(new RuntimeException("Parse Error: " + e.getMessage()));
                    }
                }
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<String> translate(String text) {
        String customPrompt = ConfigLoader.getTranslationPrompt();
        return callGemini(customPrompt + "\n\n" + text);
    }

    @Override
    public CompletableFuture<String> analyze(String text) {
        String customPrompt = ConfigLoader.getAnalysisPrompt();
        return callGemini(customPrompt + text);
    }
}