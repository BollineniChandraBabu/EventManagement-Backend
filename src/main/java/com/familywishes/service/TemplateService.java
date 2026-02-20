package com.familywishes.service;

import com.familywishes.dto.TemplateDtos.*;
import com.familywishes.dto.CommonDtos.PagedResponse;

public interface TemplateService {
    TemplateResponse create(TemplateRequest request);
    PagedResponse<TemplateResponse> versions(Long id, int page, int size, String searchKey);
    String preview(PreviewRequest request);
    TemplateResponse restore(Long id, Long versionId);
}
