package com.familywishes.service;

import com.familywishes.dto.CalendarDtos;
import java.util.List;

public interface CalendarService {
  List<CalendarDtos.CalendarResponse> listByMonth(Integer month, boolean isAdmin, String userEmail);
}
