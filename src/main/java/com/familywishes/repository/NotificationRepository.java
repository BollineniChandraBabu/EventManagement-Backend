package com.familywishes.repository;

import com.familywishes.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  Optional<Notification> findFirstByPublishedTrueOrderByPublishedAtDesc();

  @Query(
      """
          SELECT n FROM Notification n
          WHERE :searchKey = ''
             OR LOWER(n.title) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(n.message) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(n.createdBy) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(n.updatedBy) LIKE LOWER(CONCAT('%', :searchKey, '%'))
          """)
  Page<Notification> findAllBySearchKey(@Param("searchKey") String searchKey, Pageable pageable);
}
