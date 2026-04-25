package com.familywishes.service.impl;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.exception.BadRequestException;
import com.familywishes.service.AiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {
  private final RestTemplate restTemplate;

  @Value("${app.gemini.api.key}")
  private String apiKey;

  @Value("${app.gemini.api.url}")
  private String apiURL;

  @Value("${app.pollinations.image.url}")
  private String pollinationsImageUrl;

  @Value("${app.pollinations.image.key}")
  private String pollinationsApiKey;

  @Value("${app.pollinations.image.model:imagen-4}")
  private String pollinationsImageModel;

  @Value("${app.pollinations.balance.url:https://gen.pollinations.ai/account/balance}")
  private String pollinationsBalanceUrl;

  @Override
  public AiWishResponse generate(AiWishRequest request) throws JsonProcessingException {
    String prompt = getWishPrompt(request);

    Map<String, Object> body =
        Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<Map> response =
        restTemplate.exchange(
            apiURL + apiKey, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
    if (candidates == null || candidates.isEmpty())
      throw new BadRequestException("AI response missing");
    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
    var parts = (List<Map<String, String>>) content.get("parts");
    String text = parts.get(0).get("text");

    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(
        text.replace("```json", "").replace("```", "").trim(), AiWishResponse.class);
  }

  public byte[] callGeminiImage(AiWishRequest request) {
    HttpHeaders headers = new HttpHeaders();
    if (StringUtils.hasText(pollinationsApiKey)) {
      headers.setBearerAuth(pollinationsApiKey);
    }
    headers.setAccept(List.of(MediaType.IMAGE_PNG));

    String requestUrl =
        UriComponentsBuilder.fromHttpUrl(pollinationsImageUrl)
            .pathSegment(getImagePrompt(request).trim())
            .queryParam("model", pollinationsImageModel)
            .build()
            .toUriString();

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<byte[]> response =
          restTemplate.exchange(requestUrl, HttpMethod.GET, entity, byte[].class);
      return response.getBody();
    } catch (HttpStatusCodeException ex) {
      log.warn(
          "Pollinations image generation failed for URL {} with status {}: {}",
          requestUrl,
          ex.getStatusCode().value(),
          ex.getResponseBodyAsString());
      return null;
    }
  }

  @Override
  public Map<String, Object> getPollinationsBalance() {
    HttpHeaders headers = new HttpHeaders();
    if (StringUtils.hasText(pollinationsApiKey)) {
      headers.setBearerAuth(pollinationsApiKey);
    }

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<Map> response =
          restTemplate.exchange(pollinationsBalanceUrl, HttpMethod.GET, entity, Map.class);
      return response.getBody();
    } catch (HttpStatusCodeException ex) {
      log.warn(
          "Pollinations balance fetch failed with status {}: {}",
          ex.getStatusCode().value(),
          ex.getResponseBodyAsString());
      throw new BadRequestException("Unable to fetch pollinations balance");
    }
  }

  private static String getWishPrompt(AiWishRequest request) {
    //        String prompt = "Generate JSON with keys subject and htmlMessage for a personalized
    // wish: " +
    //                "name=" + request.name() + ", relation=" + request.relation() + ", event=" +
    // request.event() +
    //                ", festival=" + request.festival() + ", tone=" + request.tone() + ",
    // language=" + request.language();

    String prompt =
        String.format(
            """
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
            "Chandra");
    return prompt;
  }

  private String getImagePrompt(AiWishRequest request) {
    return String.format(
        """
                        Professional ultra HD greeting card, 4k resolution, landscape orientation.

                        Occasion: %s

                        Create a realistic, elegant, emotional greeting card.

                        Include clearly readable text:

                        Main message:
                        Happy %s %s

                        Greeting message:
                        A warm and heartfelt %s wish for you.

                        Design requirements:

                        Recipient name must appear clearly: %s

                        Visual elements based on occasion:
                        Birthday: cake, candles, balloons, confetti
                        Anniversary: roses, romantic flowers, candles
                        Good Morning: sunrise, flowers, warm sunlight
                        Good Night: moon, stars, night sky
                        Festival: cultural decorations, lamps, festive lights

                        Style:
                        photorealistic,
                        professional design,
                        ultra detailed,
                        sharp focus,
                        clear typography,
                        clean layout,
                        soft white background,
                        beautiful color harmony,
                        no blur,
                        no distortion,
                        no watermark
                        """,
        StringUtils.hasText(request.event())? request.event() : request.festival(), request.event(), request.name(), request.tone(), request.name());
  }
}
