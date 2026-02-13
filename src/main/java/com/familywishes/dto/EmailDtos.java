package com.familywishes.dto;

public class EmailDtos {
    public record EmailStatusResponse(long total, long pending, long sent, long failed) {}
}

