package com.familywishes.controller;

import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import com.familywishes.service.FestivalWishMappingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/festival-wish-mappings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FestivalWishMappingController {

  private final FestivalWishMappingService festivalWishMappingService;

  @PostMapping
  public FestivalWishMappingResponse upsert(@Valid @RequestBody FestivalWishMappingRequest request) {
    return festivalWishMappingService.upsert(request);
  }

  @GetMapping
  public List<FestivalWishMappingResponse> list() {
    return festivalWishMappingService.list();
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    festivalWishMappingService.delete(id);
  }
}
