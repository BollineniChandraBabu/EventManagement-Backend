package com.familywishes.repository;

import com.familywishes.entity.Notification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  Optional<Notification> findFirstByPublishedTrueOrderByPublishedAtDesc();
}
