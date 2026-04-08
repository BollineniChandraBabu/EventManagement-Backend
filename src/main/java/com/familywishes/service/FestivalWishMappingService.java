package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import java.util.List;

public interface FestivalWishMappingService {

  FestivalWishMappingResponse upsert(FestivalWishMappingRequest request);

  List<FestivalWishMappingResponse> list();

  PagedResponse<FestivalWishMappingResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir);

  List<FestivalWishMappingResponse> listForCurrentUser();

  void delete(Long id);
}
