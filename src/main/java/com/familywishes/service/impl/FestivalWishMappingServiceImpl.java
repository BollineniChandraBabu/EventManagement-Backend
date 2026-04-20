package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import com.familywishes.entity.FestivalWishMapping;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.entity.User;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.FestivalWishMappingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FestivalWishMappingServiceImpl implements FestivalWishMappingService {

  private final FestivalWishMappingRepository mappingRepository;
  private final SpecialEventRepository specialEventRepository;
  private final UserRepository userRepository;

  @Override
  public FestivalWishMappingResponse upsert(FestivalWishMappingRequest request) {
    SpecialEvent specialEvent =
        specialEventRepository
            .findById(request.specialEventId())
            .orElseThrow(() -> new NotFoundException("Festival not found"));

    User user =
        userRepository.findById(request.userId()).orElseThrow(() -> new NotFoundException("User not found"));

    FestivalWishMapping mapping =
        mappingRepository
            .findBySpecialEvent_IdAndUser_Id(request.specialEventId(), request.userId())
            .orElseGet(FestivalWishMapping::new);

    mapping.setSpecialEvent(specialEvent);
    mapping.setUser(user);
    mapping.setCustomMessage(null);
    mapping.setActive(request.active());

    return toResponse(mappingRepository.save(mapping));
  }

  @Override
  public List<FestivalWishMappingResponse> list() {
    return mappingRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Override
  public PagedResponse<FestivalWishMappingResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    String normalizedSortBy = resolveSortBy(sortBy);
    Sort sort =
        "desc".equalsIgnoreCase(sortDir)
            ? Sort.by(normalizedSortBy).descending()
            : Sort.by(normalizedSortBy).ascending();

    var pageResult =
        mappingRepository.findAllBySearchKey(
            searchKey == null ? "" : searchKey.trim(), PageRequest.of(page, size, sort));
    return new PagedResponse<>(
        pageResult.getContent().stream().map(this::toResponse).toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements(),
        pageResult.getTotalPages(),
        pageResult.hasNext(),
        pageResult.hasPrevious());
  }

  @Override
  public void delete(Long id) {
    if (!mappingRepository.existsById(id)) {
      throw new NotFoundException("Festival mapping not found");
    }
    mappingRepository.deleteById(id);
  }

  @Override
  public List<FestivalWishMappingResponse> listForCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new NotFoundException("Authenticated user not found");
    }
    User me =
        userRepository
            .findByEmailAndDeletedFalse(auth.getName())
            .orElseThrow(() -> new NotFoundException("User not found"));
    return mappingRepository.findByUser_IdAndActiveTrue(me.getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  private FestivalWishMappingResponse toResponse(FestivalWishMapping mapping) {
    return new FestivalWishMappingResponse(
        mapping.getId(),
        mapping.getSpecialEvent().getId(),
        mapping.getSpecialEvent().getEventName(),
        mapping.getUser().getId(),
        mapping.getUser().getName(),
        mapping.isActive());
  }

  private String resolveSortBy(String sortBy) {
    return switch (sortBy) {
      case "id", "active", "createdAt", "updatedAt", "lastWishSentOn" -> sortBy;
      case "festivalName" -> "specialEvent.eventName";
      case "userName" -> "user.name";
      default -> "id";
    };
  }
}
