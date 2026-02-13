package com.familywishes.service.impl;

import com.familywishes.dto.DashboardDtos.DashboardResponse;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EmailLogRepository emailLogRepository;

    @Override
    public DashboardResponse getDashboard() {
        long totalUsers = userRepository.countByDeletedFalse();
        long upcomingEvents = eventRepository.countByEventDateGreaterThanEqualAndActiveTrue(LocalDate.now());

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

        long emailsSentToday = emailLogRepository.countByStatusAndSentAtGreaterThanEqualAndSentAtLessThan(
                EmailStatus.SENT,
                startOfDay,
                startOfNextDay
        );
        long failedEmails = emailLogRepository.countByStatus(EmailStatus.FAILED);

        return new DashboardResponse(totalUsers, upcomingEvents, emailsSentToday, failedEmails);
    }
}
