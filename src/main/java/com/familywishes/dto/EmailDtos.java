package com.familywishes.dto;

import java.time.LocalDateTime;

public class EmailDtos {
    public record EmailStatusResponse(long id, String toEmail, String subject, String status, LocalDateTime sentAt) {}

}

