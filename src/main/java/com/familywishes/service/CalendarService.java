package com.familywishes.service;

import com.familywishes.dto.CalendarDtos;
import java.util.List;

public interface CalendarService {
  List<CalendarDtos.CalendarResponse> listByMonth(
      Integer month, Integer year, boolean isAdmin, String userEmail);
}
