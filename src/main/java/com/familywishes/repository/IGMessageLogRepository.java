
package com.familywishes.repository;

import com.familywishes.entity.MessageLog;
import com.familywishes.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface IGMessageLogRepository extends JpaRepository<MessageLog, Long> {
    long countByStatus(MessageStatus status);
    long countByStatusAndCreatedAtBetween(MessageStatus status, LocalDateTime start, LocalDateTime end);
    List<MessageLog> findByStatusAndRetryCountLessThan(
            MessageStatus status,
            int retryCount
    );

    @Query("SELECT DATE(m.createdAt), COUNT(m) FROM MessageLog m WHERE m.createdAt >= :start GROUP BY DATE(m.createdAt) ORDER BY DATE(m.createdAt)")
    List<Object[]> getDailyCounts(LocalDateTime start);

}
