package com.familywishes.service;

import com.familywishes.dto.TemplateDtos.*;

import java.util.List;

public interface TemplateService {
    TemplateResponse create(TemplateRequest request);
    List<TemplateResponse> versions(Long id);
    String preview(PreviewRequest request);
    TemplateResponse restore(Long id, Long versionId);
}
