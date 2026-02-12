package com.familywishes.controller;

import com.familywishes.dto.TemplateDtos.*;
import com.familywishes.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService service;

    @PostMapping
    public TemplateResponse create(@Valid @RequestBody TemplateRequest request) { return service.create(request); }

    @GetMapping("/{id}/versions")
    public List<TemplateResponse> versions(@PathVariable Long id) { return service.versions(id); }

    @PostMapping("/preview")
    public String preview(@Valid @RequestBody PreviewRequest request) { return service.preview(request); }

    @PostMapping("/{id}/versions/{versionId}/restore")
    public TemplateResponse restore(@PathVariable Long id, @PathVariable Long versionId) { return service.restore(id, versionId); }
}
