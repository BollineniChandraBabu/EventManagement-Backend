package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingBulkRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingBulkResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingSummary;
import com.familywishes.entity.FestivalWishMapping;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.entity.User;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.FestivalWishMappingService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalWishMappingServiceImpl implements FestivalWishMappingService {

  private final FestivalWishMappingRepository mappingRepository;
  private final SpecialEventRepository specialEventRepository;
  private final SupabaseStorageService storageService;
  private static final long UPCOMING_DAYS = 30;
  private static final Set<String> STATUSES = Set.of("ALL", "ACTIVE", "INACTIVE", "FAILED");

  private final UserRepository userRepository;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Override
  public FestivalWishMappingResponse upsert(FestivalWishMappingRequest request) {
    return upsertWithImage(request, null);
  }

  @Override
  public FestivalWishMappingResponse upsertWithImage(
      FestivalWishMappingRequest request, org.springframework.web.multipart.MultipartFile imageFile) {
    SpecialEvent specialEvent =
        specialEventRepository
            .findById(request.specialEventId())
            .orElseThrow(() -> new NotFoundException("Festival not found"));

    User user =
        userRepository
            .findById(request.userId())
            .orElseThrow(() -> new NotFoundException("User not found"));

    FestivalWishMapping mapping =
        mappingRepository
            .findBySpecialEvent_IdAndUser_Id(request.specialEventId(), request.userId())
            .orElseGet(FestivalWishMapping::new);

    mapping.setSpecialEvent(specialEvent);
    mapping.setUser(user);
    mapping.setCustomMessage(normalizeNote(request.customMessage()));
    mapping.setActive(request.active());

    if (imageFile != null && !imageFile.isEmpty()) {
      if (mapping.getImageObjectKey() != null && !mapping.getImageObjectKey().isBlank()) {
        storageService.deleteObject(mapping.getImageObjectKey());
      }
      String key = storageService.uploadWishImage(imageFile);
      mapping.setImageObjectKey(key);
    }

    return toResponse(mappingRepository.save(mapping));
  }

  @Override
  @Transactional
  public FestivalWishMappingBulkResponse upsertBulk(FestivalWishMappingBulkRequest request) {
    SpecialEvent specialEvent =
        specialEventRepository
            .findById(request.specialEventId())
            .orElseThrow(() -> new NotFoundException("Festival not found"));

    List<Long> userIds = request.userIds().stream().distinct().toList();
    Map<Long, User> usersById =
        userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, user -> user));
    if (usersById.size() != userIds.size()) {
      throw new NotFoundException("One or more users were not found");
    }

    Map<Long, FestivalWishMapping> existingByUserId =
        mappingRepository.findBySpecialEvent_Id(specialEvent.getId()).stream()
            .collect(Collectors.toMap(mapping -> mapping.getUser().getId(), mapping -> mapping));

    String note = normalizeNote(request.customMessage());
    int created = 0;
    List<FestivalWishMapping> toSave = new ArrayList<>();
    for (Long userId : userIds) {
      FestivalWishMapping mapping = existingByUserId.get(userId);
      if (mapping == null) {
        mapping = new FestivalWishMapping();
        mapping.setSpecialEvent(specialEvent);
        mapping.setUser(usersById.get(userId));
        created++;
      }
      mapping.setActive(request.active());
      mapping.setCustomMessage(note);
      toSave.add(mapping);
    }
    mappingRepository.saveAll(toSave);
    return new FestivalWishMappingBulkResponse(created, userIds.size() - created);
  }

  @Override
  public FestivalWishMappingSummary summary() {
    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
    long total = mappingRepository.count();
    long active = mappingRepository.countByActiveTrue();
    long upcoming =
        mappingRepository.countByActiveTrueAndSpecialEvent_EventDateBetween(
            today, today.plusDays(UPCOMING_DAYS));
    return new FestivalWishMappingSummary(
        total, active, total - active, upcoming, mappingRepository.countByIsError(true));
  }

  @Override
  public List<FestivalWishMappingResponse> list() {
    return mappingRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Override
  public PagedResponse<FestivalWishMappingResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    return list(page, size, searchKey, "ALL", sortBy, sortDir);
  }

  @Override
  public PagedResponse<FestivalWishMappingResponse> list(
      int page, int size, String searchKey, String status, String sortBy, String sortDir) {
    List<String> statuses = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim()))
        ? null
        : List.of(status.trim().toUpperCase(Locale.ROOT));
    return list(page, size, searchKey, null, null, statuses, sortBy, sortDir);
  }

  @Override
  public PagedResponse<FestivalWishMappingResponse> list(
      int page,
      int size,
      String searchKey,
      List<Long> specialEventIds,
      List<Long> userIds,
      List<String> statuses,
      String sortBy,
      String sortDir) {
    String normalizedSortBy = resolveSortBy(sortBy);
    Sort sort =
        "desc".equalsIgnoreCase(sortDir)
            ? Sort.by(normalizedSortBy).descending()
            : Sort.by(normalizedSortBy).ascending();

    List<Long> cleanEventIds = (specialEventIds == null || specialEventIds.isEmpty()) ? null : specialEventIds;
    List<Long> cleanUserIds = (userIds == null || userIds.isEmpty()) ? null : userIds;
    List<String> cleanStatuses = (statuses == null || statuses.isEmpty())
        ? null
        : statuses.stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).toList();

    var pageResult =
        mappingRepository.findAllByFilters(
            searchKey == null ? "" : searchKey.trim(),
            cleanEventIds,
            cleanUserIds,
            cleanStatuses,
            PageRequest.of(page, size, sort));
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
  public PagedResponse<FestivalWishMappingResponse> listForCurrentUser(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new NotFoundException("Authenticated user not found");
    }
    User me =
        userRepository
            .findByEmailAndDeletedFalse(auth.getName())
            .orElseThrow(() -> new NotFoundException("User not found"));

    String normalizedSortBy = resolveSortBy(sortBy);
    Sort sort =
        "desc".equalsIgnoreCase(sortDir)
            ? Sort.by(normalizedSortBy).descending()
            : Sort.by(normalizedSortBy).ascending();
    var pageResult =
        mappingRepository.findActiveByUserIdAndSearchKey(
            me.getId(), searchKey == null ? "" : searchKey.trim(), PageRequest.of(page, size, sort));

    return new PagedResponse<>(
        pageResult.getContent().stream().map(this::toResponse).toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements(),
        pageResult.getTotalPages(),
        pageResult.hasNext(),
        pageResult.hasPrevious());
  }

  private FestivalWishMappingResponse toResponse(FestivalWishMapping mapping) {
    String imageUrl =
        mapping.getImageObjectKey() != null && !mapping.getImageObjectKey().isBlank()
            ? storageService.getSignedUrl(mapping.getImageObjectKey())
            : null;
    return new FestivalWishMappingResponse(
        mapping.getId(),
        mapping.getSpecialEvent().getId(),
        mapping.getSpecialEvent().getEventName(),
        mapping.getSpecialEvent().getEventDate(),
        mapping.getUser().getId(),
        mapping.getUser().getName(),
        mapping.isActive(),
        mapping.getCustomMessage(),
        imageUrl,
        mapping.getLastWishSentOn(),
        mapping.isError());
  }

  /** Blank means "no note"; the limit also protects the 1000 character column. */
  private static String normalizeNote(String note) {
    return note == null || note.isBlank() ? null : note.trim();
  }

  private static String normalizeStatus(String status) {
    String upper = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
    return STATUSES.contains(upper) ? upper : "ALL";
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
