package com.familywishes.repository;

import com.familywishes.entity.FestivalWishMapping;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalWishMappingRepository extends JpaRepository<FestivalWishMapping, Long> {

  List<FestivalWishMapping> findBySpecialEvent_EventDateAndActiveTrue(LocalDate eventDate);

  Optional<FestivalWishMapping> findBySpecialEvent_IdAndInstagramUser_Id(Long specialEventId, Long instagramUserId);
}
