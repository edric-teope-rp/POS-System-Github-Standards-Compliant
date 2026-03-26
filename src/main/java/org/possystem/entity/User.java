package org.possystem.entity;

import java.time.LocalDateTime;

/**
 * User entity for authentication system.
 * Represents a user account with credentials and metadata.
 */
public record User(
    int id,
    String username,
    String passwordHash,
    String role,
    LocalDateTime createdAt,
    LocalDateTime lastLogin
) {}
