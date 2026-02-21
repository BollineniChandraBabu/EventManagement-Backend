package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.dto.SeedDtos.WishTemplateSeedRequest;
import com.familywishes.dto.SeedDtos.WishTemplateSeedResponse;
import java.util.List;

public interface SeedService {
  EnumSeedResponse createEnumSeed(String category, EnumSeedRequest request);

  EnumSeedResponse updateEnumSeed(String category, String code, EnumSeedRequest request);

  List<EnumSeedResponse> listEnumSeeds(String category);

  SpecialEventSeedResponse createSpecialEventSeed(SpecialEventSeedRequest request);

  SpecialEventSeedResponse getSpecialEventSeedById(Long id);

  PagedResponse<SpecialEventSeedResponse> listSpecialEventSeeds(
      int page, int size, String searchKey, String sortBy, String sortDir);

  List<SpecialEventSeedResponse> listTodayActiveSpecialEventSeeds(int day, int month);

  SpecialEventSeedResponse updateSpecialEventSeed(Long id, SpecialEventSeedRequest request);

  void deleteSpecialEventSeed(Long id);

  WishTemplateSeedResponse createWishTemplateSeed(WishTemplateSeedRequest request);

  WishTemplateSeedResponse getWishTemplateSeedByType(String type);

  List<WishTemplateSeedResponse> listWishTemplateSeeds();

  WishTemplateSeedResponse updateWishTemplateSeed(String type, WishTemplateSeedRequest request);
}
