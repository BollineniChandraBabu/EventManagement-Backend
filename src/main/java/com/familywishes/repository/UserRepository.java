package com.familywishes.repository;

import com.familywishes.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmailAndDeletedFalse(String email);

  Optional<User> findByEmail(String email);

  List<User> findByIdInAndDeletedFalse(Set<Long> ids);

  long countByDeletedFalse();

  @Query("SELECT u FROM User u WHERE MONTH(u.birthday) = :month AND DAY(u.birthday) = :day")
  List<User> findTodaysBirthdays(int month, int day);

  @Query(
      """
            SELECT u FROM User u
            WHERE u.deleted = false
              AND (
                    :searchKey = ''
                    OR LOWER(u.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                  )
            """)
  Page<User> findAllActiveUsers(@Param("searchKey") String searchKey, Pageable pageable);

  @Query(
      """
      SELECT u FROM User u
      WHERE u.id <> :excludeUserId
        AND (:onlyOnline = false OR u.online = true)
        AND (:searchKey = ''
             OR LOWER(u.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchKey, '%')))
      """)
  Page<User> findChatUsers(
      @Param("excludeUserId") Long excludeUserId,
      @Param("onlyOnline") boolean onlyOnline,
      @Param("searchKey") String searchKey,
      Pageable pageable);

  @Modifying
  @Transactional
  @Query(
      """
      UPDATE User u
         SET u.online = false
       WHERE u.online = true
         AND u.lastSeenAt IS NOT NULL
         AND u.lastSeenAt < :staleBefore
      """)
  int markStaleUsersOffline(@Param("staleBefore") java.time.LocalDateTime staleBefore);
}
