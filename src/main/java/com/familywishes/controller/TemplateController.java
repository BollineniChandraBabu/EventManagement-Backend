package com.familywishes.controller;

import com.familywishes.dto.TemplateDtos.*;
import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService service;

    @PostMapping
    public TemplateResponse create(@Valid @RequestBody TemplateRequest request) { return service.create(request); }

    @GetMapping("/{id}/versions")
    public PagedResponse<TemplateResponse> versions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String searchKey
    ) { return service.versions(id, page, size, searchKey); }

    @PostMapping("/preview")
    public String preview(@Valid @RequestBody PreviewRequest request) { return service.preview(request); }

    @PostMapping("/{id}/versions/{versionId}/restore")
    public TemplateResponse restore(@PathVariable Long id, @PathVariable Long versionId) { return service.restore(id, versionId); }
}
