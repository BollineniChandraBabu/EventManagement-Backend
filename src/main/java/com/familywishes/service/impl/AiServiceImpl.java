package com.familywishes.service.impl;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.exception.BadRequestException;
import com.familywishes.service.AiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${app.gemini.api.key}")
    private String apiKey;

    @Value("${app.gemini.api.url}")
    private String apiURL;

    @Override
    public AiWishResponse generate(AiWishRequest request) throws JsonProcessingException {
        String prompt = getPrompt(request);

        Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.exchange(
                apiURL + apiKey,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
        if (candidates == null || candidates.isEmpty()) throw new BadRequestException("AI response missing");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (List<Map<String, String>>) content.get("parts");
        String text = parts.get(0).get("text");

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(
                text .replace("```json", "")
                        .replace("```", "")
                        .trim(),
                AiWishResponse.class
        );
    }

    private static String getPrompt(AiWishRequest request) {
//        String prompt = "Generate JSON with keys subject and htmlMessage for a personalized wish: " +
//                "name=" + request.name() + ", relation=" + request.relation() + ", event=" + request.event() +
//                ", festival=" + request.festival() + ", tone=" + request.tone() + ", language=" + request.language();

                String prompt = String.format("""
                        Generate a personalized %s wish email.
                        
                        Recipient Name: %s
                        Sender Name: %s
                        Relationship: %s
                        Festival: %s
                        Tone: %s
                        Language: %s
                        
                        Instructions:
                        - Use the exact names provided.
                        - Do NOT use placeholders.
                        - Email must end with:
                          Regards,
                          %s
                        - Return ONLY valid JSON.
                        - No markdown.
                        
                        Format:
                        {
                          "subject": "...",
                          "htmlMessage": "..."
                        }
                        """,
                        request.event(),
                        request.name(),
                        "Chandra",
                        request.relation(),
                        request.festival(),
                        request.tone(),
                        request.language(),
                        "Chandra"
                );
        return prompt;
    }

    private String generateClosing(AiWishRequest request) {

        String senderName = "Chandra";

        String relation = request.relation() == null
                ? ""
                : request.relation().toLowerCase();

        String tone = request.tone() == null
                ? "formal"
                : request.tone().toLowerCase();

        String closing;

        // 2️⃣ Relation-based logic
        switch (relation) {

            case "father":
            case "mother":
            case "parents":
                closing = "With love and gratitude";
                break;

            case "brother":
            case "sister":
            case "sibling":
                closing = "With lots of love";
                break;

            case "friend":
            case "best friend":
                closing = "Cheers buddy";
                break;

            case "wife":
            case "husband":
            case "girlfriend":
            case "boyfriend":
                closing = "With all my love";
                break;

            case "colleague":
            case "manager":
            case "boss":
                closing = "Warm regards";
                break;

            case "client":
            case "customer":
                closing = "Sincerely";
                break;

            case "teacher":
            case "mentor":
                closing = "With respect";
                break;

            default:
                // 3️⃣ Fallback to tone
                closing = switch (tone) {
                    case "casual" -> "Cheers";
                    case "romantic" -> "Forever yours";
                    case "friendly" -> "Best wishes";
                    default -> "Best regards";
                };
        }

        return "\n\n\n" +closing + "\n" + senderName;
    }

}
