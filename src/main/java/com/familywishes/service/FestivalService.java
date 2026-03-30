package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalResponse;
import java.util.List;

public interface FestivalService {
  List<FestivalResponse> listByMonth(Integer month);

  PagedResponse<FestivalResponse> listByMonth(
      Integer month, int page, int size, String searchKey, String sortBy, String sortDir);

  void syncCalendarificFestivals();
}
