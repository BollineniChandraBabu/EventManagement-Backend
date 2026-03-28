package com.familywishes.service;

import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import java.util.List;

public interface FestivalWishMappingService {

  FestivalWishMappingResponse upsert(FestivalWishMappingRequest request);

  List<FestivalWishMappingResponse> list();

  void delete(Long id);
}
