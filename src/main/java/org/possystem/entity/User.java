package org.possystem.entity;

import java.time.LocalDateTime;

/**
 * Record class for User entity.
 * Represents a system user with authentication credentials.
 */
public record User(
        int id,
        String username,
        String passwordHash,
        String role,
        LocalDateTime createdAt,
        LocalDateTime lastLogin
) {}
