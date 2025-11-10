package com.announcements.AutomateAnnouncements.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.announcements.AutomateAnnouncements.dtos.request.ImageGenerationRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.ImageGenerationResponseDTO;
import com.announcements.AutomateAnnouncements.services.ImageGenerationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai/images")
@Tag(name = "AI Image Generation", description = "Endpoints para generar imágenes con IA")
public class ImageGenerationController {

    @Autowired
    private ImageGenerationService imageGenerationService;

    @PostMapping("/generate")
    @Operation(summary = "Generar imagen", description = "Genera una imagen usando IA a partir de un prompt y parámetros opcionales")
    public ResponseEntity<ImageGenerationResponseDTO> generateImage(
            @Valid @RequestBody ImageGenerationRequestDTO request) {
        ImageGenerationResponseDTO response = imageGenerationService.generateImage(request);
        return ResponseEntity.ok(response);
    }
}
