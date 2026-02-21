package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.RelationshipSeedRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelationshipSeedServiceImplTest {

  @Mock private RelationshipSeedRepository relationshipSeedRepository;

  @InjectMocks private RelationshipSeedServiceImpl relationshipSeedService;

  @Test
  void updateShouldThrowWhenCodeDoesNotExist() {
    when(relationshipSeedRepository.findByCode("BROTHER")).thenReturn(Optional.empty());

    EnumSeedRequest request = new EnumSeedRequest("BROTHER", "Brother", true);

    assertThrows(NotFoundException.class, () -> relationshipSeedService.update("BROTHER", request));
  }

  @Test
  void createShouldNormalizeCode() {
    EnumSeedRequest request = new EnumSeedRequest(" sister ", "Sister", true);

    com.familywishes.entity.RelationshipSeed persisted =
        com.familywishes.entity.RelationshipSeed.builder()
            .id(3L)
            .code("SISTER")
            .displayName("Sister")
            .active(true)
            .build();

    when(relationshipSeedRepository.findByCode("SISTER")).thenReturn(Optional.empty());
    when(relationshipSeedRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(persisted);

    assertEquals("SISTER", relationshipSeedService.create(request).code());
  }
}
