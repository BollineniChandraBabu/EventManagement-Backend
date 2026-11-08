package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.DirectoryUserResponse;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.repository.RelationshipSeedRepository;
import com.familywishes.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies the public user directory used by the circle search: only a small, non-sensitive subset
 * is returned, and the search term and sort are normalised.
 */
@ExtendWith(MockitoExtension.class)
class UserDirectoryTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private RelationshipSeedRepository relationshipSeedRepository;
  @Mock private SupabaseStorageService supabaseStorageService;
  @Mock private FestivalWishMappingRepository festivalWishMappingRepository;
  @Mock private com.familywishes.service.AiService aiService;

  @InjectMocks private UserServiceImpl userService;

  private User user(Long id, String name) {
    return User.builder()
        .id(id)
        .name(name)
        .email(name.toLowerCase() + "@example.com")
        .role(Role.ROLE_ADMIN)
        .gender(Gender.FEMALE)
        .relationShip(RelationshipSeed.builder().code("FRIEND").active(true).build())
        .online(true)
        .lastSeenAt(LocalDateTime.of(2026, 1, 2, 3, 4))
        .active(true)
        .deleted(false)
        .build();
  }

  @Test
  void directoryReturnsOnlyTheSafeSubsetOfFields() {
    when(supabaseStorageService.getDefaultProfilePictureUrl(any()))
        .thenReturn("https://cdn.example.com/avatar.png");
    when(userRepository.findAllActiveUsers(eq(""), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(user(2L, "Asha")), PageRequest.of(0, 20), 1));

    PagedResponse<DirectoryUserResponse> response = userService.directory(0, 20, null, null, null);

    assertEquals(1, response.content().size());
    DirectoryUserResponse entry = response.content().get(0);
    assertEquals(2L, entry.id());
    assertEquals("Asha", entry.name());
    assertEquals("asha@example.com", entry.email());
    assertEquals("FRIEND", entry.relationShip());
    assertTrue(entry.online());
    assertEquals("https://cdn.example.com/avatar.png", entry.profilePictureUrl());
    assertEquals(1, response.totalElements());
  }

  @Test
  void directoryTrimsTheSearchTermAndDefaultsTheSortToName() {
    when(userRepository.findAllActiveUsers(anyString(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    userService.directory(0, 20, "  as  ", "  ", "asc");

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(userRepository).findAllActiveUsers(eq("as"), captor.capture());
    Sort.Order order = captor.getValue().getSort().getOrderFor("name");
    assertNotNull(order, "default sort should be by name");
    assertTrue(order.isAscending());
  }

  @Test
  void directoryMapsUnknownSortToNameAndHonoursDirection() {
    when(userRepository.findAllActiveUsers(anyString(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    userService.directory(0, 20, "", "somethingElse", "desc");

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(userRepository).findAllActiveUsers(eq(""), captor.capture());
    Sort.Order order = captor.getValue().getSort().getOrderFor("name");
    assertNotNull(order, "unknown sort should fall back to name");
    assertTrue(order.isDescending());
  }
}
