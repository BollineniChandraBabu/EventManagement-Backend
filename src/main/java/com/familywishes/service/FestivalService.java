package com.familywishes.service;

import com.familywishes.dto.FestivalDtos.FestivalResponse;
import java.util.List;

public interface FestivalService {
  List<FestivalResponse> listByMonth(int month);

  void syncCalendarificFestivals();
}
