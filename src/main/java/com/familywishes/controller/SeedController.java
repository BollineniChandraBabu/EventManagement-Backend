package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.dto.SeedDtos.WishTemplateSeedRequest;
import com.familywishes.dto.SeedDtos.WishTemplateSeedResponse;
import com.familywishes.service.SeedService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SeedController {
  private final SeedService seedService;

  @PostMapping("/enums/{category}")
  public EnumSeedResponse createEnumSeed(
      @PathVariable String category, @Valid @RequestBody EnumSeedRequest request) {
    return seedService.createEnumSeed(category, request);
  }

  @PutMapping("/enums/{category}/{code}")
  public EnumSeedResponse updateEnumSeed(
      @PathVariable String category,
      @PathVariable String code,
      @Valid @RequestBody EnumSeedRequest request) {
    return seedService.updateEnumSeed(category, code, request);
  }

  @GetMapping("/enums/{category}")
  public List<EnumSeedResponse> listEnumSeeds(@PathVariable String category) {
    return seedService.listEnumSeeds(category);
  }

  @PostMapping("/special-events")
  public SpecialEventSeedResponse createSpecialEventSeed(
      @Valid @RequestBody SpecialEventSeedRequest request) {
    return seedService.createSpecialEventSeed(request);
  }

  @GetMapping("/special-events/{id}")
  public SpecialEventSeedResponse getSpecialEventSeedById(@PathVariable Long id) {
    return seedService.getSpecialEventSeedById(id);
  }

  @GetMapping("/special-events")
  public PagedResponse<SpecialEventSeedResponse> listSpecialEventSeeds(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return seedService.listSpecialEventSeeds(page, size, searchKey, sortBy, sortDir);
  }

  @GetMapping("/special-events/today")
  public List<SpecialEventSeedResponse> listTodayActiveSpecialEventSeeds() {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    return seedService.listTodayActiveSpecialEventSeeds(
        today.getDayOfMonth(), today.getMonthValue());
  }

  @PutMapping("/special-events/{id}")
  public SpecialEventSeedResponse updateSpecialEventSeed(
      @PathVariable Long id, @Valid @RequestBody SpecialEventSeedRequest request) {
    return seedService.updateSpecialEventSeed(id, request);
  }

  @DeleteMapping("/special-events/{id}")
  public void deleteSpecialEventSeed(@PathVariable Long id) {
    seedService.deleteSpecialEventSeed(id);
  }

  @PostMapping("/wish-templates")
  public WishTemplateSeedResponse createWishTemplateSeed(
      @Valid @RequestBody WishTemplateSeedRequest request) {
    return seedService.createWishTemplateSeed(request);
  }

  @GetMapping("/wish-templates")
  public List<WishTemplateSeedResponse> listWishTemplateSeeds() {
    return seedService.listWishTemplateSeeds();
  }

  @GetMapping("/wish-templates/{type}")
  public WishTemplateSeedResponse getWishTemplateSeedByType(@PathVariable String type) {
    return seedService.getWishTemplateSeedByType(type);
  }

  @PutMapping("/wish-templates/{type}")
  public WishTemplateSeedResponse updateWishTemplateSeed(
      @PathVariable String type, @Valid @RequestBody WishTemplateSeedRequest request) {
    return seedService.updateWishTemplateSeed(type, request);
  }
}
