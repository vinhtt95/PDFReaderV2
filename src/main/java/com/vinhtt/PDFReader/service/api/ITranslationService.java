package com.vinhtt.PDFReader.service.api;

import java.util.concurrent.CompletableFuture;

public interface ITranslationService {
    CompletableFuture<String> translate(String text);
    CompletableFuture<String> analyze(String text);
}