package com.familywishes.repository;

import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
  List<EmailLog> findByStatusAndRetryCountLessThan(EmailStatus status, int retryCount);

  long countByStatus(EmailStatus status);

  long countByStatusAndSentAtGreaterThanEqualAndSentAtLessThan(
      EmailStatus status, LocalDateTime startTime, LocalDateTime endTime);

  long countByStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeIn(
      EmailStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      List<EmailType> emailTypes);

  long countByStatusAndEmailTypeIn(EmailStatus status, List<EmailType> emailTypes);

  long countByStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeNotIn(
      EmailStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      List<EmailType> emailTypes);

  long countByStatusAndEmailTypeNotIn(EmailStatus status, List<EmailType> emailTypes);

  long countByRecipientUserEmailAndStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeNotIn(
      String recipientEmail,
      EmailStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      List<EmailType> emailTypes);

  long countByRecipientUserEmailAndStatusAndEmailTypeNotIn(
      String recipientEmail, EmailStatus status, List<EmailType> emailTypes);

  @Query(
      """
            SELECT e FROM EmailLog e
            WHERE :searchKey = ''
               OR LOWER(e.recipientUser.email) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(CAST(e.status as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(CAST(e.emailType as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
            """)
  Page<EmailLog> findAllBySearchKey(@Param("searchKey") String searchKey, Pageable pageable);

  @Query(
      """
            SELECT e FROM EmailLog e
            WHERE (
                    :searchKey = ''
                    OR LOWER(e.recipientUser.email) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(CAST(e.status as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(CAST(e.emailType as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                  )
              AND e.emailType IN :emailTypes
            """)
  Page<EmailLog> findAllBySearchKeyAndEmailTypeIn(
      @Param("searchKey") String searchKey,
      @Param("emailTypes") List<EmailType> emailTypes,
      Pageable pageable);

  @Query(
      """
            SELECT e FROM EmailLog e
            WHERE e.recipientUser.email = :recipientEmail
              AND (
                    :searchKey = ''
                    OR LOWER(e.recipientUser.email) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(CAST(e.status as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(CAST(e.emailType as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
              )
            """)
  Page<EmailLog> findAllByRecipientEmailAndSearchKey(
      @Param("recipientEmail") String recipientEmail,
      @Param("searchKey") String searchKey,
      Pageable pageable);

  @Query(
      """
            SELECT e FROM EmailLog e
            WHERE e.recipientUser.email = :recipientEmail
              AND (
                    :searchKey = ''
                    OR LOWER(e.recipientUser.email) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(CAST(e.status as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(CAST(e.emailType as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
              )
              AND e.emailType IN :emailTypes
            """)
  Page<EmailLog> findAllByRecipientEmailAndSearchKeyAndEmailTypeIn(
      @Param("recipientEmail") String recipientEmail,
      @Param("searchKey") String searchKey,
      @Param("emailTypes") List<EmailType> emailTypes,
      Pageable pageable);

  @Query(
      """
            SELECT DATE(e.sentAt), COUNT(e)
            FROM EmailLog e
            WHERE e.sentAt >= :start
              AND e.status = :status
            GROUP BY DATE(e.sentAt)
            ORDER BY DATE(e.sentAt)
            """)
  List<Object[]> getDailyCountsByStatus(
      @Param("start") LocalDateTime start, @Param("status") EmailStatus status);

  @Query(
      """
            SELECT DATE(e.sentAt), COUNT(e)
            FROM EmailLog e
            WHERE e.sentAt >= :start
              AND e.status = :status
              AND e.emailType IN :emailTypes
            GROUP BY DATE(e.sentAt)
            ORDER BY DATE(e.sentAt)
            """)
  List<Object[]> getDailyCountsByStatusAndEmailTypeIn(
      @Param("start") LocalDateTime start,
      @Param("status") EmailStatus status,
      @Param("emailTypes") List<EmailType> emailTypes);

  @Query(
      """
            SELECT DATE(e.sentAt), COUNT(e)
            FROM EmailLog e
            WHERE e.sentAt >= :start
              AND e.status = :status
              AND e.emailType NOT IN :emailTypes
            GROUP BY DATE(e.sentAt)
            ORDER BY DATE(e.sentAt)
            """)
  List<Object[]> getDailyCountsByStatusAndEmailTypeNotIn(
      @Param("start") LocalDateTime start,
      @Param("status") EmailStatus status,
      @Param("emailTypes") List<EmailType> emailTypes);

  @Query(
      """
            SELECT DATE(e.sentAt), COUNT(e)
            FROM EmailLog e
            WHERE e.sentAt >= :start
              AND e.status = :status
              AND e.recipientUser.email = :recipientEmail
              AND e.emailType NOT IN :emailTypes
            GROUP BY DATE(e.sentAt)
            ORDER BY DATE(e.sentAt)
            """)
  List<Object[]> getDailyCountsByRecipientAndStatusAndEmailTypeNotIn(
      @Param("start") LocalDateTime start,
      @Param("status") EmailStatus status,
      @Param("recipientEmail") String recipientEmail,
      @Param("emailTypes") List<EmailType> emailTypes);
}
