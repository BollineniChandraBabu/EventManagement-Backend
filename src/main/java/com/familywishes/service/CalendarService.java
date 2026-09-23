package com.familywishes.service;

import com.familywishes.dto.CalendarDtos;
import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalResponse;

import java.util.List;

public interface CalendarService {
  List<CalendarDtos.CalendarResponse> listByMonth(Integer month, boolean isAdmin, String userEmail);

}
