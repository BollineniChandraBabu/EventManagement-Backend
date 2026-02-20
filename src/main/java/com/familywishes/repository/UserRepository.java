package com.familywishes.repository;

import com.familywishes.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByEmail(String email);
    long countByDeletedFalse();


    @Query("SELECT u FROM User u WHERE MONTH(u.birthday) = :month AND DAY(u.birthday) = :day")
    List<User> findTodaysBirthdays(int month, int day);

    @Query("""
            SELECT u FROM User u
            WHERE u.deleted = false
              AND (
                    :searchKey = ''
                    OR LOWER(u.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                  )
            """)
    Page<User> findAllActiveUsers(@Param("searchKey") String searchKey, Pageable pageable);
}
