package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
public class FestivalController {

  private final FestivalService festivalService;

  @GetMapping
  public PagedResponse<FestivalResponse> listByMonth(
      @RequestParam(required = false) Integer month,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "eventDate") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    return festivalService.listByMonth(month, page, size, searchKey, sortBy, sortDir);
  }
}
