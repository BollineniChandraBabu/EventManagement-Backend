package com.familywishes.repository;

import com.familywishes.entity.UserWishSettings;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWishSettingsRepository extends JpaRepository<UserWishSettings, Long> {
  List<UserWishSettings> findByGoodMorningEnabledTrue();

  List<UserWishSettings> findByGoodNightEnabledTrue();

  List<UserWishSettings> findByBirthdayEnabledTrue();
}
