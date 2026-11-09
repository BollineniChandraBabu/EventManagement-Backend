package com.familywishes.controller;

import com.familywishes.dto.CalendarDtos;
import com.familywishes.service.CalendarService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

  private final CalendarService calendarService;

  @GetMapping
  public List<CalendarDtos.CalendarResponse> listByMonth(
      Authentication authentication,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year) {
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    return calendarService.listByMonth(month, year, isAdmin, authentication.getName());
  }
}
