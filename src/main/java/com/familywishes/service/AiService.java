package com.familywishes.service;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;

public interface AiService {
    AiWishResponse generate(AiWishRequest request);
}
