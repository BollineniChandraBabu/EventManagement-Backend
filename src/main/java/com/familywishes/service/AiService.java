package com.familywishes.service;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface AiService {
    AiWishResponse generate(AiWishRequest request) throws JsonProcessingException;
    byte[] callGeminiImage(AiWishRequest request) throws JsonProcessingException;
}
