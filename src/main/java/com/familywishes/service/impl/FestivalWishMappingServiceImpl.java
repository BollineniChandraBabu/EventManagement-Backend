package com.familywishes.service.impl;

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
    mapping.setCustomMessage(request.customMessage());
    mapping.setActive(request.active());

    return toResponse(mappingRepository.save(mapping));
  }

  @Override
  public List<FestivalWishMappingResponse> list() {
    return mappingRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Override
  public void delete(Long id) {
    if (!mappingRepository.existsById(id)) {
      throw new NotFoundException("Festival mapping not found");
    }
    mappingRepository.deleteById(id);
  }

  private FestivalWishMappingResponse toResponse(FestivalWishMapping mapping) {
    return new FestivalWishMappingResponse(
        mapping.getId(),
        mapping.getSpecialEvent().getId(),
        mapping.getSpecialEvent().getEventName(),
        mapping.getUser().getId(),
        mapping.getUser().getName(),
        mapping.getCustomMessage(),
        mapping.isActive());
  }
}
