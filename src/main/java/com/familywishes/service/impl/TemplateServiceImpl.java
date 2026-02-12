package com.familywishes.service.impl;

import com.familywishes.dto.TemplateDtos.*;
import com.familywishes.entity.EmailTemplate;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EmailTemplateRepository;
import com.familywishes.service.TemplateService;
import com.familywishes.util.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {
    private final EmailTemplateRepository repository;
    private final TemplateRenderer renderer;

    @Override
    public TemplateResponse create(TemplateRequest request) {
        int version = repository.findByNameOrderByVersionDesc(request.name()).stream().findFirst().map(EmailTemplate::getVersion).orElse(0) + 1;
        EmailTemplate template = repository.save(EmailTemplate.builder().name(request.name()).subject(request.subject()).htmlContent(request.htmlContent()).version(version).build());
        return new TemplateResponse(template.getId(), template.getName(), template.getSubject(), template.getHtmlContent(), template.getVersion());
    }

    @Override
    public List<TemplateResponse> versions(Long id) {
        EmailTemplate template = repository.findById(id).orElseThrow(() -> new NotFoundException("Template not found"));
        return repository.findByNameOrderByVersionDesc(template.getName()).stream().map(t -> new TemplateResponse(t.getId(), t.getName(), t.getSubject(), t.getHtmlContent(), t.getVersion())).toList();
    }

    @Override
    public String preview(PreviewRequest request) {
        return renderer.render(request.htmlContent(), request.variables() == null ? java.util.Map.of() : request.variables());
    }

    @Override
    public TemplateResponse restore(Long id, Long versionId) {
        EmailTemplate current = repository.findById(id).orElseThrow(() -> new NotFoundException("Template not found"));
        EmailTemplate old = repository.findById(versionId).orElseThrow(() -> new NotFoundException("Version not found"));
        int version = repository.findByNameOrderByVersionDesc(current.getName()).stream().findFirst().map(EmailTemplate::getVersion).orElse(0) + 1;
        EmailTemplate restored = repository.save(EmailTemplate.builder().name(current.getName()).subject(old.getSubject()).htmlContent(old.getHtmlContent()).version(version).build());
        return new TemplateResponse(restored.getId(), restored.getName(), restored.getSubject(), restored.getHtmlContent(), restored.getVersion());
    }
}
