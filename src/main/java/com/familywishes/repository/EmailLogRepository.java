package com.familywishes.repository;

import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByStatusAndRetryCountLessThan(EmailStatus status, int retryCount);

    long countByStatus(EmailStatus status);

    long countByStatusAndSentAtGreaterThanEqualAndSentAtLessThan(
            EmailStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    @Query("""
            SELECT e FROM EmailLog e
            WHERE :searchKey = ''
               OR LOWER(e.recipientEmail) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(CAST(e.status as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
            """)
    Page<EmailLog> findAllBySearchKey(@Param("searchKey") String searchKey, Pageable pageable);

    @Query("""
            SELECT e FROM EmailLog e
            WHERE e.recipientEmail = :recipientEmail
              AND (
                    :searchKey = ''
                    OR LOWER(e.recipientEmail) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(CAST(e.status as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
              )
            """)
    Page<EmailLog> findAllByRecipientEmailAndSearchKey(
            @Param("recipientEmail") String recipientEmail,
            @Param("searchKey") String searchKey,
            Pageable pageable
    );
}
