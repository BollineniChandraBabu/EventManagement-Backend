package com.familywishes.repository;

import com.familywishes.entity.User;
import com.familywishes.entity.UserWishSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWishSettingsRepository extends JpaRepository<UserWishSettings, Long> {
    List<UserWishSettings> findByGoodMorningEnabledTrue();
    List<UserWishSettings> findByGoodNightEnabledTrue();
    List<UserWishSettings> findByBirthdayEnabledTrue();
}
