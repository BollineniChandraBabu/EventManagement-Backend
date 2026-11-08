package com.familywishes.repository;

import com.familywishes.entity.Circle;
import com.familywishes.entity.enums.CircleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CircleRepository extends JpaRepository<Circle, Long> {

  @Query(
      """
      SELECT c FROM Circle c
       WHERE (c.requester.id = :a AND c.addressee.id = :b)
          OR (c.requester.id = :b AND c.addressee.id = :a)
      """)
  Optional<Circle> findBetween(@Param("a") Long userA, @Param("b") Long userB);

  @Query(
      """
      SELECT c FROM Circle c
       WHERE c.status = :status
         AND (c.requester.id = :userId OR c.addressee.id = :userId)
      """)
  List<Circle> findAllForUser(@Param("userId") Long userId, @Param("status") CircleStatus status);

  @Query(
      """
      SELECT c FROM Circle c
       WHERE c.status = :status
         AND c.requester.id = :userId
      """)
  List<Circle> findAllByRequester(
      @Param("userId") Long userId, @Param("status") CircleStatus status);

  @Query(
      """
      SELECT c FROM Circle c
       WHERE c.status = :status
         AND c.addressee.id = :userId
      """)
  List<Circle> findAllByAddressee(
      @Param("userId") Long userId, @Param("status") CircleStatus status);

  @Query(
      """
      SELECT COUNT(c) > 0 FROM Circle c
       WHERE c.status = com.familywishes.entity.enums.CircleStatus.ACCEPTED
         AND ((c.requester.id = :a AND c.addressee.id = :b)
           OR (c.requester.id = :b AND c.addressee.id = :a))
      """)
  boolean areInCircle(@Param("a") Long userA, @Param("b") Long userB);
}
