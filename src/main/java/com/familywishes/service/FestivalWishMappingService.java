package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingBulkRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingBulkResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingSummary;
import java.util.List;

public interface FestivalWishMappingService {

  FestivalWishMappingResponse upsert(FestivalWishMappingRequest request);

  FestivalWishMappingResponse upsertWithImage(
      FestivalWishMappingRequest request, org.springframework.web.multipart.MultipartFile imageFile);

  List<FestivalWishMappingResponse> list();

  PagedResponse<FestivalWishMappingResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir);

  PagedResponse<FestivalWishMappingResponse> listForCurrentUser(
      int page, int size, String searchKey, String sortBy, String sortDir);

  void delete(Long id);

  /** Maps one festival to many users; users that are already mapped are updated. */
  FestivalWishMappingBulkResponse upsertBulk(FestivalWishMappingBulkRequest request);

  FestivalWishMappingSummary summary();

  /** status is ALL, ACTIVE, INACTIVE or FAILED. */
  PagedResponse<FestivalWishMappingResponse> list(
      int page, int size, String searchKey, String status, String sortBy, String sortDir);

  PagedResponse<FestivalWishMappingResponse> list(
      int page,
      int size,
      String searchKey,
      List<Long> specialEventIds,
      List<Long> userIds,
      List<String> statuses,
      String sortBy,
      String sortDir);
}
