package com.announcements.AutomateAnnouncements.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.announcements.AutomateAnnouncements.dtos.request.ImageGenerationRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.ImageGenerationResponseDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ImageGenerationService {

    private final WebClient webClient;
    private final String defaultModel;

    public ImageGenerationService(
            WebClient.Builder webClientBuilder,
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.base-url:https://api.openai.com/v1}") String apiBaseUrl,
            @Value("${openai.images.model:dall-e-3}") String defaultModel) {

        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        this.defaultModel = defaultModel;
        String normalizedBaseUrl = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;

        this.webClient = webClientBuilder
                .baseUrl(normalizedBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ImageGenerationResponseDTO generateImage(ImageGenerationRequestDTO request) {
        String prompt = request.getPrompt().trim();
        String size = StringUtils.hasText(request.getSize()) ? request.getSize() : "1024x1024";
        String quality = StringUtils.hasText(request.getQuality()) ? request.getQuality() : "standard";
        String style = StringUtils.hasText(request.getStyle()) ? request.getStyle() : "vivid";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", defaultModel);
        payload.put("prompt", prompt);
        payload.put("n", 1);
        payload.put("size", size);
        payload.put("quality", quality);
        payload.put("style", style);

        log.info("Requesting OpenAI image generation with size={}, quality={}, style={}", size, quality, style);

        OpenAiImageResponse response = webClient.post()
                .uri("/images/generations")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .map(body -> {
                            log.error("OpenAI API error: status={}, body={}", clientResponse.statusCode(), body);
                            return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                    "El servicio de imágenes no está disponible en este momento.");
                        }))
                .bodyToMono(OpenAiImageResponse.class)
                .onErrorResume(throwable -> {
                    log.error("Unexpected error calling OpenAI image API", throwable);
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "No se pudo generar la imagen. Intenta de nuevo en unos minutos."));
                })
                .block();

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El servicio de imágenes no devolvió resultados.");
        }

        OpenAiImageData imageData = response.getData().get(0);
        if (!StringUtils.hasText(imageData.getUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El servicio de imágenes no devolvió una URL válida.");
        }

        ImageGenerationResponseDTO dto = new ImageGenerationResponseDTO();
        dto.setPrompt(prompt);
        dto.setRevisedPrompt(imageData.getRevisedPrompt());
        dto.setImageUrl(imageData.getUrl());
        dto.setSize(size);
        dto.setQuality(quality);
        dto.setStyle(style);
        dto.setGeneratedAt(LocalDateTime.now());
        return dto;
    }

    @Data
    private static class OpenAiImageResponse {
        private List<OpenAiImageData> data;
    }

    @Data
    private static class OpenAiImageData {
        private String url;
        @JsonProperty("revised_prompt")
        private String revisedPrompt;
    }
}
