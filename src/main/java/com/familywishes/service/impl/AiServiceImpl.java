package com.familywishes.service.impl;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.exception.BadRequestException;
import com.familywishes.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {
    private final RestTemplate restTemplate;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Override
    public AiWishResponse generate(AiWishRequest request) {
        String prompt = "Generate JSON with keys subject and htmlMessage for a personalized wish: " +
                "name=" + request.name() + ", relation=" + request.relation() + ", event=" + request.event() +
                ", festival=" + request.festival() + ", tone=" + request.tone() + ", language=" + request.language();

        Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
        if (candidates == null || candidates.isEmpty()) throw new BadRequestException("AI response missing");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (List<Map<String, String>>) content.get("parts");
        String text = parts.get(0).get("text");
        String subject = text.contains("subject") ? text : "Warm wishes for " + request.event();
        return new AiWishResponse(subject, "<div>" + text.replace("\n", "<br/>") + "</div>");
    }
}
