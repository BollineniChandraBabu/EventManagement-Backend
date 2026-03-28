package com.familywishes.controller;

import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.service.FestivalService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
  public List<FestivalResponse> listByMonth(@RequestParam(required = false) Integer month) {
    int targetMonth =
        month != null ? month : LocalDate.now(ZoneId.of("Asia/Kolkata")).getMonthValue();
    return festivalService.listByMonth(targetMonth);
  }
}
