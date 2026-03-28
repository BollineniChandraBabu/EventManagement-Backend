package com.familywishes.repository;

import com.familywishes.entity.FestivalWishMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalWishMappingRepository extends JpaRepository<FestivalWishMapping, Long> {

  List<FestivalWishMapping> findBySpecialEvent_DayAndSpecialEvent_MonthAndActiveTrue(int day, int month);

  Optional<FestivalWishMapping> findBySpecialEvent_IdAndInstagramUser_Id(Long specialEventId, Long instagramUserId);
}
