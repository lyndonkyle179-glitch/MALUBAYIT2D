package com.yourdomain.model;

public record ActivityLog(
        int id,
        Integer userId,
        String username,
        String action,
        String createdAt
) {
}