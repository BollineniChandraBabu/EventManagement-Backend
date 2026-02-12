package com.familywishes.repository;

import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByStatusAndRetryCountLessThan(EmailStatus status, int retryCount);
}
