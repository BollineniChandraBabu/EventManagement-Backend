package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalResponse;
import java.util.List;

public interface FestivalService {
  List<FestivalResponse> listByMonth(Integer month);

  List<FestivalResponse> listByMonthForYear(Integer month, int year);

  PagedResponse<FestivalResponse> listByMonth(
      Integer month, int page, int size, String searchKey, String sortBy, String sortDir);

  PagedResponse<FestivalResponse> listByMonthForYear(
      Integer month, int year, int page, int size, String searchKey, String sortBy, String sortDir);

  void syncCalendarificFestivals();
}
